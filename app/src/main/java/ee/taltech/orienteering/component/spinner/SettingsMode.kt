package ee.taltech.orienteering.component.spinner

class SettingsMode {
    companion object {
        const val SETTINGS = "Settings"
        const val CURRENT_ACTIVITY = "Current"
        const val USER_ACTIVITY = "User"
        const val HISTORY_ACTIVITY = "History"
        const val OVERVIEW_ACTIVITY = "Overview"

        val OPTIONS = arrayOf(
            SETTINGS,
            CURRENT_ACTIVITY,
            USER_ACTIVITY,
            HISTORY_ACTIVITY,
            OVERVIEW_ACTIVITY
        )
    }
}