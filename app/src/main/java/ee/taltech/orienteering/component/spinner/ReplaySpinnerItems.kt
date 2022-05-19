package ee.taltech.orienteering.component.spinner

class ReplaySpinnerItems {
    companion object {
        const val BLUE = "Blue"
        const val GREEN = "Green"
        const val ORANGE = "Orange"
        const val PURPLE = "Purple"
        const val NONE = "None"
        val OPTIONS = arrayOf(
            NONE,
            BLUE,
            GREEN,
            ORANGE,
            PURPLE
        )

        val COLORS = hashMapOf(
            NONE to 0xFFFFFFFF.toInt(),
            BLUE to 0xFF0037ff.toInt(),
            GREEN to 0xFF0f7a00.toInt(),
            ORANGE to 0xFFc92700.toInt(),
            PURPLE to 0xFF6200b0.toInt()
        )

        val COLORS_MIN_SPEED = hashMapOf(
            NONE to 0xFFa60000.toInt(),
            BLUE to 0xFF0900bd.toInt(),
            GREEN to 0xFF035200.toInt(),
            ORANGE to 0xFFb30c00.toInt(),
            PURPLE to 0xFF4b009c.toInt()
        )

        val COLORS_MAX_SPEED = hashMapOf(
            NONE to 0xFFffc9c9.toInt(),
            BLUE to 0xFFc7e9ff.toInt(),
            GREEN to 0xFFb8ffad.toInt(),
            ORANGE to 0xFFfcce4e.toInt(),
            PURPLE to 0xFFffc2ff.toInt()
        )
    }
}