package ee.taltech.orienteering.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import java.util.concurrent.atomic.AtomicInteger


class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "Orienteering"
        private const val DATABASE_VERSION = 1

        // ------------------------------------ Table names -----------------------------------
        const val TABLE_TRACKS = "tracks"
        const val TABLE_LOCATIONS = "locations"
        const val TABLE_CHECKPOINTS = "checkpoints"
        const val TABLE_WAY_POINTS = "way_points"
        const val TABLE_USERS = "users"
        const val TABLE_OFFLINE_SESSIONS = "offline_sessions"

        // ---------------------------------- Common Columns names ---------------------------
        const val KEY_ID = BaseColumns._ID
        const val KEY_TRACK_ID = "track_id"

        // ------------------------------- Tracks table column names ----------------------------
        const val KEY_TRACK_NAME = "name"
        const val KEY_TRACK_TYPE = "type"
        const val KEY_TRACK_START_STAMP = "start_stamp"
        const val KEY_TRACK_START_ELAPSED = "start_time_elapsed"
        const val KEY_TRACK_END_STAMP = "end_stamp"
        const val KEY_TRACK_END_ELAPSED = "end_time_elapsed"

        const val KEY_TRACK_DURATION_MOVING = "duration_moving"
        const val KEY_TRACK_DISTANCE = "distance"
        const val KEY_TRACK_DRIFT = "drift"
        const val KEY_TRACK_ELEVATION_GAINED = "elevation_gained"
        const val KEY_TRACK_MAX_SPEED = "max_speed"
        const val KEY_TRACK_MIN_SPEED = "min_speed"

        // ------------------------------ Locations table column names ---------------------------
        const val KEY_LOCATION_NUMBER = "nr"
        const val KEY_LOCATION_LATITUDE = "latitude"
        const val KEY_LOCATION_LONGITUDE = "longitude"
        const val KEY_LOCATION_ALTITUDE = "altitude"
        const val KEY_LOCATION_ACCURACY = "accuracy"
        const val KEY_LOCATION_ALTITUDE_ACCURACY = "altitude_accuracy"
        const val KEY_LOCATION_TIME = "time"
        const val KEY_LOCATION_TIME_ELAPSED = "time_elapsed"

        // ------------------------------- Checkpoints table column names ---------------------------
        const val KEY_CHECKPOINT_NUMBER = "nr"
        const val KEY_CHECKPOINT_LATITUDE = "latitude"
        const val KEY_CHECKPOINT_LONGITUDE = "longitude"
        const val KEY_CHECKPOINT_ALTITUDE = "altitude"
        const val KEY_CHECKPOINT_ACCURACY = "accuracy"
        const val KEY_CHECKPOINT_ALTITUDE_ACCURACY = "altitude_accuracy"
        const val KEY_CHECKPOINT_TIMESTAMP = "timestamp"
        const val KEY_CHECKPOINT_ELAPSED_TIMESTAMP = "elapsed_timestamp"
        const val KEY_CHECKPOINT_DRIFT_FROM_LAST_CP = "drift_from_last_cp"
        const val KEY_CHECKPOINT_TIME_SINCE_LAST_CP = "time_since_last_cp"
        const val KEY_CHECKPOINT_DIST_FROM_LAST_CP = "distance_from_last_cp"

        // -------------------------------- Way points table column names ----------------------------
        const val KEY_WAY_POINT_NUMBER = "nr"
        const val KEY_WAY_POINT_LATITUDE = "latitude"
        const val KEY_WAY_POINT_LONGITUDE = "longitude"
        const val KEY_WAY_POINT_ADDED_TIMESTAMP = "added_timestamp"
        const val KEY_WAY_POINT_REMOVED_TIMESTAMP = "removed_timestamp"

        // -------------------------------- Way points table column names ----------------------------
        const val KEY_USER_USERNAME = "username"
        const val KEY_USER_EMAIL = "email"
        const val KEY_USER_PASSWORD = "password"
        const val KEY_USER_FIRST_NAME = "first_name"
        const val KEY_USER_LAST_NAME = "last_name"
        const val KEY_USER_SPEED_MODE = "speed_mode"
        const val KEY_USER_DEFAULT_ACTIVITY = "default_activity"
        const val KEY_USER_AUTO_SYNC = "auto_sync"
        const val KEY_USER_SYNC_INTERVAL = "sync_interval"

        private var instance: DatabaseHelper? = null
        private var instanceCount: AtomicInteger = AtomicInteger(0)

        @Synchronized
        fun getInstance(context: Context): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(context)
            }
            instanceCount.incrementAndGet()
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTracksTable = ("CREATE TABLE " + TABLE_TRACKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TRACK_NAME + " TEXT NOT NULL,"
                + KEY_TRACK_TYPE + " INTEGER NOT NULL,"
                + KEY_TRACK_START_STAMP + " UNSIGNED BIGINT NOT NULL,"
                + KEY_TRACK_START_ELAPSED + " UNSIGNED BIGINT NOT NULL,"
                + KEY_TRACK_END_STAMP + " UNSIGNED BIGINT NOT NULL,"
                + KEY_TRACK_END_ELAPSED + " UNSIGNED BIGINT NOT NULL,"
                + KEY_TRACK_DURATION_MOVING + " UNSIGNED BIGINT NOT NULL,"
                + KEY_TRACK_DISTANCE + " REAL NOT NULL,"
                + KEY_TRACK_DRIFT + " REAL NOT NULL,"
                + KEY_TRACK_ELEVATION_GAINED + " REAL NOT NULL,"
                + KEY_TRACK_MAX_SPEED + " REAL NOT NULL,"
                + KEY_TRACK_MIN_SPEED + " REAL NOT NULL" + ")")

        db?.execSQL(createTracksTable)

        val createLocationsTable = ("CREATE TABLE " + TABLE_LOCATIONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TRACK_ID + " INTEGER NOT NULL,"
                + KEY_LOCATION_NUMBER + " INTEGER NOT NULL,"
                + KEY_LOCATION_LATITUDE + " REAL NOT NULL,"
                + KEY_LOCATION_LONGITUDE + " REAL NOT NULL,"
                + KEY_LOCATION_ALTITUDE + " REAL NOT NULL,"
                + KEY_LOCATION_ACCURACY + " REAL NOT NULL,"
                + KEY_LOCATION_ALTITUDE_ACCURACY + " REAL NOT NULL,"
                + KEY_LOCATION_TIME + " UNSIGNED BIGINT NOT NULL,"
                + KEY_LOCATION_TIME_ELAPSED + " UNSIGNED BIGINT NOT NULL,"
                + " FOREIGN KEY (" + KEY_TRACK_ID
                + ") REFERENCES " + TABLE_TRACKS + "(" + KEY_ID + ") ON UPDATE CASCADE ON DELETE CASCADE" + ")")

        db?.execSQL(createLocationsTable)

        val createCheckpointsTable = ("CREATE TABLE " + TABLE_CHECKPOINTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TRACK_ID + " INTEGER NOT NULL,"
                + KEY_CHECKPOINT_NUMBER + " INTEGER NOT NULL,"
                + KEY_CHECKPOINT_LATITUDE + " REAL NOT NULL,"
                + KEY_CHECKPOINT_LONGITUDE + " REAL NOT NULL,"
                + KEY_CHECKPOINT_ALTITUDE + " REAL NOT NULL,"
                + KEY_CHECKPOINT_ACCURACY + " REAL NOT NULL,"
                + KEY_CHECKPOINT_ALTITUDE_ACCURACY + " REAL NOT NULL,"
                + KEY_CHECKPOINT_TIMESTAMP + " UNSIGNED BIGINT NOT NULL,"
                + KEY_CHECKPOINT_ELAPSED_TIMESTAMP + " UNSIGNED BIGINT NOT NULL,"
                + KEY_CHECKPOINT_DRIFT_FROM_LAST_CP + " REAL NOT NULL,"
                + KEY_CHECKPOINT_TIME_SINCE_LAST_CP + " UNSIGNED BIGINT NOT NULL,"
                + KEY_CHECKPOINT_DIST_FROM_LAST_CP + " REAL NOT NULL,"
                + " FOREIGN KEY (" + KEY_TRACK_ID
                + ") REFERENCES " + TABLE_TRACKS + "(" + KEY_ID + ") ON UPDATE CASCADE ON DELETE CASCADE" + ")")

        db?.execSQL(createCheckpointsTable)

        val createWayPointsTable = ("CREATE TABLE " + TABLE_WAY_POINTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TRACK_ID + " INTEGER NOT NULL,"
                + KEY_WAY_POINT_NUMBER + " INTEGER NOT NULL,"
                + KEY_WAY_POINT_LATITUDE + " REAL NOT NULL,"
                + KEY_WAY_POINT_LONGITUDE + " REAL NOT NULL,"
                + KEY_WAY_POINT_ADDED_TIMESTAMP + " UNSIGNED BIGINT NOT NULL,"
                + KEY_WAY_POINT_REMOVED_TIMESTAMP + " UNSIGNED BIGINT NULL,"
                + " FOREIGN KEY (" + KEY_TRACK_ID
                + ") REFERENCES " + TABLE_TRACKS + "(" + KEY_ID + ") ON UPDATE CASCADE ON DELETE CASCADE" + ")")

        db?.execSQL(createWayPointsTable)

        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USER_USERNAME + " TEXT NOT NULL,"
                + KEY_USER_EMAIL + " TEXT NOT NULL,"
                + KEY_USER_PASSWORD + " TEXT NOT NULL,"
                + KEY_USER_FIRST_NAME + " TEXT NOT NULL,"
                + KEY_USER_LAST_NAME + " TEXT NOT NULL,"
                + KEY_USER_SPEED_MODE + " INTEGER NOT NULL,"
                + KEY_USER_DEFAULT_ACTIVITY + " INTEGER NULL,"
                + KEY_USER_AUTO_SYNC + " INTEGER NOT NULL,"
                + KEY_USER_SYNC_INTERVAL + " INTEGER NOT NULL" + ")")
        db?.execSQL(createUsersTable)

        val createOfflineSessionsTable = ("CREATE TABLE " + TABLE_OFFLINE_SESSIONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TRACK_ID + " INTEGER NOT NULL,"
                + " FOREIGN KEY (" + KEY_TRACK_ID
                + ") REFERENCES " + TABLE_TRACKS + "(" + KEY_ID + ") ON UPDATE CASCADE ON DELETE CASCADE" + ")")

        db?.execSQL(createOfflineSessionsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    override fun close() {
        instanceCount.decrementAndGet()
        if (instanceCount.get() <= 0) {
            super.close()
        }
    }
}