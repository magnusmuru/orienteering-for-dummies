package ee.taltech.orienteering.db.repository

import android.content.ContentValues
import android.content.Context
import ee.taltech.orienteering.db.DatabaseHelper
import ee.taltech.orienteering.db.domain.User
import ee.taltech.orienteering.track.TrackType

class UserRepository(context: Context): IRepository {

    companion object {
        fun open(context: Context): UserRepository {
            return UserRepository(context)
        }
    }

    private val databaseHelper: DatabaseHelper = DatabaseHelper.getInstance(context)

    fun saveUser(user: User): Long {
        val userValues = ContentValues()
        userValues.put(DatabaseHelper.KEY_USER_USERNAME, user.username)
        userValues.put(DatabaseHelper.KEY_USER_EMAIL, user.email)
        userValues.put(DatabaseHelper.KEY_USER_PASSWORD, user.password)
        userValues.put(DatabaseHelper.KEY_USER_FIRST_NAME, user.firstName)
        userValues.put(DatabaseHelper.KEY_USER_LAST_NAME, user.lastName)
        userValues.put(DatabaseHelper.KEY_USER_SPEED_MODE, user.speedMode)
        userValues.put(DatabaseHelper.KEY_USER_DEFAULT_ACTIVITY, user.defaultActivityType.value)
        userValues.put(DatabaseHelper.KEY_USER_AUTO_SYNC, user.autoSync)
        userValues.put(DatabaseHelper.KEY_USER_SYNC_INTERVAL, user.syncInterval)

        databaseHelper.writableDatabase.beginTransaction()
        val id = databaseHelper.writableDatabase.insert(DatabaseHelper.TABLE_USERS, null, userValues)
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
        return id
    }

    fun readUser(): User? {
        val selectQuery = ("SELECT  * FROM " + DatabaseHelper.TABLE_USERS
                // + " WHERE " + DatabaseHelper.KEY_ID + " = " + id.toString()
                + " LIMIT 1 ")

        val cursor = databaseHelper.readableDatabase.rawQuery(selectQuery, null)
        var user: User? = null

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                user = User(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getInt(6) == 1,
                    TrackType.fromInt(cursor.getInt(7))!!,
                    cursor.getInt(8) == 1,
                    cursor.getLong(9)
                )
            }
            cursor.close()
        }
        return user
    }

    fun updateUser(user: User) {
        val userValues = ContentValues()
        userValues.put(DatabaseHelper.KEY_USER_USERNAME, user.username)
        userValues.put(DatabaseHelper.KEY_USER_EMAIL, user.email)
        userValues.put(DatabaseHelper.KEY_USER_PASSWORD, user.password)
        userValues.put(DatabaseHelper.KEY_USER_FIRST_NAME, user.firstName)
        userValues.put(DatabaseHelper.KEY_USER_LAST_NAME, user.lastName)
        userValues.put(DatabaseHelper.KEY_USER_SPEED_MODE, user.speedMode)
        userValues.put(DatabaseHelper.KEY_USER_DEFAULT_ACTIVITY, user.defaultActivityType.value)
        userValues.put(DatabaseHelper.KEY_USER_AUTO_SYNC, user.autoSync)
        userValues.put(DatabaseHelper.KEY_USER_SYNC_INTERVAL, user.syncInterval)

        databaseHelper.writableDatabase.beginTransaction()
        databaseHelper.writableDatabase.update(DatabaseHelper.TABLE_USERS, userValues, "${DatabaseHelper.KEY_ID} = ?", arrayOf(user.userId.toString()))
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
    }

    fun deleteUsers() {
        databaseHelper.writableDatabase.beginTransaction()
        databaseHelper.writableDatabase.delete(DatabaseHelper.TABLE_USERS, null, null)
        databaseHelper.writableDatabase.setTransactionSuccessful()
        databaseHelper.writableDatabase.endTransaction()
    }

    override fun close() {
        databaseHelper.close()
    }
}