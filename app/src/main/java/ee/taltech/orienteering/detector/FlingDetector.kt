package ee.taltech.orienteering.detector

import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class FlingDetector(context: Context) : GestureDetector.OnGestureListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private val detector: GestureDetector = GestureDetector(context, this)

    var onFlingUp: Runnable? = null
    var onFlingDown: Runnable? = null
    var onFlingLeft: Runnable? = null
    var onFlingRight: Runnable? = null

    var flingMinSpeedX = 2000f
    var flingMinSpeedY = 2000f
    var flingMinDistanceX = 50f
    var flingMinDistanceY = 60f


    fun update(event: MotionEvent?) {
        detector.onTouchEvent(event)
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return true
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val distanceX: Float = e1!!.x - e2!!.x
        val distanceY: Float = e1.x - e2.y
        val time: Long = e2.downTime - e1.downTime

        if (velocityX > flingMinSpeedX && abs(distanceX) > flingMinDistanceX) {
            // fling right
            Log.d(TAG, "fling right!")
            onFlingRight?.run()
        }
        if (velocityX < -flingMinSpeedX && abs(distanceX) > flingMinDistanceX) {
            // fling left
            Log.d(TAG, "fling left!")
            onFlingLeft?.run()
        }
        if (velocityY > flingMinSpeedY && abs(distanceY) > flingMinDistanceY) {
            // fling down
            Log.d(TAG, "fling down!")
            onFlingDown?.run()
        }
        if (velocityY < -flingMinSpeedY && abs(distanceY) > flingMinDistanceY) {
            // fling up
            Log.d(TAG, "fling up!")
            onFlingUp?.run()
        }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
        return
    }
}