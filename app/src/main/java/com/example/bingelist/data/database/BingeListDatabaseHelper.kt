package com.example.bingelist.data.database
// BingeListDatabaseHelper.kt
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// SQLite to be used for watchlist and for offline caching
class BingeListDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {

        val createWatchlistTable = """
            CREATE TABLE $TABLE_WATCHLIST (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_IMDB_ID TEXT UNIQUE NOT NULL
            )
        """.trimIndent()

        db.execSQL(createWatchlistTable)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WATCHLIST")
        onCreate(db)
    }

    fun addToWatchlist(imdbId: String): Boolean {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COLUMN_IMDB_ID, imdbId)
        }

        val result = db.insert(
            TABLE_WATCHLIST,
            null,
            values
        )

        db.close()

        return result != -1L
    }

    fun removeFromWatchlist(imdbId: String): Boolean {
        val db = writableDatabase

        val result = db.delete(
            TABLE_WATCHLIST,
            "$COLUMN_IMDB_ID = ?",
            arrayOf(imdbId)
        )

        db.close()

        return result > 0
    }

    fun isInWatchlist(imdbId: String): Boolean {
        val db = readableDatabase

        val cursor = db.query(
            TABLE_WATCHLIST,
            arrayOf(COLUMN_IMDB_ID),
            "$COLUMN_IMDB_ID = ?",
            arrayOf(imdbId),
            null,
            null,
            null
        )

        val exists = cursor.moveToFirst()

        cursor.close()
        db.close()

        return exists
    }

    fun getWatchlistIds(): List<String> {
        val ids = mutableListOf<String>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_WATCHLIST,
            arrayOf(COLUMN_IMDB_ID),
            null,
            null,
            null,
            null,
            "$COLUMN_ID ASC"
        )

        while (cursor.moveToNext()) {
            ids.add(
                cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_IMDB_ID)
                )
            )
        }

        cursor.close()
        db.close()

        return ids
    }

    companion object {
        private const val DATABASE_NAME = "bingelist.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_WATCHLIST = "watchlist"
        private const val COLUMN_ID = "id"
        private const val COLUMN_IMDB_ID = "imdb_id"
    }
}