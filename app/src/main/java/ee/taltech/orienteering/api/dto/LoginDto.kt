package ee.taltech.orienteering.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class LoginDto(
    @JsonProperty("email")
    var email: String,
    @JsonProperty("password")
    var password: String
)