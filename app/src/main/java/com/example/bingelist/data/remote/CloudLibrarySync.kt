package com.example.bingelist.data.remote

import com.example.bingelist.data.model.MovieDetail
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

enum class LibraryList(val collection: String) {
    FAVORITES("favorites"),
    WATCHLIST("watchlist")
}

/** [movie] is null for old documents that only stored the IMDb id. */
data class CloudEntry(val imdbId: String, val movie: MovieDetail?)

/**
 * Firestore side of the library. One document per movie:
 * users/{uid}/favorites/{imdbId} and users/{uid}/watchlist/{imdbId}.
 * Each document stores the movie's card fields so a reinstall can rebuild the
 * screens without calling OMDb again.
 */
class CloudLibrarySync(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun ref(uid: String, list: LibraryList) =
        firestore.collection("users").document(uid).collection(list.collection)

    suspend fun add(uid: String, list: LibraryList, movie: MovieDetail) {
        ref(uid, list).document(movie.imdbId).set(
            mapOf(
                "imdbId" to movie.imdbId,
                "title" to movie.title,
                "year" to movie.year,
                "runtime" to (movie.runtime ?: ""),
                "genre" to (movie.genre ?: ""),
                "rating" to (movie.imdbRating ?: ""),
                "plot" to (movie.plot ?: ""),
                "director" to (movie.director ?: ""),
                "actors" to (movie.actors ?: ""),
                "poster" to (movie.posterUrlOrNull ?: ""),
                "addedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun remove(uid: String, list: LibraryList, imdbId: String) {
        ref(uid, list).document(imdbId).delete().await()
    }

    suspend fun pull(uid: String, list: LibraryList): List<CloudEntry> {
        val snapshot = ref(uid, list).get().await()
        return snapshot.documents.map { doc ->
            val title = doc.getString("title")
            val movie = if (title.isNullOrBlank()) null else MovieDetail(
                title = title,
                year = doc.getString("year") ?: "",
                rated = null,
                released = null,
                runtime = doc.getString("runtime"),
                genre = doc.getString("genre"),
                director = doc.getString("director"),
                actors = doc.getString("actors"),
                plot = doc.getString("plot"),
                language = null,
                country = null,
                poster = doc.getString("poster"),
                imdbRating = doc.getString("rating"),
                imdbVotes = null,
                imdbId = doc.id,
                response = "True"
            )
            CloudEntry(doc.id, movie)
        }
    }
}