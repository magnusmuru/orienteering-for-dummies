package ee.taltech.orienteering.db.domain

import ee.taltech.orienteering.track.TrackType

class User(
    val userId: Long?,
    val username: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    var speedMode: Boolean = true,
    var defaultActivityType: TrackType = TrackType.UNKNOWN,
    var autoSync: Boolean = true,
    var syncInterval: Long = 30 * 1000L
)