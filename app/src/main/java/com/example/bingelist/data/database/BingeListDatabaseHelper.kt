package com.example.bingelist.data.database
// BingeListDatabaseHelper.kt
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.bingelist.data.model.MovieDetail


class BingeListDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {

        val createWatchlistTable = """
            CREATE TABLE $TABLE_WATCHLIST (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_IMDB_ID TEXT UNIQUE NOT NULL
            )
        """.trimIndent()

        val createFavoritesTable = """
            CREATE TABLE $TABLE_FAVORITES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_IMDB_ID TEXT NOT NULL,
                UNIQUE($COLUMN_USER_ID, $COLUMN_IMDB_ID)
            )
        """.trimIndent()

        val createMovieCacheTable = """
            CREATE TABLE $TABLE_MOVIE_CACHE (
                $COLUMN_CACHE_IMDB_ID TEXT PRIMARY KEY,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_POSTER_URL TEXT,
                $COLUMN_YEAR TEXT,
                $COLUMN_RUNTIME TEXT,
                $COLUMN_GENRE TEXT,
                $COLUMN_RATING TEXT,
                $COLUMN_PLOT TEXT,
                $COLUMN_DIRECTOR TEXT,
                $COLUMN_ACTORS TEXT,
                $COLUMN_CACHED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        db.execSQL(createWatchlistTable)
        db.execSQL(createFavoritesTable)
        db.execSQL(createMovieCacheTable)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WATCHLIST")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MOVIE_CACHE")
        onCreate(db)
    }

    //Watchlist

    fun addToWatchlist(imdbId: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put(COLUMN_IMDB_ID, imdbId) }
        val result = db.insert(TABLE_WATCHLIST, null, values)
        return result != -1L
    }

    fun removeFromWatchlist(imdbId: String): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_WATCHLIST, "$COLUMN_IMDB_ID = ?", arrayOf(imdbId))
        return result > 0
    }

    fun isInWatchlist(imdbId: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WATCHLIST, arrayOf(COLUMN_IMDB_ID), "$COLUMN_IMDB_ID = ?",
            arrayOf(imdbId), null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun getWatchlistIds(): List<String> {
        val ids = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WATCHLIST, arrayOf(COLUMN_IMDB_ID), null, null, null, null, "$COLUMN_ID ASC"
        )
        while (cursor.moveToNext()) {
            ids.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMDB_ID)))
        }
        cursor.close()
        return ids
    }

    //Favorites

    fun addToFavorites(userId: String, imdbId: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_IMDB_ID, imdbId)
        }
        val result = db.insert(TABLE_FAVORITES, null, values)
        return result != -1L
    }

    fun removeFromFavorites(userId: String, imdbId: String): Boolean {
        val db = writableDatabase
        val result = db.delete(
            TABLE_FAVORITES,
            "$COLUMN_USER_ID = ? AND $COLUMN_IMDB_ID = ?",
            arrayOf(userId, imdbId)
        )
        return result > 0
    }

    fun isFavorite(userId: String, imdbId: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_FAVORITES, arrayOf(COLUMN_IMDB_ID),
            "$COLUMN_USER_ID = ? AND $COLUMN_IMDB_ID = ?",
            arrayOf(userId, imdbId), null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun getFavoriteIds(userId: String): List<String> {
        val ids = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_FAVORITES, arrayOf(COLUMN_IMDB_ID),
            "$COLUMN_USER_ID = ?", arrayOf(userId),
            null, null, "$COLUMN_ID ASC"
        )
        while (cursor.moveToNext()) {
            ids.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMDB_ID)))
        }
        cursor.close()
        return ids
    }

    //Movie detail cache

    fun cacheMovie(movie: MovieDetail) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CACHE_IMDB_ID, movie.imdbId)
            put(COLUMN_TITLE, movie.title)
            put(COLUMN_POSTER_URL, movie.posterUrlOrNull)
            put(COLUMN_YEAR, movie.year)
            put(COLUMN_RUNTIME, movie.runtime?.takeIf { it != "N/A" } ?: "")
            put(COLUMN_GENRE, movie.genre?.split(",")?.firstOrNull()?.trim() ?: "")
            put(COLUMN_RATING, movie.imdbRating?.takeIf { it != "N/A" } ?: "—")
            put(COLUMN_PLOT, movie.plot?.takeIf { it != "N/A" } ?: "No description available.")
            put(COLUMN_DIRECTOR, movie.director ?: "")
            put(COLUMN_ACTORS, movie.actors ?: "")
            put(COLUMN_CACHED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_MOVIE_CACHE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCachedMovie(imdbId: String): MovieDetail? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MOVIE_CACHE, null, "$COLUMN_CACHE_IMDB_ID = ?", arrayOf(imdbId), null, null, null
        )
        val movie = if (cursor.moveToFirst()) cursor.toMovieDetail() else null
        cursor.close()
        return movie
    }

    fun getCachedMovies(imdbIds: List<String>): List<MovieDetail> {
        if (imdbIds.isEmpty()) return emptyList()
        val placeholders = imdbIds.joinToString(",") { "?" }
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MOVIE_CACHE, null, "$COLUMN_CACHE_IMDB_ID IN ($placeholders)",
            imdbIds.toTypedArray(), null, null, null
        )
        val results = mutableListOf<MovieDetail>()
        while (cursor.moveToNext()) results.add(cursor.toMovieDetail())
        cursor.close()
        val byId = results.associateBy { it.imdbId }
        return imdbIds.mapNotNull { byId[it] } // preserves caller's ordering
    }

    private fun android.database.Cursor.toMovieDetail() = MovieDetail(
        title = getString(getColumnIndexOrThrow(COLUMN_TITLE)),
        year = getString(getColumnIndexOrThrow(COLUMN_YEAR)) ?: "",
        rated = null,
        released = null,
        runtime = getString(getColumnIndexOrThrow(COLUMN_RUNTIME)),
        genre = getString(getColumnIndexOrThrow(COLUMN_GENRE)),
        director = getString(getColumnIndexOrThrow(COLUMN_DIRECTOR)),
        actors = getString(getColumnIndexOrThrow(COLUMN_ACTORS)),
        plot = getString(getColumnIndexOrThrow(COLUMN_PLOT)),
        language = null,
        country = null,
        poster = getString(getColumnIndexOrThrow(COLUMN_POSTER_URL)),
        imdbRating = getString(getColumnIndexOrThrow(COLUMN_RATING)),
        imdbVotes = null,
        imdbId = getString(getColumnIndexOrThrow(COLUMN_CACHE_IMDB_ID)),
        response = "True"
    )


    companion object {
        private const val DATABASE_NAME = "bingelist.db"
        private const val DATABASE_VERSION = 3

        private const val TABLE_WATCHLIST = "watchlist"
        private const val TABLE_FAVORITES = "favorites"
        private const val TABLE_MOVIE_CACHE = "movie_cache"

        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_IMDB_ID = "imdb_id"

        private const val COLUMN_CACHE_IMDB_ID = "imdb_id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_POSTER_URL = "poster_url"
        private const val COLUMN_YEAR = "year"
        private const val COLUMN_RUNTIME = "runtime"
        private const val COLUMN_GENRE = "genre"
        private const val COLUMN_RATING = "rating"
        private const val COLUMN_PLOT = "plot"
        private const val COLUMN_DIRECTOR = "director"
        private const val COLUMN_ACTORS = "actors"
        private const val COLUMN_CACHED_AT = "cached_at"
    }
}