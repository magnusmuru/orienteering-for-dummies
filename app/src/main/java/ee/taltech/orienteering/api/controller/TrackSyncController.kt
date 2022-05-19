package ee.taltech.orienteering.api.controller

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.fasterxml.jackson.databind.ObjectMapper
import ee.taltech.orienteering.api.WebApiHandler
import ee.taltech.orienteering.api.dto.BulkUploadResponseDto
import ee.taltech.orienteering.api.dto.GpsLocationDto
import ee.taltech.orienteering.api.dto.GpsSessionDto
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


class TrackSyncController private constructor(val context: Context) {

    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private const val BUNDLE_MAX_LOCATIONS = 250

        private var instance: TrackSyncController? = null
        private val mapper = ObjectMapper()

        @Synchronized
        fun getInstance(context: Context): TrackSyncController {
            if (instance == null) {
                instance = TrackSyncController(context)
            }
            return instance!!
        }
    }

    fun createNewSession(gpsSessionDto: GpsSessionDto, onSuccess: (r: GpsSessionDto) -> Unit, onError: () -> Unit) {
        val handler = WebApiHandler.getInstance(context)
        Log.d(TAG, mapper.writeValueAsString(gpsSessionDto))

        handler.makeAuthorizedRequest(
            "GpsSessions",
            JSONObject(mapper.writeValueAsString(gpsSessionDto)),
            { response ->
                Log.d(TAG, response.toString())
                val responseDto = mapper.readValue(response.toString(), GpsSessionDto::class.java)
                onSuccess(responseDto)
            },
            { error ->
                Log.e(TAG, error.toString())
                if (error.networkResponse != null) {
                    Log.d(TAG, String(error.networkResponse.data, Charset.defaultCharset()))
                }
                onError()
                // Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
            })
    }

    fun addLocationToSession(gpsLocationDto: GpsLocationDto, onError: () -> Unit) {
        val handler = WebApiHandler.getInstance(context)
        handler.makeAuthorizedRequest(
            "GpsLocations",
            JSONObject(mapper.writeValueAsString(gpsLocationDto)),
            { response ->
                Log.d(TAG, response.toString())
            }, { error ->
                Log.e(TAG, error.toString())
                Log.d(TAG, String(error.networkResponse.data, Charset.defaultCharset()))
                onError()
                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show()
            })
    }

    fun addMultipleLocationsToSession(
        gpsLocations: List<GpsLocationDto>,
        gpsSessionId: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val handler = WebApiHandler.getInstance(context)

        val chuncks = gpsLocations.chunked(BUNDLE_MAX_LOCATIONS)
        var err = AtomicBoolean(false)
        var count: AtomicInteger = AtomicInteger(chuncks.size)
        chuncks.forEach { chunk ->
            handler.makeAuthorizedArrayRequest(
                "GpsLocations/bulkupload/$gpsSessionId",
                JSONArray(mapper.writeValueAsString(chunk)),
                { response ->
                    Log.d(TAG, response.toString())
                    val i = count.decrementAndGet()
                    val responseDto = mapper.readValue(response[0].toString(), BulkUploadResponseDto::class.java)
                    if (responseDto.locationsAdded != responseDto.locationsReceived) {
                        err.set(true)
                    }
                    if (i == 0) {
                        if (err.get()) {
                            onError() // <- Something more elegant here? Not too much info to work with tho
                        } else {
                            onSuccess()
                        }
                    }
                }, { error ->
                    Log.e(TAG, error.toString())
                    val i = count.decrementAndGet()
                    if (error.networkResponse != null) {
                        Log.d(TAG, String(error.networkResponse.data, Charset.defaultCharset()))
                    }
                    err.set(true)
                    if (i == 0) {
                        onError()
                    }
                })
        }
    }
}