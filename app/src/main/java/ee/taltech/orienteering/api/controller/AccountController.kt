package ee.taltech.orienteering.api.controller

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.fasterxml.jackson.databind.ObjectMapper
import ee.taltech.orienteering.api.WebApiHandler
import ee.taltech.orienteering.api.dto.LoginDto
import ee.taltech.orienteering.api.dto.LoginResponseDto
import ee.taltech.orienteering.api.dto.RegisterDto
import org.json.JSONObject


class AccountController private constructor(val context: Context) {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private const val BASE_URL = "https://sportmap.akaver.com/api/"
        private const val API_VERSION = 1.0

        private var instance: AccountController? = null
        private val mapper = ObjectMapper()

        @Synchronized
        fun getInstance(context: Context): AccountController {
            if (instance == null) {
                instance = AccountController(context)
            }
            return instance!!
        }
    }

    fun createAccount(registerDto: RegisterDto, onSuccess: (r: LoginResponseDto) -> Unit) {
        val handler = WebApiHandler.getInstance(context)
        val httpRequest = JsonObjectRequest(
            Request.Method.POST,
            "${BASE_URL}v${API_VERSION}/Account/Register",
            JSONObject(mapper.writeValueAsString(registerDto)),
            { response ->
                Log.d(TAG, response.toString())
                val responseDto = mapper.readValue(response.toString(), LoginResponseDto::class.java)
                handler.jwt.set(responseDto.token)
                onSuccess(responseDto)
            },
            {error ->
                Log.e(TAG, error.toString())
                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show()
            }
        )
        handler.addToRequestQueue(httpRequest)
    }

    fun login(loginDto: LoginDto, onSuccess: (r: LoginResponseDto) -> Unit = { }) {
        val handler = WebApiHandler.getInstance(context)
        val httpRequest = JsonObjectRequest(
            Request.Method.POST,
            "${BASE_URL}v${API_VERSION}/Account/Login",
            JSONObject(mapper.writeValueAsString(loginDto)),
            { response ->
                val responseDto = mapper.readValue(response.toString(), LoginResponseDto::class.java)
                handler.jwt.set(responseDto.token)
                onSuccess(responseDto)
            },
            {error ->
                Log.e(TAG, error.toString())
                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show()
            }
        )
        handler.addToRequestQueue(httpRequest)
    }
}