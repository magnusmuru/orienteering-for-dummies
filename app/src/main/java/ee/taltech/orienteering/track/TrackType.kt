package ee.taltech.orienteering.track

enum class TrackType(val value: Int) {

    UNKNOWN(0),
    RUNNING(1),
    CYCLING(2),
    SKIING(3),
    SOCCER(4),
    ROWING(5),
    SWIMMING(6);

    companion object {
        private val map = values().associateBy(TrackType::value)
        fun fromInt(type: Int) = map[type]
    }
}