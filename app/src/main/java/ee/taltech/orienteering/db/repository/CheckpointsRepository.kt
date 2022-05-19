package ee.taltech.orienteering.db.repository

import android.content.ContentValues
import android.content.Context
import ee.taltech.orienteering.db.DatabaseHelper
import ee.taltech.orienteering.track.pracelable.loaction.Checkpoint

class CheckpointsRepository private constructor(context: Context): IRepository {

    companion object {
        fun open(context: Context): CheckpointsRepository {
            return CheckpointsRepository(context)
        }
    }

    private val databaseHelper: DatabaseHelper = DatabaseHelper.getInstance(context)

    fun saveCheckpointToTrack(checkpoints: List<Checkpoint>, trackId: Long) {
        databaseHelper.writableDatabase.beginTransaction()
        checkpoints.forEachIndexed { index, cp ->
            val cpValues = ContentValues()
            cpValues.put(DatabaseHelper.KEY_TRACK_ID, trackId)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_NUMBER, index)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_LATITUDE, cp.latitude)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_LONGITUDE, cp.longitude)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_ALTITUDE, cp.altitude)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_ACCURACY, cp.accuracy)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_ALTITUDE_ACCURACY, cp.altitudeAccuracy)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_TIMESTAMP, cp.timestamp)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_ELAPSED_TIMESTAMP, cp.elapsedTimestamp)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_DRIFT_FROM_LAST_CP, cp.driftFromLastCP)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_TIME_SINCE_LAST_CP, cp.timeSinceLastCP)
            cpValues.put(DatabaseHelper.KEY_CHECKPOINT_DIST_FROM_LAST_CP, cp.distanceFromLastCP)
            databaseHelper.writableDatabase.insert(DatabaseHelper.TABLE_CHECKPOINTS, null, cpValues)
        }
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
    }

    fun readTrackCheckpoints(trackId: Long): List<Checkpoint> {
        val trackList = mutableListOf<Checkpoint>()

        val selectQuery = ("SELECT  * FROM " + DatabaseHelper.TABLE_CHECKPOINTS
                + " WHERE " + DatabaseHelper.KEY_TRACK_ID + " = " + trackId.toString()
                + " ORDER BY " + DatabaseHelper.KEY_CHECKPOINT_NUMBER + " ASC")

        val cursor = databaseHelper.readableDatabase.rawQuery(selectQuery, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val trackLocation = Checkpoint(
                        cursor.getDouble(3),
                        cursor.getDouble(4),
                        cursor.getDouble(5),
                        cursor.getDouble(6),
                        cursor.getDouble(7),
                        cursor.getLong(8),
                        cursor.getLong(9),
                        cursor.getFloat(10),
                        cursor.getLong(11),
                        cursor.getDouble(12)
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