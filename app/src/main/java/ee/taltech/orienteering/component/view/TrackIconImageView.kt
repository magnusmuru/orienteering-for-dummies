package ee.taltech.orienteering.component.view

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import ee.taltech.orienteering.component.spinner.ReplaySpinnerItems
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation
import kotlin.math.*

class TrackIconImageView(context: Context,attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    companion object {
        const val PADDING = 0.05f

        val argbEvaluator = ArgbEvaluator()
    }

    var color: Int = ReplaySpinnerItems.COLORS_MIN_SPEED[ReplaySpinnerItems.NONE]!!
    var colorMax: Int = ReplaySpinnerItems.COLORS_MAX_SPEED[ReplaySpinnerItems.NONE]!!
    var track: List<TrackLocation>? = null
    var maxSpeed = 99.99

    override fun onDrawForeground(canvas: Canvas?) {
        super.onDrawForeground(canvas)

        if (track == null || track?.size == 0) return

        val minLat = track?.minByOrNull { location -> location.latitude }?.latitude ?: 0.0
        val maxLat = track?.maxByOrNull { location -> location.latitude }?.latitude ?: 0.0
        val minLng = track?.minByOrNull { location -> location.longitude }?.longitude ?: 0.0
        val maxLng = track?.maxByOrNull { location -> location.longitude }?.longitude ?: 0.0

        val latDelta = maxLat - minLat
        val lngMultiplier = cos((minLat + latDelta / 2) / 180f * PI)
        val lngDelta = (maxLng - minLng) * lngMultiplier

        val maxDelta = max(latDelta, lngDelta)
        val latOffset = (maxDelta - latDelta) / 2
        val lngOffset = (maxDelta - lngDelta) * lngMultiplier / 2

        val paint = Paint()
        paint.color = color
        paint.strokeWidth = 3f

        val paddedWidth = width * (1 - PADDING * 2)

        var last = track?.first()
        for (location in track!!) {
            val relSpeed = max(0.0, min(1.0, TrackLocation.calcDistanceBetween(location, last!!) /
                    ((location.elapsedTimestamp - (last.elapsedTimestamp)  + 1) / 1_000_000_000 / 3.6) / maxSpeed))

            paint.color = argbEvaluator.evaluate(relSpeed.pow(1).toFloat(), color, colorMax) as Int

            canvas?.drawLine(
                width * PADDING + ((lngOffset + location.longitude - minLng) * lngMultiplier / maxDelta * paddedWidth).toFloat(),
                height * (1 - PADDING) - ((latOffset + location.latitude - minLat) / maxDelta * paddedWidth).toFloat(),
                width * PADDING + ((lngOffset + last.longitude - minLng) * lngMultiplier/ maxDelta * paddedWidth).toFloat(),
                height * (1 - PADDING) - ((latOffset + last.latitude - minLat) / maxDelta * paddedWidth).toFloat(),
                paint
            )
            last = location
        }
    }
}