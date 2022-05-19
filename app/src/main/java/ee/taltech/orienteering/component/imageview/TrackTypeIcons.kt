package ee.taltech.orienteering.component.imageview

import ee.taltech.orienteering.R
import ee.taltech.orienteering.track.TrackType

class TrackTypeIcons {
    companion object {
        const val UNKNOWN = "Activity"
        const val RUNNING = "Running"
        const val CYCLING = "Cycling"
        const val SKIING = "Skiing"
        const val SOCCER = "Soccer"
        const val ROWING = "Rowing"
        const val SWIMMING = "Swimming"

        val OPTIONS = arrayOf(
            UNKNOWN, RUNNING, CYCLING, SKIING, SOCCER, ROWING, SWIMMING
        )

        fun getTrackType(string: String): TrackType {
            return when(string) {
                UNKNOWN -> TrackType.UNKNOWN
                RUNNING -> TrackType.RUNNING
                CYCLING -> TrackType.CYCLING
                SKIING -> TrackType.SKIING
                SOCCER -> TrackType.SOCCER
                ROWING -> TrackType.ROWING
                SWIMMING -> TrackType.SWIMMING
                else -> TrackType.UNKNOWN
            }
        }

        fun getIcon(trackType: TrackType): Int {
            return when (trackType) {
                TrackType.UNKNOWN -> R.drawable.ic_activity_24px
                TrackType.RUNNING -> R.drawable.ic_run_24px
                TrackType.CYCLING -> R.drawable.ic_bike_24px
                TrackType.SKIING -> R.drawable.ic_skiing
                TrackType.SOCCER -> R.drawable.ic_sports_soccer_24px
                TrackType.ROWING -> R.drawable.ic_rowing_24px
                TrackType.SWIMMING -> R.drawable.ic_swimming_24px
            }
        }

        fun getString(trackType: TrackType): String {
            return when (trackType) {
                TrackType.UNKNOWN -> UNKNOWN
                TrackType.RUNNING -> RUNNING
                TrackType.CYCLING -> CYCLING
                TrackType.SKIING -> SKIING
                TrackType.SOCCER -> SOCCER
                TrackType.ROWING -> ROWING
                TrackType.SWIMMING -> SWIMMING
            }
        }
    }
}