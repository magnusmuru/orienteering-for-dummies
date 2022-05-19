package ee.taltech.orienteering.db.repository

import android.content.ContentValues
import android.content.Context
import ee.taltech.orienteering.db.DatabaseHelper
import ee.taltech.orienteering.track.pracelable.loaction.TrackLocation

class TrackLocationsRepository private constructor(context: Context): IRepository {

    companion object {
        fun open(context: Context): TrackLocationsRepository {
            return TrackLocationsRepository(context)
        }
    }

    private val databaseHelper: DatabaseHelper = DatabaseHelper.getInstance(context)

    fun saveLocationToTrack(locations: List<TrackLocation>, trackId: Long) {
        databaseHelper.writableDatabase.beginTransaction()
        locations.forEachIndexed { index, location ->
            val locationValues = ContentValues()
            locationValues.put(DatabaseHelper.KEY_TRACK_ID, trackId)
            locationValues.put(DatabaseHelper.KEY_LOCATION_NUMBER, index)
            locationValues.put(DatabaseHelper.KEY_LOCATION_LATITUDE, location.latitude)
            locationValues.put(DatabaseHelper.KEY_LOCATION_LONGITUDE, location.longitude)
            locationValues.put(DatabaseHelper.KEY_LOCATION_ALTITUDE, location.altitude)
            locationValues.put(DatabaseHelper.KEY_LOCATION_ACCURACY, location.accuracy)
            locationValues.put(DatabaseHelper.KEY_LOCATION_ALTITUDE_ACCURACY, location.altitudeAccuracy)
            locationValues.put(DatabaseHelper.KEY_LOCATION_TIME, location.timestamp)
            locationValues.put(DatabaseHelper.KEY_LOCATION_TIME_ELAPSED, location.elapsedTimestamp)
            databaseHelper.writableDatabase.insert(DatabaseHelper.TABLE_LOCATIONS, null, locationValues)
        }

        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
    }

    fun readTrackLocations(trackId: Long, startTime: Long, endTime: Long): List<TrackLocation> {
        val trackList = mutableListOf<TrackLocation>()

        val selectQuery = ("SELECT  * FROM " + DatabaseHelper.TABLE_LOCATIONS
                + " WHERE " + DatabaseHelper.KEY_TRACK_ID + " = " + trackId.toString()
                + " AND " + DatabaseHelper.KEY_LOCATION_TIME_ELAPSED + " BETWEEN " + startTime + " AND " + endTime
                + " ORDER BY " + DatabaseHelper.KEY_LOCATION_NUMBER + " ASC")

        val cursor = databaseHelper.readableDatabase.rawQuery(selectQuery, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val trackLocation = TrackLocation(
                        cursor.getDouble(3),
                        cursor.getDouble(4),
                        cursor.getDouble(5),
                        cursor.getFloat(6),
                        cursor.getFloat(7),
                        cursor.getLong(8),
                        cursor.getLong(9)
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