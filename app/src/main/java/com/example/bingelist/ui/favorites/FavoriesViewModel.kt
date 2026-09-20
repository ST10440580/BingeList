package com.example.bingelist.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.data.model.toCardUiModel
import com.example.bingelist.data.remote.FavoritesCloudSync
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = BingeListDatabaseHelper(application)
    private val cloudSync = FavoritesCloudSync()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _favorites = MutableStateFlow<List<MovieCardUiModel>>(emptyList())
    val favorites: StateFlow<List<MovieCardUiModel>> = _favorites.asStateFlow()

    init {
        loadFavorites()   // instant, from local SQLite works offline
        hydrateFromCloud() // background refresh from Firestore, if online
    }


    fun loadFavorites() {
        val uid = currentUserId
        if (uid == null) {
            _favorites.value = emptyList()
            return
        }
        viewModelScope.launch {
            val movies = withContext(Dispatchers.IO) {
                dbHelper.getCachedMovies(dbHelper.getFavoriteIds(uid)).map { it.toCardUiModel(isFavorite = true) }
            }
            _favorites.value = movies
        }
    }

    fun toggleFavorite(imdbId: String) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val nowFavorite = withContext(Dispatchers.IO) {
                val wasFavorite = dbHelper.isFavorite(uid, imdbId)
                if (wasFavorite) dbHelper.removeFromFavorites(uid, imdbId)
                else dbHelper.addToFavorites(uid, imdbId)
                !wasFavorite
            }
            loadFavorites()
            runCatching { cloudSync.pushFavorite(uid, imdbId, nowFavorite) }
            // If this push fails (offline), local SQLite state is unaffected — it's
            // just out of sync with the cloud until the next toggle or app open.
        }
    }


    private fun hydrateFromCloud() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val cloudIds = runCatching { cloudSync.pullFavoriteIds(uid) }.getOrDefault(emptyList())
                cloudIds.forEach { imdbId ->
                    if (!dbHelper.isFavorite(uid, imdbId)) dbHelper.addToFavorites(uid, imdbId)
                }
            }
            loadFavorites()
        }
    }
}