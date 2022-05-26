package ee.taltech.orienteering.util.serializer

import android.content.Context
import io.jenetics.jpx.GPX
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TrackSerializer {

    private var mExternalFile: File?=null

    fun saveGpx(gpx: GPX, name: String, context: Context, onSuccess: () -> Unit, onError: () -> Unit) {
        mExternalFile = File(context.filesDir, name.replace(" ", "_") + ".gpx")
        try {
            FileOutputStream(mExternalFile!!).use { out ->
                GPX.write(gpx, out)
            }
            onSuccess()
        } catch (e: IOException) {
            e.printStackTrace()
            onError()
        }
    }
}