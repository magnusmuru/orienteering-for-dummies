package ee.taltech.orienteering.db.repository

import android.content.ContentValues
import android.content.Context
import ee.taltech.orienteering.db.DatabaseHelper
import ee.taltech.orienteering.db.domain.TrackSummary
import ee.taltech.orienteering.track.Track
import ee.taltech.orienteering.track.TrackType

class TrackSummaryRepository private constructor(context: Context) : IRepository {

    companion object {
        fun open(context: Context): TrackSummaryRepository {
            return TrackSummaryRepository(context)
        }
    }

    private val databaseHelper: DatabaseHelper = DatabaseHelper.getInstance(context)


    fun saveTrack(track: Track): Long {
        val trackValues = ContentValues()
        trackValues.put(DatabaseHelper.KEY_TRACK_NAME, track.name)
        trackValues.put(DatabaseHelper.KEY_TRACK_TYPE, track.type.value)
        trackValues.put(DatabaseHelper.KEY_TRACK_START_STAMP, track.startTime)
        trackValues.put(DatabaseHelper.KEY_TRACK_START_ELAPSED, track.startTimeElapsed)
        trackValues.put(DatabaseHelper.KEY_TRACK_END_STAMP, track.lastLocation!!.timestamp)
        trackValues.put(DatabaseHelper.KEY_TRACK_END_ELAPSED, track.currentTimeElapsed)
        trackValues.put(DatabaseHelper.KEY_TRACK_DURATION_MOVING, track.movingTime)
        trackValues.put(DatabaseHelper.KEY_TRACK_DISTANCE, track.runningDistance)
        trackValues.put(DatabaseHelper.KEY_TRACK_DRIFT, track.getDrift())
        trackValues.put(DatabaseHelper.KEY_TRACK_ELEVATION_GAINED, track.elevationGained)
        trackValues.put(DatabaseHelper.KEY_TRACK_MAX_SPEED, track.maxSpeed)
        trackValues.put(DatabaseHelper.KEY_TRACK_MIN_SPEED, track.minSpeed)

        databaseHelper.writableDatabase.beginTransaction()
        val id = databaseHelper.writableDatabase.insert(DatabaseHelper.TABLE_TRACKS, null, trackValues)
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
        return id
    }

    fun updateTrackName(track: TrackSummary) {
        val contentValues = ContentValues()
        contentValues.put(DatabaseHelper.KEY_TRACK_NAME, track.name)
        databaseHelper.writableDatabase.beginTransaction()
        databaseHelper.writableDatabase.update(
            DatabaseHelper.TABLE_TRACKS,
            contentValues,
            "${DatabaseHelper.KEY_ID}=${track.trackId}",
            null
        )
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
    }

    fun readMaxSpeed(trackType: TrackType): Double? {
        val selectQuery = ("SELECT MAX(" + DatabaseHelper.KEY_TRACK_MAX_SPEED
                + ") FROM " + DatabaseHelper.TABLE_TRACKS
                + " WHERE " + DatabaseHelper.KEY_TRACK_TYPE + " = " + trackType.value)
        val cursor = databaseHelper.readableDatabase.rawQuery(selectQuery, null)
        var track: Double? = null

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                track = cursor.getDouble(0)
            }
            cursor.close()
        }
        return track
    }

    fun readTrackSummary(id: Long): TrackSummary? {

        val selectQuery = ("SELECT * FROM " + DatabaseHelper.TABLE_TRACKS
                + " WHERE " + DatabaseHelper.KEY_ID + " = " + id + " LIMIT 1 ")

        val cursor = databaseHelper.readableDatabase.rawQuery(selectQuery, null)
        var track: TrackSummary? = null

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                track = TrackSummary(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getInt(2),
                    cursor.getLong(3),
                    cursor.getLong(4),
                    cursor.getLong(5),
                    cursor.getLong(6),
                    cursor.getLong(7),
                    cursor.getDouble(8),
                    cursor.getDouble(9),
                    cursor.getDouble(10),
                    cursor.getDouble(11),
                    cursor.getDouble(12)
                )
            }
            cursor.close()
        }
        return track
    }

    fun readTrackSummaries(startId: Long, endId: Long): List<TrackSummary> {
        val trackList = mutableListOf<TrackSummary>()

        val selectQuery = ("SELECT  * FROM " + DatabaseHelper.TABLE_TRACKS
                + " WHERE " + DatabaseHelper.KEY_ID + " BETWEEN " + startId + " AND " + endId
                + " ORDER BY " + DatabaseHelper.KEY_ID + " DESC")

        val cursor = databaseHelper.readableDatabase.rawQuery(selectQuery, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val trackSummary = TrackSummary(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        cursor.getLong(3),
                        cursor.getLong(4),
                        cursor.getLong(5),
                        cursor.getLong(6),
                        cursor.getLong(7),
                        cursor.getDouble(8),
                        cursor.getDouble(9),
                        cursor.getDouble(10),
                        cursor.getDouble(11),
                        cursor.getDouble(12)
                    )
                    trackList.add(trackSummary)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return trackList
    }

    fun readTrackSummariesDuringPeriod(startTime: Long, endTime: Long): List<TrackSummary> {
        val trackList = mutableListOf<TrackSummary>()

        val selectQuery = ("SELECT  * FROM " + DatabaseHelper.TABLE_TRACKS
                + " WHERE " + DatabaseHelper.KEY_TRACK_START_STAMP + " BETWEEN " + startTime.toString() + " AND " + endTime.toString()
                + " ORDER BY " + DatabaseHelper.KEY_ID + " DESC")

        val cursor = databaseHelper.readableDatabase.rawQuery(selectQuery, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val trackSummary = TrackSummary(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        cursor.getLong(3),
                        cursor.getLong(4),
                        cursor.getLong(5),
                        cursor.getLong(6),
                        cursor.getLong(7),
                        cursor.getDouble(8),
                        cursor.getDouble(9),
                        cursor.getDouble(10),
                        cursor.getDouble(11),
                        cursor.getDouble(12)
                    )
                    trackList.add(trackSummary)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
        return trackList
    }

    fun deleteTrack(trackId: Long) {
        databaseHelper.writableDatabase.beginTransaction()
        databaseHelper.writableDatabase.delete(
            DatabaseHelper.TABLE_TRACKS,
            "${DatabaseHelper.KEY_ID} = ?",
            arrayOf(trackId.toString())
        )
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
    }

    override fun close() {
        databaseHelper.close()
    }
}