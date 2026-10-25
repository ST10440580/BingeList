package com.example.bingelist.ui.watchlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.data.model.toCardUiModel
import com.example.bingelist.data.remote.LibraryList
import com.example.bingelist.data.repository.MovieRepository
import com.example.bingelist.data.sync.LibrarySync
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WatchlistUiState {
    object Loading : WatchlistUiState()
    data class Success(val movies: List<MovieCardUiModel>) : WatchlistUiState()
    object Empty : WatchlistUiState()
    data class Error(val message: String) : WatchlistUiState()
}

class WatchlistViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = MovieRepository()
    private val database = BingeListDatabaseHelper(application)
    private val sync = LibrarySync(database)
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _uiState =
        MutableStateFlow<WatchlistUiState>(WatchlistUiState.Loading)

    val uiState: StateFlow<WatchlistUiState> =
        _uiState.asStateFlow()

    init {
        // Show the local list straight away (the Activity calls loadWatchlist), then
        // merge with Firestore in the background and reload.
        val uid = currentUserId
        if (uid != null) {
            viewModelScope.launch {
                sync.hydrateOnce(uid)
                loadWatchlist()
            }
        }
    }

    fun loadWatchlist() {

        viewModelScope.launch(Dispatchers.IO) {

            val userId = currentUserId
            if (userId == null) {
                _uiState.value = WatchlistUiState.Empty
                return@launch
            }

            _uiState.value = WatchlistUiState.Loading

            try {

                // Get movie IDs saved in this user's SQLite watchlist
                val ids = database.getWatchlistIds(userId)

                if (ids.isEmpty()) {
                    _uiState.value = WatchlistUiState.Empty
                    return@launch
                }

                // First try to load movie details from SQLite cache
                val cachedMovies = database.getCachedMovies(ids)

                val cachedIds = cachedMovies
                    .map { it.imdbId }
                    .toSet()

                // Find movies that are not stored locally yet
                val missingIds = ids.filterNot {
                    cachedIds.contains(it)
                }

                // Fetch missing movie details from OMDb
                val fetchedMovies =
                    if (missingIds.isNotEmpty()) {
                        repository.getMovieDetailsByIds(missingIds)
                    } else {
                        emptyList()
                    }

                // Save newly fetched movies into SQLite cache
                fetchedMovies.forEach {
                    database.cacheMovie(it)
                }

                // Combine cached and newly fetched movies
                val allMovies = (cachedMovies + fetchedMovies)
                    .associateBy { it.imdbId }
                    .let { movieMap ->
                        ids.mapNotNull { movieMap[it] }
                    }

                if (allMovies.isEmpty()) {
                    _uiState.value =
                        WatchlistUiState.Error(
                            "Could not load watchlist items."
                        )
                    return@launch
                }

                // Get the user's favourites from SQLite
                val favoriteIds = database.getFavoriteIds(userId).toSet()

                _uiState.value =
                    WatchlistUiState.Success(
                        allMovies.map { movie ->
                            movie.toCardUiModel(
                                isFavorite = favoriteIds.contains(movie.imdbId),
                                isInWatchlist = true
                            )
                        }
                    )

            } catch (e: Exception) {

                _uiState.value =
                    WatchlistUiState.Error(
                        e.message ?: "Could not load watchlist."
                    )
            }
        }
    }

    fun toggleFavorite(imdbId: String) {

        val userId = currentUserId ?: return

        viewModelScope.launch(Dispatchers.IO) {

            val wasFavorite =
                database.isFavorite(userId, imdbId)

            if (wasFavorite) {
                database.removeFromFavorites(userId, imdbId)
            } else {
                database.addToFavorites(userId, imdbId)
            }

            loadWatchlist()
            sync.push(userId, LibraryList.FAVORITES, imdbId, !wasFavorite)
        }
    }

    fun removeFromWatchlist(imdbId: String) {

        val userId = currentUserId ?: return

        viewModelScope.launch(Dispatchers.IO) {

            database.removeFromWatchlist(userId, imdbId)

            loadWatchlist()
            sync.push(userId, LibraryList.WATCHLIST, imdbId, false)
        }
    }
}