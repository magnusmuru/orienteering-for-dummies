package ee.taltech.orienteering.component.spinner

class DisplayMode {
   companion object {
       const val CENTERED = "Centered"
       const val FREE_MOVE = "Free move"
       val OPTIONS = arrayOf(
           CENTERED,
           FREE_MOVE
       )
   }
}