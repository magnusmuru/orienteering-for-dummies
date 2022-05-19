package ee.taltech.orienteering.component.spinner

class OverviewMode {
    companion object {
        const val WEEK = "1 Week"
        const val TWO_WEEKS = "2 Weeks"
        const val MONTH = "1 Month"
        const val THREE_MONTHS = "3 months"
        const val SIX_MONTH = "6 months"
        const val YEAR = "1 Year"
        val OPTIONS = arrayOf(
            WEEK, TWO_WEEKS, MONTH, THREE_MONTHS, SIX_MONTH, YEAR
        )

        fun getDurationMillis(period: String): Long {
            return when (period) {
                WEEK -> 7 * 24 * 60 * 60 * 1000L
                TWO_WEEKS -> 2 * getDurationMillis(WEEK)
                MONTH -> 30 * 24 * 60 * 60 * 1000L
                THREE_MONTHS -> 3 * getDurationMillis(MONTH)
                SIX_MONTH -> 6 * getDurationMillis(MONTH)
                YEAR -> 365 * 24 * 60 * 60 * 1000L
                else -> 0L
            }
        }
    }
}