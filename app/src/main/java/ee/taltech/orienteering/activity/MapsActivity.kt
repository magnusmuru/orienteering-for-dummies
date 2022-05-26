package ee.taltech.orienteering.activity

import android.Manifest
import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.Sensor.TYPE_MAGNETIC_FIELD
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_GAME
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.OnMapReadyCallback
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ui.IconGenerator
import ee.taltech.orienteering.BuildConfig
import ee.taltech.orienteering.C
import ee.taltech.orienteering.R
import ee.taltech.orienteering.component.spinner.*
import ee.taltech.orienteering.service.LocationService
import ee.taltech.orienteering.db.ReadDatabaseTask
import ee.taltech.orienteering.db.domain.TrackSummary
import ee.taltech.orienteering.db.domain.User
import ee.taltech.orienteering.db.repository.TrackLocationsRepository
import ee.taltech.orienteering.db.repository.TrackSummaryRepository
import ee.taltech.orienteering.db.repository.UserRepository
import ee.taltech.orienteering.provider.FakeLocationProvider
import ee.taltech.orienteering.track.TrackType
import ee.taltech.orienteering.track.converters.Converter
import ee.taltech.orienteering.track.pracelable.TrackData
import ee.taltech.orienteering.track.pracelable.TrackSyncData
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation
import ee.taltech.orienteering.track.pracelable.loaction.WayPoint
import ee.taltech.orienteering.util.filter.SimpleFilter
import java.lang.Math.toDegrees
import kotlin.math.min
import kotlin.math.pow


class MapsActivity : AppCompatActivity(), SensorEventListener, OnMapReadyCallback {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private const val DEFAULT_ZOOM_LEVEL = 14f
        private const val FOCUSED_ZOOM_LEVEL = 17f

        private const val FILTER_LENGTH = 5

        private const val TRACK_COLOR_SLOW = Color.RED
        private const val TRACK_COLOR_FAST = 0xFFffc9c9.toInt()

        private const val BUNDLE_LAST_UPDATE_TIME = "last_update_time"
        private const val BUNDLE_IS_ADDING_WP = "is_adding_wp"
        private const val BUNDLE_COMPASS_MODE = "compass_mode"
        private const val BUNDLE_DISPLAY_MODE = "display_mode"
        private const val BUNDLE_ROTATION_MODE = "rotation_mode"
        private const val BUNDLE_GPS_ACTIVE = "gps_active"
        private const val BUNDLE_TRACK_ACTIVE = "track_active"
        private const val BUNDLE_RABBITS = "rabbits"
        private const val BUNDLE_RABBIT_TRACKS = "rabbit_tracks"
        private const val BUNDLE_MIN_SPEED = "min_speed"
        private const val BUNDLE_MAX_SPEED = "max_speed"

