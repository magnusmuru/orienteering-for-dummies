package ee.taltech.orienteering.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import ee.taltech.orienteering.C
import ee.taltech.orienteering.R
import ee.taltech.orienteering.api.controller.AccountController
import ee.taltech.orienteering.api.controller.TrackSyncController
import ee.taltech.orienteering.api.dto.GpsLocationDto
import ee.taltech.orienteering.api.dto.GpsSessionDto
import ee.taltech.orienteering.api.dto.LoginDto
import ee.taltech.orienteering.db.domain.User
import ee.taltech.orienteering.db.repository.*
import ee.taltech.orienteering.track.Track
import ee.taltech.orienteering.track.TrackType
import ee.taltech.orienteering.track.pracelable.TrackData
import ee.taltech.orienteering.track.converters.Converter
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation
import ee.taltech.orienteering.track.pracelable.loaction.WayPoint
import ee.taltech.orienteering.util.TrackUtils
import ee.taltech.orienteering.util.filter.KalmanTrackLocationFilter
import java.util.*


class LocationService : Service() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        // The desired intervals for location updates. Inexact. Updates may be more or less frequent.
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2000L
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }

    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()

    private val mLocationRequest: LocationRequest = LocationRequest()
    private var mLocationCallback: LocationCallback? = null

    private val offlineSessionsRepository = OfflineSessionsRepository.open(this)
    private val trackSummaryRepository = TrackSummaryRepository.open(this)
    private val trackLocationsRepository = TrackLocationsRepository.open(this)
    private val checkpointsRepository = CheckpointsRepository.open(this)
    private val wayPointsRepository = WayPointsRepository.open(this)
    private val userRepository = UserRepository.open(this)

    private val accountController = AccountController.getInstance(this)
    private val trackSyncController = TrackSyncController.getInstance(this)

    private val trackLocationFilter = KalmanTrackLocationFilter(10.0)

    private var user: User? = null
    private var gpsSession: GpsSessionDto? = null
    private var lastUploadTime: Long = 0L
    private var gpsLocationsToUpload = Collections.synchronizedList(mutableListOf<TrackLocation>())

    private var track: Track? = null
    private var isAddingToTrack = false
    private var isSendingDetailedData = false

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager


    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()

        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_ADD_CP)
        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_ADD_WP)
        broadcastReceiverIntentFilter.addAction(C.TRACK_ACTION_ADD_WP)
        broadcastReceiverIntentFilter.addAction(C.TRACK_ACTION_ADD_CP)
        broadcastReceiverIntentFilter.addAction(C.TRACK_ACTION_REMOVE_WP)
        broadcastReceiverIntentFilter.addAction(C.TRACK_SYNC_REQUEST)
        broadcastReceiverIntentFilter.addAction(C.TRACK_DETAIL_REQUEST)
        broadcastReceiverIntentFilter.addAction(C.TRACK_RESET)
        broadcastReceiverIntentFilter.addAction(C.TRACK_SAVE)
        broadcastReceiverIntentFilter.addAction(C.TRACK_START)
        broadcastReceiverIntentFilter.addAction(C.TRACK_STOP)
        broadcastReceiverIntentFilter.addAction(C.TRACK_SET_TYPE)
        broadcastReceiverIntentFilter.addAction(C.TRACK_SET_NAME)

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)
        registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)

        notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }

        getLastLocation()

        createLocationRequest()
        requestLocationUpdates()

    }

    private fun requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates")

        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
        }
    }

    private fun onNewLocation(location: Location) {
        Log.i(TAG, "New location: $location")

        // First location

        val trackLocation = trackLocationFilter.process(TrackLocation.fromLocation(location))

        if (isAddingToTrack) {
            track?.update(trackLocation)
        }

        uploadLocationIfNeeded(trackLocation)

        // broadcast trackData
        val trackData = track?.getTrackData()

        if (isAddingToTrack) {
            showNotification(trackData)
        } else {
            stopForeground(false)
        }

        val trackDataIntent = Intent(C.TRACK_STATS_UPDATE_ACTION)
        trackDataIntent.putExtra(C.TRACK_STATS_UPDATE_ACTION_DATA, trackData)

        // broadcast new location to UI
        val locationIntent = Intent(C.LOCATION_UPDATE_ACTION)
        locationIntent.putExtra(C.LOCATION_UPDATE_ACTION_TRACK_LOCATION, trackLocation as Parcelable)
        LocalBroadcastManager.getInstance(this).sendBroadcast(locationIntent)
        LocalBroadcastManager.getInstance(this).sendBroadcast(trackDataIntent)

        if (isSendingDetailedData) sendDetailedTrackData()
    }

    private fun createLocationRequest() {
        mLocationRequest.interval =
            UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.fastestInterval =
            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.maxWaitTime =
            UPDATE_INTERVAL_IN_MILLISECONDS
    }


    private fun getLastLocation() {
        try {
            mFusedLocationClient.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.w(TAG, "Get location task successful");
                        if (task.result != null) {
                            onNewLocation(task.result!!)
                        }
                    } else {
                        Log.w(TAG, "Failed to get location." + task.exception)
                    }
                }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. $unlikely")
        }
    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()

        offlineSessionsRepository.close()
        trackSummaryRepository.close()
        trackLocationsRepository.close()
        checkpointsRepository.close()
        wayPointsRepository.close()
        userRepository.close()

        //stop location updates
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)

        // remove notifications
        NotificationManagerCompat.from(this).cancelAll()

        // Unregistering broadcast reciever
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        unregisterReceiver(broadcastReceiver)

        // Broadcast stop to UI
        val intent = Intent(C.LOCATION_UPDATE_ACTION)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

    }

    override fun onLowMemory() {
        Log.d(TAG, "onLowMemory")
        super.onLowMemory()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        user = userRepository.readUser()

        // set counters to 0 if fresh start
        if (track == null) {
            track = Track()
            track!!.type = user?.defaultActivityType ?: TrackType.UNKNOWN
        }

        if (isAddingToTrack) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(C.TRACK_IS_RUNNING))
        }

        showNotification(track?.getTrackData())

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind")
        TODO("not implemented")
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind")
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    fun showNotification(trackData: TrackData?) {
        val intentCp = Intent(C.NOTIFICATION_ACTION_ADD_CP)
        val intentWp = Intent(C.NOTIFICATION_ACTION_ADD_WP)
        if (track != null && track!!.lastLocation != null) {
            val locWP = WayPoint(track!!.lastLocation!!.latitude, track!!.lastLocation!!.longitude, track!!.lastLocation!!.timestamp)
            intentWp.putExtra(C.NOTIFICATION_ACTION_ADD_WP_DATA, locWP)

            intentCp.putExtra(C.NOTIFICATION_ACTION_ADD_CP_DATA, track!!.lastLocation as Parcelable)
        }

        val notifyView = RemoteViews(packageName, R.layout.track_control)

        val pendingIntentCp = PendingIntent.getBroadcast(this, 0, intentCp, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntentWp = PendingIntent.getBroadcast(this, 0, intentWp, PendingIntent.FLAG_UPDATE_CURRENT)

        notifyView.setOnClickPendingIntent(R.id.btn_add_cp, pendingIntentCp)
        notifyView.setOnClickPendingIntent(R.id.btn_add_wp, pendingIntentWp)

        notifyView.setTextViewText(R.id.total_distance, Converter.distToString(trackData?.totalDistance ?: 0.0))
        notifyView.setTextViewText(R.id.duration, Converter.longToHhMmSs(trackData?.totalTime ?: 0))
        notifyView.setTextViewText(
            R.id.avg_speed,
            Converter.speedToString(trackData?.getAverageSpeedFromStart() ?: 0.0, user?.speedMode ?: true)
        )

        notifyView.setTextViewText(R.id.distance_cp, Converter.distToString(trackData?.distanceFromLastCP ?: 0.0))
        notifyView.setTextViewText(R.id.drift_cp, Converter.distToString(trackData?.driftLastCP?.toDouble() ?: 0.0))
        notifyView.setTextViewText(
            R.id.avg_speed_cp,
            Converter.speedToString(trackData?.getAverageSpeedFromLastCP() ?: 0.0, user?.speedMode ?: true)
        )

        notifyView.setTextViewText(R.id.distance_wp, Converter.distToString(trackData?.distanceFromLastWP ?: 0.0))
        notifyView.setTextViewText(R.id.drift_wp, Converter.distToString(trackData?.driftLastWP?.toDouble() ?: 0.0))
        notifyView.setTextViewText(
            R.id.avg_speed_wp,
            Converter.speedToString(trackData?.getAverageSpeedFromLastWP() ?: 0.0, user?.speedMode ?: true)
        )
        notifyView.setViewPadding(R.id.track_control_bar, 0, 100, 1, 0)

        // construct and show notification
        val builder = NotificationCompat.Builder(
            applicationContext,
            C.NOTIFICATION_CHANNEL
        )
            .setSmallIcon(R.drawable.baseline_gps_fixed_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOngoing(isAddingToTrack)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContent(notifyView)
            .setCustomContentView(notifyView)
            .setCustomBigContentView(notifyView)

        // Super important, start as foreground service - ie android considers this as an active app.
        // Need visual reminder - notification.
        // must be called within 5 secs after service starts.
        startForeground(C.NOTIFICATION_ID, builder.build())
    }

    private fun sendDetailedTrackData() {
        val intent = Intent(C.TRACK_DETAIL_RESPONSE)
        val data = track?.getDetailedTrackData()
        intent.putExtra(C.TRACK_DETAIL_DATA, data)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun onTrackSyncRequest(since: Long) {
        user = userRepository.readUser() // TODO: Something else here?

        if (track == null) return

        // Notify activity, that track has been reset
        /*if (track!!.lastLocation == null) {
            val intent = Intent(C.TRACK_RESET)
            intent.putExtra(C.TRACK_RESET_IS_TRACKING, isAddingToTrack)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }*/

        val intent = Intent(C.TRACK_SYNC_RESPONSE)
        val data = track!!.getTrackSyncData(since)
        intent.putExtra(C.TRACK_SYNC_DATA, data)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun uploadLocationIfNeeded(location: TrackLocation?) {
        if (!isAddingToTrack) return

        if (location != null)
            gpsLocationsToUpload.add(location)

        if (user != null && user!!.autoSync) {
            if (gpsSession == null) {
                val gpsSessionDto = GpsSessionDto(
                    name = track!!.name,
                    description = track!!.name,
                    recordedAt = Date(track!!.startTime)
                )
                trackSyncController.createNewSession(gpsSessionDto, { response -> gpsSession = response }, {
                    accountController.login(LoginDto(user!!.email, user!!.password + "5x-9#Y6Cpp"))
                })
            }

            if (location == null && gpsSession?.id != null
                || gpsSession?.id != null && location!!.timestamp - lastUploadTime > user!!.syncInterval
            ) {
                val backUp = gpsLocationsToUpload.toList()
                val toUpload = gpsLocationsToUpload.map { loc ->
                    GpsLocationDto.fromTrackLocation(
                        loc,
                        gpsSession!!.id!!
                    )
                }.toMutableList()

                if (track!!.checkpoints?.size ?: 0 > 0) {
                    track!!.checkpoints!!.filter { cp -> cp.timestamp + UPDATE_INTERVAL_IN_MILLISECONDS >= lastUploadTime }
                        .forEach { cp -> toUpload.add(GpsLocationDto.fromCheckpoint(cp, gpsSession!!.id!!)) }
                }
                if (track!!.wayPoints?.size ?: 0 > 0) {
                    track!!.wayPoints!!.filter { wp -> wp.timeAdded + UPDATE_INTERVAL_IN_MILLISECONDS >= lastUploadTime }
                        .forEach { wp -> toUpload.add(GpsLocationDto.fromWayPoint(wp, gpsSession!!.id!!)) }
                }

                gpsLocationsToUpload = Collections.synchronizedList(mutableListOf())

                trackSyncController.addMultipleLocationsToSession(toUpload, gpsSession!!.id!!,
                    {
                        lastUploadTime = location?.timestamp ?: 0L
                    }
                    , {
                        gpsLocationsToUpload.addAll(backUp)
                    }
                )
            }
        }
    }

    // ===================================== BROADCAST RECEIVER ========================================

    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, intent!!.action!!)
            when (intent.action) {
                C.NOTIFICATION_ACTION_ADD_WP -> {
                    if (!intent.hasExtra(C.NOTIFICATION_ACTION_ADD_WP_DATA)) return
                    track?.addWayPoint(intent.getParcelableExtra(C.NOTIFICATION_ACTION_ADD_WP_DATA)!!)
                }
                C.NOTIFICATION_ACTION_ADD_CP -> {
                    if (!intent.hasExtra(C.NOTIFICATION_ACTION_ADD_CP_DATA)) return
                    track?.addCheckpoint(intent.getParcelableExtra(C.NOTIFICATION_ACTION_ADD_CP_DATA)!!)
                }
                C.TRACK_ACTION_ADD_WP -> {
                    if (!intent.hasExtra(C.TRACK_ACTION_ADD_WP_DATA)) return
                    track?.addWayPoint(intent.getParcelableExtra(C.TRACK_ACTION_ADD_WP_DATA)!!)
                }
                C.TRACK_ACTION_ADD_CP -> {
                    if (!intent.hasExtra(C.TRACK_ACTION_ADD_CP_DATA)) return
                    val trackLocation: TrackLocation = intent.getParcelableExtra(C.TRACK_ACTION_ADD_CP_DATA)!!
                    track?.addCheckpoint(trackLocation)
                    gpsLocationsToUpload.add(trackLocation)
                }
                C.TRACK_ACTION_REMOVE_WP -> {
                    if (!intent.hasExtra(C.TRACK_ACTION_REMOVE_WP_LOCATION)) return
                    track?.removeWayPoint(intent.getParcelableExtra(C.TRACK_ACTION_REMOVE_WP_LOCATION) as? WayPoint ?: return)
                }
                C.TRACK_SYNC_REQUEST -> {
                    if (!intent.hasExtra(C.TRACK_SYNC_REQUEST_TIME)) return
                    onTrackSyncRequest(intent.getLongExtra(C.TRACK_SYNC_REQUEST_TIME, 0L))
                }
                C.TRACK_DETAIL_REQUEST -> onDetailTrackDataRequest(intent)
                C.TRACK_SAVE -> onTrackSave()
                C.TRACK_RESET -> {
                    user = userRepository.readUser()
                    track = Track()
                    track!!.type = user?.defaultActivityType ?: TrackType.UNKNOWN
                    isAddingToTrack = false
                    showNotification(track!!.getTrackData())
                    stopForeground(false)
                }
                C.TRACK_START -> {
                    isAddingToTrack = true
                    user = userRepository.readUser()
                }
                C.TRACK_STOP -> {
                    isAddingToTrack = false
                    track?.onPause()
                    showNotification(track!!.getTrackData()) // <- This is somehow needed
                    stopForeground(false)
                }
                C.TRACK_SET_TYPE -> {
                    track?.type = TrackType.fromInt(intent.getIntExtra(C.TRACK_SET_TYPE_DATA, 0))!!
                    track?.name = TrackUtils.generateNameIfNeeded(track!!.name, track!!.type)
                }
                C.TRACK_SET_NAME -> track?.name =
                    TrackUtils.generateNameIfNeeded(intent.getStringExtra(C.TRACK_SET_NAME_DATA) ?: "", track!!.type)
            }
        }

        // ----------------------------------- BROADCAST RECEIVER CALLBACKS --------------------------------
        private fun onDetailTrackDataRequest(intent: Intent) {
            isSendingDetailedData = intent.getBooleanExtra(C.TRACK_DETAIL_REQUEST_DATA, false)
            if (isSendingDetailedData)
                sendDetailedTrackData()
        }

        private fun onTrackSave() {
            if (track == null || track!!.track?.size ?: 0 < 2) return
            user = userRepository.readUser()
            val trackId = trackSummaryRepository.saveTrack(track!!)
            trackLocationsRepository.saveLocationToTrack(track!!.track!!, trackId)
            checkpointsRepository.saveCheckpointToTrack(track!!.checkpoints!!, trackId)
            wayPointsRepository.saveWayPointToTrack(track!!.wayPoints!!, trackId)

            if (user != null && user!!.autoSync) {
                uploadLocationIfNeeded(null)
                if (gpsLocationsToUpload.isNotEmpty()) {
                    offlineSessionsRepository.saveOfflineSession(trackId)
                }
            } else {
                offlineSessionsRepository.saveOfflineSession(trackId)
            }
            track = Track()
            track!!.type = user?.defaultActivityType ?: TrackType.UNKNOWN
            isAddingToTrack = false
            showNotification(track!!.getTrackData())
            stopForeground(false)
        }
    }
}
