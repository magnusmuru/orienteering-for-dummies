package ee.taltech.orienteering.api

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.android.volley.*
import com.android.volley.Request.Method.POST
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicReference


class WebApiHandler private constructor(var context: Context) {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
        private var instance: AtomicReference<WebApiHandler?> = AtomicReference()
        private const val BASE_URL = "https://sportmap.akaver.com/api/"
        private const val API_VERSION = 1.0

        @Synchronized
        fun getInstance(context: Context): WebApiHandler {
            if (instance.get() == null) {
                instance.set(WebApiHandler(context))
            }
            return instance.get()!!
        }
    }

    var jwt: AtomicReference<String?> = AtomicReference()

    private var requestQueue: RequestQueue? = null
        get() {
            if (field == null) {
                field = Volley.newRequestQueue(context)
            }
            return field
        }

    @Synchronized
    fun <T> addToRequestQueue(request: Request<T>, tag: String = TAG) {
        Log.d(TAG, request.url)
        request.tag = tag
        requestQueue?.add(request)
    }

    @Synchronized
    fun cancelPendingRequest(tag: String) {
        if (requestQueue != null) {
            requestQueue!!.cancelAll(if (TextUtils.isEmpty(tag)) TAG else tag)
        }
    }

    fun makeAuthorizedRequest(
        url: String,
        json: JSONObject,
        onSuccess: (r: JSONObject) -> Unit,
        onError: (error: VolleyError) -> Unit
    ) {
        if (jwt.get() == null) {
            onError(VolleyError())
            return
        }
        val httpRequest = object : JsonObjectRequest(
            POST,
            "${BASE_URL}v${API_VERSION}/$url",
            json,
            Response.Listener { response ->
                Log.d(TAG, response.toString())
                onSuccess(response)
            },
            Response.ErrorListener { error ->
                Log.e(TAG, error.toString())
                if (error.networkResponse != null) {
                    Log.d(TAG, String(error.networkResponse.data, Charset.defaultCharset()))
                }
                onError(error)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + jwt.get()!!
                return headers
            }
        }
        addToRequestQueue(httpRequest)
    }

    fun makeAuthorizedArrayRequest(
        url: String,
        json: JSONArray,
        onSuccess: (r: JSONArray) -> Unit,
        onError: (error: VolleyError) -> Unit
    ) {
        if (jwt.get() == null) {
            onError(VolleyError())
            return
        }
        val httpRequest = object : JsonArrayRequest(
            POST,
            "${BASE_URL}v${API_VERSION}/$url",
            json,
            Response.Listener { response ->
                Log.d(TAG, response.toString())
                onSuccess(response)
            },
            Response.ErrorListener { error ->
                Log.e(TAG, error.toString())
                if (error.networkResponse != null) {
                    Log.d(TAG, String(error.networkResponse.data, Charset.defaultCharset()))
                }
                onError(error)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + jwt.get()!!
                return headers
            }

            override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONArray> {
                return try {
                    val jsonString = String(response!!.data, Charset.defaultCharset())
                    val arr = JSONArray()
                    arr.put(JSONObject(jsonString))
                    Response.success(
                        arr,
                        HttpHeaderParser.parseCacheHeaders(response)
                    )
                } catch (je: JSONException) {
                    Response.error(ParseError(je))
                }
            }
        }
        addToRequestQueue(httpRequest)
    }
}
