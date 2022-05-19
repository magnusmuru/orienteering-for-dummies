package ee.taltech.orienteering.db.repository

import android.content.ContentValues
import android.content.Context
import ee.taltech.orienteering.db.DatabaseHelper
import ee.taltech.orienteering.track.pracelable.loaction.WayPoint

class WayPointsRepository private constructor(context: Context): IRepository {

    companion object {
        fun open(context: Context): WayPointsRepository {
            return WayPointsRepository(context)
        }
    }

    private val databaseHelper: DatabaseHelper = DatabaseHelper.getInstance(context)

    fun saveWayPointToTrack(wayPoints: List<WayPoint>, trackId: Long) {
        databaseHelper.writableDatabase.beginTransaction()
        wayPoints.forEachIndexed { index, wp ->
            val wpValues = ContentValues()
            wpValues.put(DatabaseHelper.KEY_TRACK_ID, trackId)
            wpValues.put(DatabaseHelper.KEY_WAY_POINT_NUMBER, index)
            wpValues.put(DatabaseHelper.KEY_WAY_POINT_LATITUDE, wp.latitude)
            wpValues.put(DatabaseHelper.KEY_WAY_POINT_LONGITUDE, wp.longitude)
            wpValues.put(DatabaseHelper.KEY_WAY_POINT_ADDED_TIMESTAMP, wp.timeAdded)
            wpValues.put(DatabaseHelper.KEY_WAY_POINT_REMOVED_TIMESTAMP, wp.timeRemoved)
           databaseHelper.writableDatabase.insert(DatabaseHelper.TABLE_WAY_POINTS, null, wpValues)
        }
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
    }

    fun readTrackWayPoints(trackId: Long): List<WayPoint> {
        val trackList = mutableListOf<WayPoint>()

        val selectQuery = ("SELECT  * FROM " + DatabaseHelper.TABLE_WAY_POINTS
                + " WHERE " + DatabaseHelper.KEY_TRACK_ID + " = " + trackId.toString()
                + " ORDER BY " + DatabaseHelper.KEY_WAY_POINT_NUMBER + " ASC")

        val cursor = databaseHelper.readableDatabase.rawQuery(selectQuery, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val trackLocation = WayPoint(
                        cursor.getDouble(3),
                        cursor.getDouble(4),
                        cursor.getLong(5),
                        cursor.getLong(6)
                    )
                    trackList.add(trackLocation)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return trackList
    }

    override fun close() {
        databaseHelper.close()
    }
}