        private val argbEvaluator = ArgbEvaluator()
    }

    private val speedFilter = SimpleFilter(FILTER_LENGTH)

    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()

    private val trackSummaryRepository = TrackSummaryRepository.open(this)
    private val trackLocationsRepository = TrackLocationsRepository.open(this)
    private val userRepository = UserRepository.open(this)

    private val lastRabbitLocations = hashMapOf<String, TrackLocation>()
    private val wpMarkers = HashMap<Marker, WayPoint>()

    private val mapLocationProvider = FakeLocationProvider()

    private var user: User? = null

    private var locationServiceActive = false
    private var isTracking = false
    private var shouldDrawTail = true
    private var lastLocation: TrackLocation? = null
    private var isSyncedWithService = false
    private var isPermissionsGranted = false

    private var isAddingWP = false
    private var displayMode = DisplayMode.CENTERED
    private var compassMode = CompassMode.IMAGE
    private var rotationMode = RotationMode.NORTH_UP
    private var lastUpdateTime = 0L
    private var elapsedRunningTime = 0L

    private var isCameraIdle = true

    private var minSpeed = 0.0
    private var maxSpeed = 0.0
    private var rabbits = hashMapOf<String, Long>()
    private var rabbitTracks = hashMapOf<Long, TrackSummary>()
    private var rabbitLines = hashMapOf<Long, Polyline>()
    private var rabbitStyleSpans = hashMapOf<Long, MutableList<StyleSpan>>()
    private var rabbitColors = hashMapOf<Long, Int>()
    private var trackTypeMaxSpeeds = hashMapOf<TrackType, Double?>()

    private var currentDegree = 0.0f
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var mPolyline: Polyline? = null
    private var mLineStyles = mutableListOf<StyleSpan>()
    private var lastColor = 0xFFFFFF

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor

    private lateinit var mMap: GoogleMap
    private lateinit var wpIconGenerator: IconGenerator
    private lateinit var cpIconGenerator: IconGenerator

    private lateinit var btnStartStop: ImageButton
    private lateinit var btnAddWP: ImageButton
    private lateinit var btnAddCP: ImageButton

    private lateinit var textViewTotalDistance: TextView
    private lateinit var textViewTotalTime: TextView
    private lateinit var textViewAverageSpeed: TextView

    private lateinit var textViewDistanceLastCP: TextView
    private lateinit var textViewDriftLastCP: TextView
    private lateinit var textViewAverageSpeedLastCP: TextView

    private lateinit var textViewDistanceLastWP: TextView
    private lateinit var textViewDriftLastWP: TextView
    private lateinit var textViewAverageSpeedLastWP: TextView

    private lateinit var imageVieWCompass: TextView
    private lateinit var spinnerDisplayMode: Spinner
    private lateinit var spinnerCompassMode: Spinner
    private lateinit var spinnerRotationMode: Spinner
    private lateinit var spinnerSettingsMode: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_NO_TITLE)
        actionBar?.hide()
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        // safe to call every time
        createNotificationChannel()

        isPermissionsGranted = checkPermissions()
        if (!isPermissionsGranted) {
            requestPermissions()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD)

        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)
        broadcastReceiverIntentFilter.addAction(C.TRACK_STATS_UPDATE_ACTION)
        broadcastReceiverIntentFilter.addAction(C.TRACK_SYNC_RESPONSE)
        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_ADD_WP)
        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_ADD_CP)
        broadcastReceiverIntentFilter.addAction(C.TRACK_RESET)
        broadcastReceiverIntentFilter.addAction(C.TRACK_SAVE)
        broadcastReceiverIntentFilter.addAction(C.TRACK_SET_RABBIT)
        broadcastReceiverIntentFilter.addAction(C.TRACK_IS_RUNNING)

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)
        registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)

        // Obtain the SupportMapFragment and get notified when the activity_maps is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.retainInstance = false
        mapFragment.getMapAsync(this)

        btnStartStop = findViewById(R.id.btn_startStop)
        btnAddWP = findViewById(R.id.btn_add_wp)
        btnAddCP = findViewById(R.id.btn_add_cp)

        locationServiceActive = savedInstanceState?.getBoolean(BUNDLE_GPS_ACTIVE) ?: false
        isTracking = savedInstanceState?.getBoolean(BUNDLE_TRACK_ACTIVE) ?: false

        btnAddWP.setOnClickListener { btnWPOnClick() }
        btnAddCP.setOnClickListener { btnCPOnClick() }
        btnStartStop.setImageResource(if (isTracking) R.drawable.ic_pause_circle_outline_24px else R.drawable.ic_play_circle_outline_24px)

        textViewTotalDistance = findViewById(R.id.total_distance)
        textViewTotalTime = findViewById(R.id.duration)
        textViewAverageSpeed = findViewById(R.id.avg_speed)

        textViewDistanceLastCP = findViewById(R.id.distance_cp)
        textViewDriftLastCP = findViewById(R.id.drift_cp)
        textViewAverageSpeedLastCP = findViewById(R.id.avg_speed_cp)

        textViewDistanceLastWP = findViewById(R.id.distance_wp)
        textViewDriftLastWP = findViewById(R.id.drift_wp)
        textViewAverageSpeedLastWP = findViewById(R.id.avg_speed_wp)

        imageVieWCompass = findViewById(R.id.compass)
        spinnerDisplayMode = findViewById(R.id.spinner_display_mode)
        spinnerCompassMode = findViewById(R.id.spinner_compass_mode)
        spinnerRotationMode = findViewById(R.id.spinner_rotation_mode)
        spinnerSettingsMode = findViewById(R.id.spinner_settings_mode)

        TrackType.values().forEach { type ->
            trackTypeMaxSpeeds[type] = trackSummaryRepository.readMaxSpeed(type)
        }

        startLocationService()
    }
    // ================================================ MAPS CALLBACKS ===============================================

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap?) {
        mMap = map ?: return
        wpIconGenerator = IconGenerator(this)
        wpIconGenerator.setStyle(IconGenerator.STYLE_PURPLE)

        cpIconGenerator = IconGenerator(this)
        cpIconGenerator.setStyle(IconGenerator.STYLE_BLUE)
        cpIconGenerator.setBackground(resources.getDrawable(R.drawable.ic_flag_filled_24px))

        mMap.setOnMapClickListener { latLng -> onMapClicked(latLng) }
        mMap.setOnMarkerClickListener { marker -> onMarkerClicked(marker) }
        mMap.setOnCameraMoveListener { isCameraIdle = false }
        mMap.setOnCameraIdleListener { isCameraIdle = true }
        mMap.isMyLocationEnabled = isPermissionsGranted
        mMap.uiSettings.isCompassEnabled = false;
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.setLocationSource(mapLocationProvider)
        setUpSpinners()
    }

    private fun onMapClicked(latLng: LatLng?) {
        if (!isAddingWP) return
        if (latLng == null) return

        val wp = WayPoint(latLng.latitude, latLng.longitude, lastLocation?.timestamp ?: 0L)
        addWP(wp)

        val intent = Intent(C.TRACK_ACTION_ADD_WP)
        intent.putExtra(C.TRACK_ACTION_ADD_WP_DATA, wp)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        // Don't add 2 in a row
        btnWPOnClick()
    }

    private fun addWP(wp: WayPoint) {
        if (wp.timeRemoved != null) return

        val latLng = LatLng(wp.latitude, wp.longitude)
        val options = MarkerOptions().position(latLng)
        options.icon(BitmapDescriptorFactory.fromBitmap(wpIconGenerator.makeIcon("")))
        options.anchor(wpIconGenerator.anchorU, wpIconGenerator.anchorV)

        val marker = mMap.addMarker(options)
        marker.showInfoWindow()
        wpMarkers[marker] = wp
    }

    private fun addCP(latLng: LatLng) {
        val options = MarkerOptions().position(latLng)

        options.icon(BitmapDescriptorFactory.fromBitmap(cpIconGenerator.makeIcon("")))
        options.anchor(0.2f, 1f)

        val marker = mMap.addMarker(options)
        marker.showInfoWindow()
    }

    private fun onMarkerClicked(marker: Marker): Boolean {
        if (wpMarkers.containsKey(marker)) {
            marker.remove()
            val intent = Intent(C.TRACK_ACTION_REMOVE_WP)
            wpMarkers[marker]?.timeRemoved = lastLocation?.timestamp
            intent.putExtra(C.TRACK_ACTION_REMOVE_WP_LOCATION, wpMarkers[marker])
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            wpMarkers.remove(marker)
        }
        return true
    }

    private fun updateLocation(trackLocation: TrackLocation, drawLine: Boolean, drawOnlyLine: Boolean = false) {
        mapLocationProvider.setLocation(trackLocation)
        val location = LatLng(trackLocation.latitude, trackLocation.longitude)
        if (lastLocation == null) {
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    location,
                    DEFAULT_ZOOM_LEVEL
                )
            )
            isCameraIdle = false
        } else {
            val currentSpeed = speedFilter.process(
                TrackLocation.calcDistanceBetween(lastLocation!!, trackLocation) /
                        ((trackLocation.elapsedTimestamp - (lastLocation!!.elapsedTimestamp) + 1) / 1_000_000_000.0 / 3.6)
            )

            if (currentSpeed > maxSpeed) {
                maxSpeed = currentSpeed
            } else if (currentSpeed < minSpeed) {
                minSpeed = currentSpeed
            }

            val relSpeed = min(1.0, (currentSpeed - minSpeed) / (maxSpeed - minSpeed))

            val color = argbEvaluator.evaluate(
                relSpeed.toFloat().pow(2),
                TRACK_COLOR_SLOW,
                TRACK_COLOR_FAST
            ) as Int

            val lastLoc = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
            if (drawLine) {
                if (mPolyline == null) {
                    mPolyline = mMap.addPolyline(
                        PolylineOptions()
                            .add(lastLoc, location)
                            .width(5f)
                            .color(color)
                    )
                } else {
                    val points = mPolyline?.points
                    points?.add(location)
                    mPolyline?.points = points
                    mLineStyles.add(StyleSpan(StrokeStyle.gradientBuilder(lastColor, color).build()))
                    mPolyline?.setSpans(mLineStyles)
                }
            }

            if (!drawOnlyLine) {

                val cameraTilt = if (rotationMode == RotationMode.DIRECTION_UP) 50f else 0f
                val cameraZoom = mMap.cameraPosition.zoom //  FOCUSED_ZOOM_LEVEL

                val cameraLoc = when (displayMode) {
                    DisplayMode.CENTERED -> lastLoc
                    else -> mMap.cameraPosition.target
                }
                val cameraBearing = when (rotationMode) {
                    RotationMode.NORTH_UP -> 0f
                    RotationMode.USER_CHOSEN_UP -> mMap.cameraPosition.bearing
                    else -> TrackLocation.calcBearingBetween(
                        lastLoc.latitude,
                        lastLoc.longitude,
                        location.latitude,
                        location.longitude
                    )
                }
                if (isCameraIdle) {
                    mMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.builder()
                                .bearing(cameraBearing)
                                .target(cameraLoc)
                                .zoom(cameraZoom)
                                .tilt(cameraTilt)
                                .build()
                        )
                    )
                }
            }

            lastColor = color

            for ((marker, wp) in wpMarkers.entries) {
                val distance = Converter.distToString(wp.getDriftToWP(trackLocation).toDouble())
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(wpIconGenerator.makeIcon(distance)))
                marker.showInfoWindow()
            }

            drawRabbits()
        }

        lastLocation = trackLocation
        lastUpdateTime = trackLocation.elapsedTimestamp
    }


    // ============================================== LIFECYCLE CALLBACKS =============================================
    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        user = userRepository.readUser()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnetometer, SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        sensorManager.unregisterListener(this, accelerometer)
        sensorManager.unregisterListener(this, magnetometer)
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        unregisterReceiver(broadcastReceiver)
        trackSummaryRepository.close()
        trackLocationsRepository.close()
        userRepository.close()
    }

    override fun onRestart() {
        Log.d(TAG, "onRestart")
        super.onRestart()
        isSyncedWithService = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(BUNDLE_LAST_UPDATE_TIME, lastUpdateTime)
        outState.putBoolean(BUNDLE_IS_ADDING_WP, isAddingWP)
        outState.putString(BUNDLE_COMPASS_MODE, compassMode)
        outState.putString(BUNDLE_DISPLAY_MODE, displayMode)
        outState.putString(BUNDLE_ROTATION_MODE, rotationMode)
        outState.putBoolean(BUNDLE_GPS_ACTIVE, locationServiceActive)
        outState.putBoolean(BUNDLE_TRACK_ACTIVE, isTracking)
        outState.putSerializable(BUNDLE_RABBITS, rabbits)
        outState.putSerializable(BUNDLE_RABBIT_TRACKS, rabbitTracks)
        outState.putDouble(BUNDLE_MIN_SPEED, minSpeed)
        outState.putDouble(BUNDLE_MAX_SPEED, maxSpeed)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lastUpdateTime = savedInstanceState.getLong(BUNDLE_LAST_UPDATE_TIME)
        isAddingWP = savedInstanceState.getBoolean(BUNDLE_IS_ADDING_WP)
        compassMode = savedInstanceState.getString(BUNDLE_COMPASS_MODE) ?: CompassMode.IMAGE
        displayMode = savedInstanceState.getString(BUNDLE_DISPLAY_MODE) ?: DisplayMode.CENTERED
        rotationMode = savedInstanceState.getString(BUNDLE_ROTATION_MODE) ?: RotationMode.NORTH_UP
        locationServiceActive = savedInstanceState.getBoolean(BUNDLE_GPS_ACTIVE)
        isTracking = savedInstanceState.getBoolean(BUNDLE_TRACK_ACTIVE)
        rabbits = savedInstanceState.getSerializable(BUNDLE_RABBITS) as HashMap<String, Long>
        rabbitTracks = savedInstanceState.getSerializable(BUNDLE_RABBIT_TRACKS) as HashMap<Long, TrackSummary>
        minSpeed = savedInstanceState.getDouble(BUNDLE_MIN_SPEED, 0.0)
        maxSpeed = savedInstanceState.getDouble(BUNDLE_MAX_SPEED, 0.0)
    }

    // ================================================= COMPASS CALLBACKS ======================================================

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor === accelerometer) {
            lowPass(event.values, lastAccelerometer)
            lastAccelerometerSet = true
        } else if (event.sensor === magnetometer) {
            lowPass(event.values, lastMagnetometer)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            val r = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val degree = (toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360

                val rotateAnimation = RotateAnimation(
                    currentDegree,
                    -degree,
                    RELATIVE_TO_SELF, 0.5f,
                    RELATIVE_TO_SELF, 0.5f
                )
                rotateAnimation.duration = 1000
                rotateAnimation.fillAfter = true

                if (compassMode == CompassMode.IMAGE) {
                    imageVieWCompass.startAnimation(rotateAnimation)
                } else if (compassMode == CompassMode.NUMERIC) {
                    imageVieWCompass.text = "%.1f".format(degree)
                }
                currentDegree = -degree
            }
        }
    }

    fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.05f
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }

    // ============================================== NOTIFICATION CHANNEL CREATION =============================================
    private fun createNotificationChannel() {
        // when on 8 Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                C.NOTIFICATION_CHANNEL,
                C.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            )

            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            //.setShowBadge(false).setSound(null, null);

            channel.description = C.NOTIFICATION_CHANNEL_DESCRIPTION

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    // ============================================== PERMISSION HANDLING =============================================
    // Returns the current state of the permissions needed.
    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun requestPermissions() {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            Snackbar.make(
                findViewById(R.id.activity_main),
                C.SNAKBAR_REQUEST_FINE_LOCATION_ACCESS_TEXT, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(C.SNAKBAR_REQUEST_FINE_LOCATION_CONFIRM_TEXT) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        C.REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                C.REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == C.REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.count() <= 0 -> {
                    // If user interaction was interrupted, the permission request is cancelled and
                    // you receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.")
                    Toast.makeText(
                        this,
                        C.TOAST_USER_INTERACTION_CANCELLED_TEXT, Toast.LENGTH_SHORT
                    ).show()
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission was granted.
                    Log.i(TAG, "Permission was granted")
                    Toast.makeText(
                        this,
                        C.TOAST_PERMISSION_GRANTED_TEXT, Toast.LENGTH_SHORT
                    ).show()
                    isPermissionsGranted = true
                    if (::mMap.isInitialized) mMap.isMyLocationEnabled = true
                }
                else -> {
                    // Permission denied.
                    Snackbar.make(
                        findViewById(R.id.activity_main),
                        C.SNAKBAR_REQUEST_FINE_LOCATION_DENIED_TEXT, Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(C.SNAKBAR_OPEN_SETTINGS_TEXT) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri: Uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }

    }


    // ============================================== CLICK HANDLERS =============================================
    fun buttonStartStopOnClick(view: View) {
        Log.d(TAG, "buttonStartStopOnClick. locationServiceActive: $locationServiceActive")
        // try to start/stop the background service

        if (isTracking) {
            // stopping the service
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(C.TRACK_STOP))
            btnStartStop.setImageResource(R.drawable.ic_play_circle_outline_24px)
        } else {
            startLocationService()
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(C.TRACK_START))
            btnStartStop.setImageResource(R.drawable.ic_pause_circle_outline_24px)
        }
        isTracking = !isTracking

        isSyncedWithService = false
        syncMapData()

    }

    private fun startLocationService() {
        if (!isPermissionsGranted) return
        // if (locationServiceActive && !force) return
        if (Build.VERSION.SDK_INT >= 26) {
            // starting the FOREGROUND service
            // service has to display non-dismissable notification within 5 secs
            startForegroundService(Intent(this, LocationService::class.java))
        } else {
            startService(Intent(this, LocationService::class.java))
        }
        locationServiceActive = true
    }

    private fun btnWPOnClick() {
        Log.d(TAG, "buttonWPOnClick")
        if (!isTracking) return

        isAddingWP = !isAddingWP // Toggle
        if (isAddingWP) {
            btnAddWP.setImageResource(R.drawable.ic_not_interested_24px)
        } else {
            btnAddWP.setImageResource(R.drawable.ic_add_location_24px)
        }
    }

    private fun btnCPOnClick() {
        Log.d(TAG, "buttonCPOnClick")
        if (!isTracking) return

        if (lastLocation == null) return
        val latLng = LatLng(lastLocation!!.latitude, lastLocation!!.longitude)
        addCP(latLng)

        val intent = Intent(C.TRACK_ACTION_ADD_CP)
        intent.putExtra(C.TRACK_ACTION_ADD_CP_DATA, lastLocation as Parcelable)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    // ============================================== BROADCAST RECEIVER =============================================
    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, intent!!.action!!)
            when (intent.action) {
                C.LOCATION_UPDATE_ACTION -> onLocationUpdate(intent)
                C.TRACK_STATS_UPDATE_ACTION -> onTrackDataUpdate(intent)
                C.TRACK_SYNC_RESPONSE -> onTrackSync(intent)
                C.NOTIFICATION_ACTION_ADD_WP -> onNotificationAddedWp(intent)
                C.NOTIFICATION_ACTION_ADD_CP -> onNotificationAddCp(intent)
                C.TRACK_RESET, C.TRACK_SAVE -> onTrackReset(intent)
                C.TRACK_SET_RABBIT -> onSetTrackRabbit(intent)
                C.TRACK_IS_RUNNING -> onTrackIsRunning(intent)
            }
        }

        // ------------------------------------- BROADCAST RECEIVER CALLBACKS ------------------------------------------

        private fun onTrackIsRunning(intent: Intent) {
            btnStartStop.setImageResource(R.drawable.ic_pause_circle_outline_24px)
            isTracking = true
        }

        private fun onSetTrackRabbit(intent: Intent) {
            Log.d(TAG, "On set rabbit.")
            val rabbitName = intent.getStringExtra(C.TRACK_SET_RABBIT_NAME) ?: ReplaySpinnerItems.NONE
            val trackId = intent.getLongExtra(C.TRACK_SET_RABBIT_VALUE, -1L)

            rabbitTracks[trackId] = trackSummaryRepository.readTrackSummary(trackId)!!

            rabbits = HashMap(rabbits.filter { r -> r.value != trackId })

            if (rabbitName != ReplaySpinnerItems.NONE) {
                rabbits[rabbitName] = trackId
            }

            if (rabbits.containsKey(rabbitName) && rabbits[rabbitName] != trackId) {
                syncMapData()
            }

        }

        private fun onTrackReset(intent: Intent) {
            mMap.clear()
            wpMarkers.clear()
            lastRabbitLocations.clear()
            rabbitColors.clear()
            rabbitLines.clear()
            mLineStyles.clear()
            mPolyline = null
            maxSpeed = 0.0
            minSpeed = 0.0
            lastUpdateTime = 0L
            lastLocation = null
            isSyncedWithService = false
            isTracking = intent.getBooleanExtra(C.TRACK_RESET_IS_TRACKING, false)
            if (!isTracking)
                btnStartStop.setImageResource(R.drawable.ic_play_circle_outline_24px)
        }

        private fun onNotificationAddCp(intent: Intent) {
            if (!intent.hasExtra(C.NOTIFICATION_ACTION_ADD_CP_DATA)) return
            val cp = intent.getParcelableExtra<TrackLocation>(C.NOTIFICATION_ACTION_ADD_CP_DATA) ?: return
            val latLng = LatLng(cp.latitude, cp.longitude)
            addCP(latLng)
        }

        private fun onNotificationAddedWp(intent: Intent) {
            if (!intent.hasExtra(C.NOTIFICATION_ACTION_ADD_WP_DATA)) return
            val wp = intent.getParcelableExtra<WayPoint>(C.NOTIFICATION_ACTION_ADD_WP_DATA) ?: return
            addWP(wp)
        }

        private fun onTrackSync(intent: Intent) {
            if (!intent.hasExtra(C.TRACK_SYNC_DATA)) return
            if (isSyncedWithService) return // Avoid double add

            val syncData = intent.getParcelableExtra<TrackSyncData>(C.TRACK_SYNC_DATA) ?: return

            syncData.track.forEachIndexed { i, trackPoint ->
                updateLocation(trackPoint, !syncData.pauses.contains(i), true)
            }
            shouldDrawTail = !syncData.pauses.contains(syncData.track.size)

            for (wp in syncData.wayPoints) {
                addWP(wp)
            }
            for (cp in syncData.checkpoints) {
                addCP(LatLng(cp.latitude, cp.longitude))
            }
            isSyncedWithService = true
        }

        private fun onLocationUpdate(intent: Intent) {
            if (!intent.hasExtra(C.LOCATION_UPDATE_ACTION_TRACK_LOCATION)) return
            if (!isSyncedWithService) {
                syncMapData()
                return // Throw away as sync will return it anyways. Avoids straight line
            }

            val trackLocation = intent.getParcelableExtra(C.LOCATION_UPDATE_ACTION_TRACK_LOCATION) as? TrackLocation ?: return

            updateLocation(trackLocation, shouldDrawTail && isTracking)
            if (!shouldDrawTail)
                shouldDrawTail = true
        }

        private fun onTrackDataUpdate(intent: Intent) {
            if (!intent.hasExtra(C.TRACK_STATS_UPDATE_ACTION_DATA)) return

            val trackData = intent.getParcelableExtra(C.TRACK_STATS_UPDATE_ACTION_DATA) as? TrackData ?: return

            elapsedRunningTime = trackData.totalTime

            textViewTotalDistance.text = Converter.distToString(trackData.totalDistance)
            textViewTotalTime.text = Converter.longToHhMmSs(trackData.totalTime)
            textViewAverageSpeed.text = Converter.speedToString(trackData.getAverageSpeedFromStart(), user?.speedMode ?: true)

            textViewDistanceLastCP.text = Converter.distToString(trackData.distanceFromLastCP)
            textViewDriftLastCP.text = Converter.distToString(trackData.driftLastCP.toDouble())
            textViewAverageSpeedLastCP.text = Converter.speedToString(trackData.getAverageSpeedFromLastCP(), user?.speedMode ?: true)

            textViewDistanceLastWP.text = Converter.distToString(trackData.distanceFromLastWP)
            textViewDriftLastWP.text = Converter.distToString(trackData.driftLastWP.toDouble())
            textViewAverageSpeedLastWP.text = Converter.speedToString(trackData.getAverageSpeedFromLastWP(), user?.speedMode ?: true)
        }
    }

    // ======================================= HELPER FUNCTIONS ======================================

    private fun syncMapData() {
        val intent = Intent(C.TRACK_SYNC_REQUEST)
        intent.putExtra(C.TRACK_SYNC_REQUEST_TIME, 0L) // lastUpdateTime)
        //intent.putExtra(C.TRACK_SYNC_REQUEST_TIME, 0L)
        lastUpdateTime = 0L
        lastLocation = null
        rabbitLines.forEach { l -> l.value.remove() }
        mMap.clear()
        mPolyline = null
        mLineStyles.clear()
        wpMarkers.clear()
        lastRabbitLocations.clear()
        rabbitColors.clear()
        rabbitLines.clear()
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun setUpSpinners() {
        // Create an ArrayAdapters
        val displayOptionAdapter = ArrayAdapter(
            this,
            R.layout.maps_spinner_item, DisplayMode.OPTIONS
        )
        spinnerDisplayMode.adapter = displayOptionAdapter
        spinnerDisplayMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                displayMode = DisplayMode.OPTIONS[position]
                mMap.uiSettings.isScrollGesturesEnabled = displayMode == DisplayMode.FREE_MOVE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val compassOptionAdapter = ArrayAdapter(
            this,
            R.layout.maps_spinner_item, CompassMode.OPTIONS
        )
        spinnerCompassMode.adapter = compassOptionAdapter
        spinnerCompassMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                compassMode = CompassMode.OPTIONS[position]
                when (compassMode) {
                    CompassMode.IMAGE -> {
                        imageVieWCompass.visibility = View.VISIBLE
                        imageVieWCompass.text = ""
                        imageVieWCompass.setBackgroundResource(R.drawable.ic_compass)
                    }
                    CompassMode.NUMERIC -> {
                        imageVieWCompass.visibility = View.INVISIBLE
                        val animation = RotateAnimation(
                            180f,
                            0f,
                            RELATIVE_TO_SELF, 0.5f,
                            RELATIVE_TO_SELF, 0.5f
                        )
                        animation.fillAfter = true
                        animation.duration = 1000
                        imageVieWCompass.startAnimation(animation)
                        imageVieWCompass.background = null
                    }
                    CompassMode.NONE -> {
                        imageVieWCompass.text = ""
                        imageVieWCompass.background = null
                        imageVieWCompass.visibility = View.INVISIBLE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val rotationOptionAdapter = ArrayAdapter(
            this,
            R.layout.maps_spinner_item, RotationMode.OPTIONS
        )
        spinnerRotationMode.adapter = rotationOptionAdapter
        spinnerRotationMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                rotationMode = RotationMode.OPTIONS[position]
                mMap.isBuildingsEnabled = rotationMode == RotationMode.DIRECTION_UP
                mMap.uiSettings.isRotateGesturesEnabled = rotationMode == RotationMode.USER_CHOSEN_UP
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val settingOptionAdapter = ArrayAdapter(
            this,
            R.layout.maps_spinner_item, SettingsMode.OPTIONS
        )
        spinnerSettingsMode.adapter = settingOptionAdapter
        spinnerSettingsMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    1 -> {
                        val current = Intent(applicationContext, DetailActivity::class.java)
                        startActivity(current)
                    }
                    2 -> {
                        val user = Intent(applicationContext, SettingsActivity::class.java)
                        startActivity(user)

                    }
                    3 -> {
                        val history = Intent(applicationContext, HistoryActivity::class.java)
                        startActivity(history)
                    }
                    4 -> {
                        val overview = Intent(applicationContext, OverviewActivity::class.java)
                        startActivity(overview)
                    }
                }
                parent.setSelection(0)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun drawRabbits() {
        rabbits.filter { r -> r.key != ReplaySpinnerItems.NONE }
            .forEach { rabbit ->

                val endTime =
                    if (isTracking) (rabbitTracks[rabbit.value]?.startTimeElapsed ?: 0L) + elapsedRunningTime else Long.MAX_VALUE
                if (!rabbitColors.containsKey(rabbit.value)) {
                    rabbitColors[rabbit.value] = 0xFFFFFF
                }

                ReadDatabaseTask<TrackLocation> { pointsToAdd ->
                    var lastRabbitLoc = lastRabbitLocations[rabbit.key]
                    val points = rabbitLines[rabbit.value]?.points ?: arrayListOf()
                    pointsToAdd.forEach { p ->
                        val location = LatLng(p.latitude, p.longitude)

                        val relSpeed = min(
                            1.0,
                            TrackLocation.calcDistanceBetween(lastRabbitLoc ?: p, p) /
                                    ((p.elapsedTimestamp - (lastRabbitLoc?.elapsedTimestamp
                                        ?: p.elapsedTimestamp) + 1) / 1_000_000_000.0 / 3.6) /
                                    (trackTypeMaxSpeeds[TrackType.fromInt(rabbitTracks[rabbit.value]!!.type)] ?: 1.0)
                        )

                        Log.d(TAG, "Relspeed $relSpeed")
                        val color = argbEvaluator.evaluate(
                            relSpeed.toFloat().pow(0.5f),
                            ReplaySpinnerItems.COLORS_MIN_SPEED[rabbit.key]!!.toInt(),
                            ReplaySpinnerItems.COLORS_MAX_SPEED[rabbit.key]!!.toInt()
                        ) as Int

                        if (!rabbitLines.containsKey(rabbit.value)) {
                            rabbitLines[rabbit.value] = mMap.addPolyline(
                                PolylineOptions()
                                    .width(5f)
                                    .color(color)
                            )
                            rabbitStyleSpans[rabbit.value] = mutableListOf()
                        } else {
                            points.add(location)
                            rabbitStyleSpans[rabbit.value]?.add(
                                StyleSpan(StrokeStyle.gradientBuilder(rabbitColors[rabbit.value]!!, color).build())
                            )
                        }
                        rabbitColors[rabbit.value] = color
                        lastRabbitLoc = p
                    }
                    rabbitLines[rabbit.value]?.points = points
                    rabbitLines[rabbit.value]?.setSpans(rabbitStyleSpans[rabbit.value])
                    if (pointsToAdd.isNotEmpty()) {
                        lastRabbitLocations[rabbit.key] = pointsToAdd.last()
                    }
                }.execute({
                    trackLocationsRepository.readTrackLocations(
                        rabbit.value,
                        lastRabbitLocations[rabbit.key]?.elapsedTimestamp ?: rabbitTracks[rabbit.value]?.startTimeElapsed ?: 0L,
                        endTime
                    )
                })
            }
    }
}
