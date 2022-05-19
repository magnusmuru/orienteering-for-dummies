package ee.taltech.orienteering.db.repository

import android.content.ContentValues
import android.content.Context
import ee.taltech.orienteering.db.DatabaseHelper
import ee.taltech.orienteering.db.domain.OfflineSession

class OfflineSessionsRepository private constructor(context: Context) : IRepository {

    companion object {
        fun open(context: Context): OfflineSessionsRepository {
            return OfflineSessionsRepository(context)
        }
    }

    private val databaseHelper: DatabaseHelper = DatabaseHelper.getInstance(context)

    fun saveOfflineSession(trackId: Long) {
        val offlineSessionsValues = ContentValues()
        offlineSessionsValues.put(DatabaseHelper.KEY_TRACK_ID, trackId)
        databaseHelper.writableDatabase.beginTransaction()
        databaseHelper.writableDatabase.insert(DatabaseHelper.TABLE_OFFLINE_SESSIONS, null, offlineSessionsValues)
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
    }

    fun readOfflineSessions(): List<OfflineSession> {
        val offlineSessionsList = mutableListOf<OfflineSession>()

        val selectQuery = ("SELECT  * FROM " + DatabaseHelper.TABLE_OFFLINE_SESSIONS)

        val cursor = databaseHelper.readableDatabase.rawQuery(selectQuery, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val trackLocation = OfflineSession(
                        cursor.getLong(0),
                        cursor.getLong(1)
                    )
                    offlineSessionsList.add(trackLocation)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return offlineSessionsList
    }

    fun deleteOfflineSession(id: Long) {
        databaseHelper.writableDatabase.beginTransaction()
        databaseHelper.writableDatabase.delete(
            DatabaseHelper.TABLE_OFFLINE_SESSIONS,
            "${DatabaseHelper.KEY_ID} = ?",
            arrayOf(id.toString())
        )
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
    }

    override fun close() {
        databaseHelper.close()
    }
}