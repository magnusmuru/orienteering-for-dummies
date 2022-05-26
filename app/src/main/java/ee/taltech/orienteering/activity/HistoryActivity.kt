package ee.taltech.orienteering.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import ee.taltech.orienteering.C
import ee.taltech.orienteering.R
import ee.taltech.orienteering.component.imageview.TrackTypeIcons
import ee.taltech.orienteering.component.spinner.ReplaySpinnerItems
import ee.taltech.orienteering.component.spinner.adapter.HistorySpinnerAdapter
import ee.taltech.orienteering.db.domain.TrackSummary
import ee.taltech.orienteering.track.TrackType
import ee.taltech.orienteering.track.converters.Converter
import ee.taltech.orienteering.component.view.TrackIconImageView
import ee.taltech.orienteering.db.ReadDatabaseTask
import ee.taltech.orienteering.db.domain.User
import ee.taltech.orienteering.db.repository.*
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation
import ee.taltech.orienteering.util.TrackUtils
import ee.taltech.orienteering.util.serializer.TrackSerializer

class HistoryActivity : AppCompatActivity() {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private const val BUNDLE_SELECTED_REPLAYS = "selected_replays"

        private const val ALERT_DELETE_TITLE = "Delete track?"
        private const val ALERT_DELETE_TEXT = "Do you want to permanently delete track?"
        private const val ALERT_DELETE_CANCEL_TEXT = "Cancel"
        private const val ALERT_DELETE_DELETE_TEXT = "Delete"
    }

    private val trackSummaryRepository = TrackSummaryRepository.open(this)
    private val trackLocationsRepository = TrackLocationsRepository.open(this)
    private val checkpointsRepository = CheckpointsRepository.open(this)
    private val wayPointsRepository = WayPointsRepository.open(this)

    private val userRepository = UserRepository.open(this)

    private var user: User? = null

    private var selectedItems = hashMapOf<Long, Int>()
    private var isPermissionsGranted = false

    private lateinit var scrollViewHistory: ScrollView
    private lateinit var linearLayoutScrollContent: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        selectedItems = savedInstanceState?.getSerializable(BUNDLE_SELECTED_REPLAYS) as? HashMap<Long, Int> ?: hashMapOf()

        // scrollViewHistory = findViewById(R.id.scroll_history)
        linearLayoutScrollContent = findViewById(R.id.linear_scroll)

        isPermissionsGranted = checkPermissions()
    }

    override fun onStart() {
        super.onStart()

        user = userRepository.readUser()

        overridePendingTransition(
            R.anim.slide_in_from_right,
            R.anim.slide_out_to_left
        )

        linearLayoutScrollContent.removeAllViews()
        trackSummaryRepository.readTrackSummaries(0, 999).forEach { track ->
            run {
                val trackView = layoutInflater.inflate(R.layout.track_history_item, linearLayoutScrollContent, false)
                trackView.findViewById<TextView>(R.id.track_name).text = track.name
                trackView.findViewById<ImageView>(R.id.track_type_icon)
                    .setImageResource(TrackTypeIcons.getIcon(TrackType.fromInt(track.type)!!))
                trackView.findViewById<TextView>(R.id.distance).text = Converter.distToString(track.distance)
                trackView.findViewById<TextView>(R.id.duration).text = Converter.longToHhMmSs(track.durationMoving)
                trackView.findViewById<TextView>(R.id.elevation_gained).text = Converter.distToString(track.elevationGained)
                trackView.findViewById<TextView>(R.id.avg_speed).text =
                    Converter.speedToString(track.distance / track.durationMoving * 1_000_000_000 * 3.6, user?.speedMode ?: true)
                trackView.findViewById<TextView>(R.id.max_speed).text =
                    Converter.speedToString(track.maxSpeed, user?.speedMode ?: true)
                trackView.findViewById<TextView>(R.id.drift).text = Converter.distToString(track.drift)

                val trackImage = trackView.findViewById<TrackIconImageView>(R.id.track_image)
                ReadDatabaseTask<TrackLocation> {
                    trackImage.track = it
                    trackImage.invalidate()
                }.execute({ trackLocationsRepository.readTrackLocations(track.trackId, 0L, Long.MAX_VALUE) })
                trackImage.maxSpeed = track.maxSpeed

                val deleteButton = trackView.findViewById<Button>(R.id.btn_delete)
                deleteButton.setOnClickListener {
                    onDeleteClicked(track, trackView)
                }

                val renameButton = trackView.findViewById<Button>(R.id.btn_rename)
                renameButton.setOnClickListener { onRenameClicked(track, trackView) }

                val exportButton = trackView.findViewById<Button>(R.id.btn_export)
                exportButton.setOnClickListener {
                    onExportClicked(track)
                }

                val replaySpinner = trackView.findViewById<Spinner>(R.id.spinner_replay)
                setUpReplaySpinner(replaySpinner, track, trackImage)

                val optionsView = trackView.findViewById<ConstraintLayout>(R.id.options)
                trackView.setOnLongClickListener {
                    if (optionsView.visibility == View.GONE) {
                        optionsView.visibility = View.VISIBLE
                    } else {
                        optionsView.visibility = View.GONE
                    }
                    return@setOnLongClickListener true
                }

                linearLayoutScrollContent.addView(trackView)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackSummaryRepository.close()
        trackLocationsRepository.close()
        checkpointsRepository.close()
        wayPointsRepository.close()
        userRepository.close()
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(
            R.anim.slide_in_from_left,
            R.anim.slide_out_to_right
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(BUNDLE_SELECTED_REPLAYS, selectedItems)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectedItems = savedInstanceState.getSerializable(BUNDLE_SELECTED_REPLAYS) as? HashMap<Long, Int> ?: hashMapOf()
    }

    // ================================= HELPER FUNCTION =================================

    private fun setUpReplaySpinner(spinner: Spinner, track: TrackSummary, trackIcon: TrackIconImageView) {
        val displayOptionAdapter = HistorySpinnerAdapter(this)

        spinner.adapter = displayOptionAdapter
        spinner.setSelection(selectedItems[track.trackId] ?: 0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedItems[track.trackId] = position
                val intent = Intent(C.TRACK_SET_RABBIT)
                intent.putExtra(C.TRACK_SET_RABBIT_NAME, ReplaySpinnerItems.OPTIONS[position])
                intent.putExtra(C.TRACK_SET_RABBIT_VALUE, track.trackId)
                trackIcon.color = ReplaySpinnerItems.COLORS_MIN_SPEED[ReplaySpinnerItems.OPTIONS[position]]!!
                trackIcon.colorMax = ReplaySpinnerItems.COLORS_MAX_SPEED[ReplaySpinnerItems.OPTIONS[position]]!!
                trackIcon.invalidate()
                LocalBroadcastManager.getInstance(this@HistoryActivity).sendBroadcast(intent)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        trackIcon.color = Color.RED //ReplaySpinnerItems.COLORS_MIN_SPEED[ReplaySpinnerItems.OPTIONS[selectedItems[track.trackId] ?: 0]]!!

        trackIcon.colorMax =
            ReplaySpinnerItems.COLORS_MAX_SPEED[ReplaySpinnerItems.OPTIONS[selectedItems[track.trackId] ?: 0]]!!
        trackIcon.invalidate()
    }

    private fun onExportClicked(trackSummary: TrackSummary) {
        if (!isPermissionsGranted) {
            return requestPermissions()
        }

        val locations = trackLocationsRepository.readTrackLocations(trackSummary.trackId, 0L, Long.MAX_VALUE)
        val checkpoints = checkpointsRepository.readTrackCheckpoints(trackSummary.trackId)
        val wayPoints = wayPointsRepository.readTrackWayPoints(trackSummary.trackId)

        val gpx = TrackUtils.serializeToGpx(locations, checkpoints, wayPoints)

        val serializer = TrackSerializer()
        serializer.saveGpx(gpx, trackSummary.name, this,
            {
                Toast.makeText(this, "Successfully exported gpx!", Toast.LENGTH_SHORT).show()
            },
            {
                Toast.makeText(this, "Error exporting gpx!", Toast.LENGTH_SHORT).show()
            }
        )

    }

    private fun onDeleteClicked(track: TrackSummary, trackView: View) {
        val alert = AlertDialog.Builder(this, R.style.AppCompatAlertWarnDialogStyle)
            .setTitle(ALERT_DELETE_TITLE)
            .setMessage(ALERT_DELETE_TEXT)
            .setPositiveButton(ALERT_DELETE_DELETE_TEXT) { _, _ ->
                run {
                    trackSummaryRepository.deleteTrack(track.trackId)
                    linearLayoutScrollContent.removeView(trackView)
                }
            }
            .setNegativeButton(ALERT_DELETE_CANCEL_TEXT, null)
            .create()
        alert.show()
    }

    private fun onRenameClicked(track: TrackSummary, trackView: View) {
        val textInputLayout = TextInputLayout(this)
        textInputLayout.setPadding(19, 0, 19, 0)
        val input = EditText(this)
        input.setTextColor(0xFFb8e7ff.toInt())
        input.setText(track.name)
        textInputLayout.addView(input)

        val alert = AlertDialog.Builder(this, R.style.AppCompatAlertInfoDialogStyle)
            .setTitle("Rename track")
            .setView(textInputLayout)
            .setPositiveButton("Rename") { _, _ ->
                // do some thing with input.text
                track.name = input.text.toString()
                trackView.findViewById<TextView>(R.id.track_name).text = track.name
                trackSummaryRepository.updateTrackName(track)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }.create()

        alert.show()
    }

    private fun requestPermissions() {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            Snackbar.make(
                findViewById(R.id.activity_main),
                C.SNAKBAR_REQUEST_EXTERNAL_STORAGE_ACCESS_TEXT, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(C.SNAKBAR_REQUEST_EXTERNAL_STORAGE_CONFIRM_TEXT) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE),
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
                arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE),
                C.REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }
    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
