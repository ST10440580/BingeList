package com.example.bingelist.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.data.model.toCardUiModel
import com.example.bingelist.data.remote.LibraryList
import com.example.bingelist.data.sync.LibrarySync
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = BingeListDatabaseHelper(application)
    private val sync = LibrarySync(dbHelper)
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _favorites = MutableStateFlow<List<MovieCardUiModel>>(emptyList())
    val favorites: StateFlow<List<MovieCardUiModel>> = _favorites.asStateFlow()

    init {
        loadFavorites()   // instant, from local SQLite (works offline)
        syncFromCloud()   // then merge with Firestore and reload
    }

    fun loadFavorites() {
        val uid = currentUserId
        if (uid == null) {
            _favorites.value = emptyList()
            return
        }
        viewModelScope.launch {
            val movies = withContext(Dispatchers.IO) {
                dbHelper.getCachedMovies(dbHelper.getFavoriteIds(uid))
                    .map { it.toCardUiModel(isFavorite = true) }
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
            sync.push(uid, LibraryList.FAVORITES, imdbId, nowFavorite)
        }
    }

    private fun syncFromCloud() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            sync.hydrateOnce(uid)
            loadFavorites()
        }
    }
}