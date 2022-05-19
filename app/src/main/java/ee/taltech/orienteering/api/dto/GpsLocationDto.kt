package ee.taltech.orienteering.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import ee.taltech.orienteering.track.pracelable.loaction.Checkpoint
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation
import ee.taltech.orienteering.track.pracelable.loaction.WayPoint
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
class GpsLocationDto(
    @JsonProperty("id")
    val id: String?,
    @JsonProperty("recordedAt")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="GMT+03:00")
    val recordedAt: Date,
    @JsonProperty("latitude")
    val latitude: Double,
    @JsonProperty("longitude")
    val longitude: Double,
    @JsonProperty("accuracy")
    val accuracy: Double,
    @JsonProperty("altitude")
    val altitude: Double,
    @JsonProperty("verticalAccuracy")
    val verticalAccuracy: Double,
    @JsonProperty("appUserId")
    val appUserId: String?,
    @JsonProperty("gpsSessionId")
    val gpsSessionId: String?,
    @JsonProperty("gpsLocationTypeId")
    val gpsLocationTypeId: String?
) {
    companion object{

        fun fromTrackLocation(location: TrackLocation, gpsSessionId: String): GpsLocationDto {
            return GpsLocationDto(
                id = null,
                recordedAt = Date(location.timestamp),
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy.toDouble(),
                altitude = location.altitude,
                verticalAccuracy = location.altitudeAccuracy.toDouble(),
                appUserId = null,
                gpsSessionId = gpsSessionId,
                gpsLocationTypeId = "00000000-0000-0000-0000-000000000001"
            )
        }

        fun fromCheckpoint(cp: Checkpoint, gpsSessionId: String): GpsLocationDto {
            return GpsLocationDto(
                id = null,
                recordedAt = Date(cp.timestamp),
                latitude = cp.latitude,
                longitude = cp.longitude,
                accuracy = cp.accuracy,
                altitude = cp.altitude,
                verticalAccuracy = cp.altitudeAccuracy,
                appUserId = null,
                gpsSessionId = gpsSessionId,
                gpsLocationTypeId = "00000000-0000-0000-0000-000000000003"
            )
        }

        fun fromWayPoint(wp: WayPoint, gpsSessionId: String): GpsLocationDto {
            return GpsLocationDto(
                id = null,
                recordedAt = Date(wp.timeAdded),
                latitude = wp.latitude,
                longitude = wp.longitude,
                accuracy = 0.0,
                altitude = 0.0,
                verticalAccuracy = 0.0,
                appUserId = null,
                gpsSessionId = gpsSessionId,
                gpsLocationTypeId = "00000000-0000-0000-0000-000000000002"
            )
        }
    }
}