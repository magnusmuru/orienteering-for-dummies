package ee.taltech.orienteering.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ee.taltech.orienteering.C
import ee.taltech.orienteering.R
import ee.taltech.orienteering.component.spinner.adapter.TrackTypeSpinnerAdapter
import ee.taltech.orienteering.db.domain.User
import ee.taltech.orienteering.db.repository.UserRepository
import ee.taltech.orienteering.track.TrackType
import ee.taltech.orienteering.track.converters.Converter
import ee.taltech.orienteering.track.pracelable.DetailedTrackData


class DetailActivity : AppCompatActivity() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private const val ALERT_RESET_TITLE = "Reset track?"
        private const val ALERT_RESET_TEXT = "Last track data will be lost! Do you want to continue?"
        private const val ALERT_RESET_CANCEL_TEXT = "Cancel"
        private const val ALERT_RESET_RESET_TEXT = "Reset"

        private const val ALERT_SAVE_TITLE = "Save track?"
        private const val ALERT_SAVE_TEXT = "After saving current session will be cleared! Do you want to continue?"
        private const val ALERT_SAVE_CANCEL_TEXT = "Cancel"
        private const val ALERT_SAVE_RESET_TEXT = "Save"

        private const val BUNDLE_TRACK_TYPE = "track_type"
    }

    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()

    private val userRepository = UserRepository.open(this)

    private var trackType = 0

    private var user: User? = null

    private lateinit var buttonSave: Button
    private lateinit var buttonReset: Button

    private lateinit var txtViewDuration: TextView
    private lateinit var txtViewAverageSpeed: TextView
    private lateinit var txtViewElevationGained: TextView
    private lateinit var txtViewDrift: TextView
    private lateinit var txtViewDistance: TextView
    private lateinit var txtViewElevation: TextView
    private lateinit var txtViewCheckpoints: TextView
    private lateinit var spinnerTrackType: Spinner
    private lateinit var textEditTrackName: EditText

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_NO_TITLE)
        actionBar?.hide()
        supportActionBar?.hide()

        setContentView(R.layout.activity_detail)
        broadcastReceiverIntentFilter.addAction(C.TRACK_DETAIL_RESPONSE)  // Remove?

        buttonSave = findViewById(R.id.btn_save)
        buttonReset = findViewById(R.id.btn_reset)

        txtViewDuration = findViewById(R.id.duration)
        txtViewAverageSpeed = findViewById(R.id.avg_speed)
        txtViewElevationGained = findViewById(R.id.elevation_gained)
        txtViewDrift = findViewById(R.id.drift)
        txtViewDistance = findViewById(R.id.total_distance)
        txtViewElevation = findViewById(R.id.avg_elevation)
        txtViewCheckpoints = findViewById(R.id.checkpoints_count)
        spinnerTrackType = findViewById(R.id.spinner_track_type)
        textEditTrackName = findViewById(R.id.track_name)

        textEditTrackName.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
            val intent = Intent(C.TRACK_SET_NAME)
            intent.putExtra(C.TRACK_SET_NAME_DATA, textEditTrackName.text.toString())
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    // ============================================== ON CLICK CALLBACKS ==============================================

    fun buttonSaveOnClick(view: View) {
        val alert = AlertDialog.Builder(this, R.style.AppCompatAlertInfoDialogStyle)
            .setTitle(ALERT_SAVE_TITLE)
            .setMessage(ALERT_SAVE_TEXT)
            .setPositiveButton(ALERT_SAVE_RESET_TEXT) { _, _ ->
                run {
                    val intent = Intent(C.TRACK_SAVE)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            }
            .setNegativeButton(ALERT_SAVE_CANCEL_TEXT, null)
            .create()
        alert.show()
    }

    fun buttonResetOnClick(view: View) {
        val alert = AlertDialog.Builder(this, R.style.AppCompatAlertWarnDialogStyle)
            .setTitle(ALERT_RESET_TITLE)
            .setMessage(ALERT_RESET_TEXT)
            .setPositiveButton(ALERT_RESET_RESET_TEXT) { _, _ ->
                run {
                    val intent = Intent(C.TRACK_RESET)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                }
            }
            .setNegativeButton(ALERT_RESET_CANCEL_TEXT, null)
            .create()
        alert.show()

    }


    // ============================================== LIFECYCLE CALLBACKS =============================================
    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
        user = userRepository.readUser()
        if (trackType == TrackType.UNKNOWN.value) {
            trackType = (user?.defaultActivityType ?: TrackType.UNKNOWN).value
        }
        setUpTypeSpinner(spinnerTrackType)
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        //LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)
        requestData(true)
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        overridePendingTransition(
            R.anim.slide_in_from_top,
            R.anim.slide_out_to_bottom
        )
    }


    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
        requestData(false)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        userRepository.close()
    }

    override fun onRestart() {
        Log.d(TAG, "onRestart")
        super.onRestart()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        trackType = savedInstanceState.getInt(BUNDLE_TRACK_TYPE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(BUNDLE_TRACK_TYPE, trackType)
        super.onSaveInstanceState(outState)
    }

    // ====================================== BROADCAST RECEIVER ======================================

    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, intent!!.action!!)
            when (intent.action) {
                C.TRACK_DETAIL_RESPONSE -> onTrackDetailResponse(intent)
            }
        }

        // ----------------------------------- BROADCAST RECEIVER CALLBACKS ------------------------------
        private fun onTrackDetailResponse(intent: Intent) {
            val data: DetailedTrackData = intent.getParcelableExtra(C.TRACK_DETAIL_DATA) ?: return
            if (!textEditTrackName.isFocused) {
                textEditTrackName.setText(data.name)
            }
            txtViewDuration.text = Converter.longToHhMmSs(data.duration);
            txtViewAverageSpeed.text = Converter.speedToString(data.getSpeed(), user?.speedMode ?: true)
            txtViewElevationGained.text = Converter.elevationToString(data.elevationGained)
            txtViewDrift.text = Converter.distToString(data.drift)
            txtViewDistance.text = Converter.distToString(data.distance)
            txtViewElevation.text = Converter.elevationToString(data.averageElevation)
            txtViewCheckpoints.text = data.checkpointsCount.toString()
            spinnerTrackType.setSelection(data.type) // TODO: Validate
        }
    }

    // ==================================== HELPER FUNCTIONS =============================================
    private fun requestData(keepBroadcasting: Boolean) {
        val intent = Intent(C.TRACK_DETAIL_REQUEST)
        intent.putExtra(C.TRACK_DETAIL_REQUEST_DATA, keepBroadcasting)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun setUpTypeSpinner(spinner: Spinner) {
        val displayOptionAdapter = TrackTypeSpinnerAdapter(this)

        spinner.adapter = displayOptionAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val intent = Intent(C.TRACK_SET_TYPE)
                intent.putExtra(C.TRACK_SET_TYPE_DATA, position)
                LocalBroadcastManager.getInstance(this@DetailActivity).sendBroadcast(intent)
                trackType = position
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spinner.setSelection(trackType)
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager: InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
