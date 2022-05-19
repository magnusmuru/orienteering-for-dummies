package ee.taltech.orienteering.activity

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import ee.taltech.orienteering.R
import ee.taltech.orienteering.component.imageview.TrackTypeIcons
import ee.taltech.orienteering.component.spinner.OverviewMode
import ee.taltech.orienteering.component.spinner.adapter.OverviewTypeSpinnerAdapter
import ee.taltech.orienteering.db.domain.User
import ee.taltech.orienteering.db.repository.TrackSummaryRepository
import ee.taltech.orienteering.db.repository.UserRepository
import ee.taltech.orienteering.detector.FlingDetector
import ee.taltech.orienteering.track.TrackType
import ee.taltech.orienteering.track.converters.Converter

class OverviewActivity : AppCompatActivity() {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
            var sum = 0L
            for (element in this) {
                sum += selector(element)
            }
            return sum
        }
    }

    private val trackSummaryRepository = TrackSummaryRepository.open(this)
    private val userRepository = UserRepository.open(this)

    private var user: User? = null

    private lateinit var flingDetector: FlingDetector

    private lateinit var overviewTypeSpinner: Spinner
    private lateinit var linearLayoutScrollContent: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overview)

        overviewTypeSpinner = findViewById(R.id.spinner_overview_type)
        linearLayoutScrollContent = findViewById(R.id.linear_scroll)

        flingDetector = FlingDetector(this)

        setUpSpinners()
    }

    override fun onStart() {
        super.onStart()
        user = userRepository.readUser()
    }

    override fun onDestroy() {
        super.onDestroy()
        trackSummaryRepository.close()
        userRepository.close()
    }


    // ======================================== FLING DETECTION =======================================

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        flingDetector.update(ev)
        return super.dispatchTouchEvent(ev)
    }

    // ============================================= HELPER FUNCTIONS ===================================================

    private fun showOverview(period: Long) {
        linearLayoutScrollContent.removeAllViews()
        val data = trackSummaryRepository.readTrackSummariesDuringPeriod(System.currentTimeMillis() - period, System.currentTimeMillis())
        data.groupBy { track -> track.type }
            .forEach { (typeInt, tracks) ->
                val overviewView = layoutInflater.inflate(R.layout.overview_item, linearLayoutScrollContent, false)
                overviewView.findViewById<TextView>(R.id.activity_name).text =
                    TrackTypeIcons.getString(TrackType.fromInt(typeInt) ?: TrackType.UNKNOWN)
                overviewView.findViewById<ImageView>(R.id.track_type_icon)
                    .setImageResource(TrackTypeIcons.getIcon(TrackType.fromInt(typeInt)!!))
                val totalDistance = tracks.sumByDouble { t -> t.distance }
                val totalDurationMoving = tracks.sumByLong { t -> t.durationMoving}
                overviewView.findViewById<TextView>(R.id.distance).text = Converter.distToString(totalDistance)
                overviewView.findViewById<TextView>(R.id.duration).text = Converter.longToHhMmSs(totalDurationMoving)
                overviewView.findViewById<TextView>(R.id.elevation_gained).text = Converter.distToString(tracks.sumByDouble { t -> t.elevationGained })
                overviewView.findViewById<TextView>(R.id.avg_speed).text =
                    Converter.speedToString(totalDistance / totalDurationMoving * 1_000_000_000 * 3.6, user?.speedMode ?: true)
                overviewView.findViewById<TextView>(R.id.max_speed).text =
                    Converter.speedToString(tracks.maxByOrNull { t -> t.maxSpeed }!!.maxSpeed, user?.speedMode ?: true)
                overviewView.findViewById<TextView>(R.id.train_count).text = tracks.size.toString()
                linearLayoutScrollContent.addView(overviewView)
            }
    }

    private fun setUpSpinners() {
        val displayOptionAdapter = OverviewTypeSpinnerAdapter(this)
        overviewTypeSpinner.adapter = displayOptionAdapter
        overviewTypeSpinner.setSelection(0)
        overviewTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                showOverview(OverviewMode.getDurationMillis(OverviewMode.OPTIONS[position]))
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}