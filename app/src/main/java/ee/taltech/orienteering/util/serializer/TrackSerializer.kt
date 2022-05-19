package ee.taltech.orienteering.util.serializer

import android.content.Context
import android.os.Environment
import android.widget.Toast
import io.jenetics.jpx.GPX
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TrackSerializer {
    companion object {
        private const val FILE_PATH = "orienteering/gpx"
    }
    private var mExternalFile: File?=null

    private val isExternalStorageReadOnly: Boolean get() {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED_READ_ONLY == extStorageState
    }
    private val isExternalStorageAvailable: Boolean get() {
        val extStorageState = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == extStorageState
    }

    fun saveGpx(gpx: GPX, name: String, context: Context, onSuccess: () -> Unit, onError: () -> Unit) {
        if (!isExternalStorageAvailable || !isExternalStorageReadOnly) {
            Toast.makeText(context, "External storage unavailable!", Toast.LENGTH_SHORT).show()
        }
        mExternalFile = File(context.getExternalFilesDir(FILE_PATH), name.replace(" ", "_") + ".gpx")
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