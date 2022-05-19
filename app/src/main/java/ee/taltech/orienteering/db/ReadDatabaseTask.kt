package ee.taltech.orienteering.db

import android.os.AsyncTask

class ReadDatabaseTask<T>(private val onResult: (result: List<T>) -> Unit) : AsyncTask<() -> List<T>, Int, List<T>>() {

    override fun doInBackground(vararg params: (() -> List<T>)): List<T> {
        val result = mutableListOf<T>()
        params.forEach { query ->
            result.addAll(query())
        }
        return result
    }

    override fun onPostExecute(result: List<T>) {
        onResult(result)
    }
}