package com.example.bingelist.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


class FavoritesCloudSync(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun favoritesRef(userId: String) =
        firestore.collection("users").document(userId).collection("favorites")

    suspend fun pushFavorite(userId: String, imdbId: String, isFavorite: Boolean) {
        val doc = favoritesRef(userId).document(imdbId)
        if (isFavorite) {
            doc.set(mapOf("imdbId" to imdbId)).await()
        } else {
            doc.delete().await()
        }
    }

    suspend fun pullFavoriteIds(userId: String): List<String> {
        val snapshot = favoritesRef(userId).get().await()
        return snapshot.documents.map { it.id }
    }
}