package ee.taltech.orienteering.api.dto

import com.fasterxml.jackson.annotation.JsonProperty

class BulkUploadResponseDto(
    @JsonProperty("locationsAdded")
    val locationsAdded: Long,
    @JsonProperty("locationsReceived")
    val locationsReceived: Long,
    @JsonProperty("gpsSessionId")
    val gpsSessionId: String
)