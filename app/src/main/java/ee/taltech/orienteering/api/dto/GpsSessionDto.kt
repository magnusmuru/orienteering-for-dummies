package ee.taltech.orienteering.api.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
class GpsSessionDto(
    @JsonProperty("id")
    val id: String? = null,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("description")
    val description: String = "",
    @JsonProperty("recordedAt")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss'Z'", timezone="GMT+03:00")
    val recordedAt: Date,
    @JsonProperty("duration")
    val duration: Long? = null,
    @JsonProperty("speed")
    val speed: Double? = null,
    @JsonProperty("distance")
    val distance: Double? = null,
    @JsonProperty("climb")
    val climb: Double? = null,
    @JsonProperty("descent")
    val descent: Double? = null,
    @JsonProperty("paceMin")
    val paceMin: Double? = 60.0,
    @JsonProperty("paceMax")
    val paceMax: Double? = 61.0,
    @JsonProperty("gpsSessionTypeId")
    val gpsSessionTypeId: String? = null,
    @JsonProperty("appUserId")
    val appUserId: String? = null
)