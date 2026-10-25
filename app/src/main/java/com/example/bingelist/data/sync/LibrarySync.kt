package com.example.bingelist.data.sync

import android.util.Log
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.model.MovieDetail
import com.example.bingelist.data.remote.CloudLibrarySync
import com.example.bingelist.data.remote.LibraryList
import com.example.bingelist.data.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keeps SQLite (local, works offline) and Firestore (cloud, survives reinstall) in step.
 * ViewModels still use BingeListDatabaseHelper directly; they only call
 * push() after a local change and hydrateOnce() when a screen opens.
 */
class LibrarySync(
    private val db: BingeListDatabaseHelper,
    private val cloud: CloudLibrarySync = CloudLibrarySync(),
    private val movies: MovieRepository = MovieRepository()
) {

    /** Call after a local add/remove. Never throws; if offline it just logs. */
    suspend fun push(uid: String, list: LibraryList, imdbId: String, added: Boolean) =
        withContext(Dispatchers.IO) {
            try {
                if (added) {
                    val movie = loadMovie(imdbId) ?: return@withContext
                    cloud.add(uid, list, movie)
                } else {
                    cloud.remove(uid, list, imdbId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Cloud push failed for $imdbId (${list.collection})", e)
            }
        }

    /**
     * Two-way sync, run once per signed-in user per app launch (retried later if it fails,
     * e.g. when offline). Safe to call from several screens at the same time.
     */
    suspend fun hydrateOnce(uid: String) = mutex.withLock {
        if (hydratedUid == uid) return@withLock
        val ok = withContext(Dispatchers.IO) {
            try {
                db.adoptLegacyWatchlist(uid)
                LibraryList.values().forEach { syncList(uid, it) }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Cloud hydrate failed; will retry next time", e)
                false
            }
        }
        if (ok) hydratedUid = uid
    }

    private suspend fun syncList(uid: String, list: LibraryList) {
        val cloudEntries = cloud.pull(uid, list) // throws if offline or denied by the rules
        val cloudIds = cloudEntries.map { it.imdbId }.toSet()
        val localIds = localIds(uid, list)

        // 1. cloud -> local (restores everything after a reinstall)
        for (entry in cloudEntries) {
            val movie = db.getCachedMovie(entry.imdbId)
                ?: entry.movie?.also { db.cacheMovie(it) }
                ?: movies.getMovieDetails(entry.imdbId).getOrNull()?.also { db.cacheMovie(it) }

            if (entry.imdbId !in localIds) addLocal(uid, list, entry.imdbId)

            // old documents only had the id: upgrade them so the next reinstall needs no OMDb call
            if (entry.movie == null && movie != null) cloud.add(uid, list, movie)
        }

        // 2. local -> cloud (items saved before sync worked, or while offline)
        for (id in localIds - cloudIds) {
            loadMovie(id)?.let { cloud.add(uid, list, it) }
        }
    }

    private suspend fun loadMovie(imdbId: String): MovieDetail? =
        db.getCachedMovie(imdbId)
            ?: movies.getMovieDetails(imdbId).getOrNull()?.also { db.cacheMovie(it) }

    private fun localIds(uid: String, list: LibraryList): Set<String> = when (list) {
        LibraryList.FAVORITES -> db.getFavoriteIds(uid)
        LibraryList.WATCHLIST -> db.getWatchlistIds(uid)
    }.toSet()

    private fun addLocal(uid: String, list: LibraryList, imdbId: String) {
        when (list) {
            LibraryList.FAVORITES -> db.addToFavorites(uid, imdbId)
            LibraryList.WATCHLIST -> db.addToWatchlist(uid, imdbId)
        }
    }

    companion object {
        private const val TAG = "LibrarySync"
        private val mutex = Mutex()
        @Volatile private var hydratedUid: String? = null
    }
}