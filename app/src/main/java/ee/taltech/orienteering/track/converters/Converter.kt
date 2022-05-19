package ee.taltech.orienteering.track.converters

import kotlin.math.abs

class Converter {
    companion object {
        fun longToHhMmSs(nanos: Long): String {
            val seconds = nanos / 1000 / 1000 / 1000
            val s = seconds % 60
            val m = seconds / 60 % 60
            val h = seconds / (60 * 60) % 24
            return String.format("%d:%02d:%02d", h, m, s)
        }

        fun distToString(distance: Double): String {
            return if (distance < 1000) "%.2f m".format(abs(distance)) else "%.1f km".format(distance / 1000)
        }

        fun speedToString(speed: Double, isSpeedMode: Boolean): String {
            if (isSpeedMode) {
                return (if (abs(speed) < 10) "%.2f km/h" else "%.1f km/h").format(abs(speed))
            }
            if (speed < 1 / 60.0) return "-:--"
            return "%d:%d".format(abs((1 / speed * 60).toInt()), abs((1 / speed * 60 * 60).toInt() % 60))
        }

        fun elevationToString(elevationGained: Double): String {
            return "%.1fm".format(elevationGained)
        }
    }
}