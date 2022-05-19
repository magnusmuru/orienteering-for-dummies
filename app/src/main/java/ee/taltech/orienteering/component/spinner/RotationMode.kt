package ee.taltech.orienteering.component.spinner

class RotationMode {
    companion object {
        const val NORTH_UP = "North Up"
        const val USER_CHOSEN_UP = "Free rotate"
        const val DIRECTION_UP = "Direction up"
        val OPTIONS = arrayOf(
                NORTH_UP,
                USER_CHOSEN_UP,
                DIRECTION_UP
        )
    }
}