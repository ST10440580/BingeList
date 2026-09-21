package com.example.bingelist.ui.watchlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.data.model.toCardUiModel
import com.example.bingelist.data.repository.MovieRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val auth = FirebaseAuth.getInstance()

    private val _uiState =
        MutableStateFlow<WatchlistUiState>(WatchlistUiState.Loading)

    val uiState: StateFlow<WatchlistUiState> =
        _uiState.asStateFlow()

    fun loadWatchlist() {

        viewModelScope.launch(Dispatchers.IO) {

            _uiState.value = WatchlistUiState.Loading

            try {

                // Get movie IDs saved in the SQLite watchlist table
                val ids = database.getWatchlistIds()

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
                val userId = auth.currentUser?.uid

                val favoriteIds =
                    if (userId != null) {
                        database.getFavoriteIds(userId).toSet()
                    } else {
                        emptySet()
                    }

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

        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {

            val isFavorite =
                database.isFavorite(userId, imdbId)

            if (isFavorite) {
                database.removeFromFavorites(userId, imdbId)
            } else {
                database.addToFavorites(userId, imdbId)
            }

            loadWatchlist()
        }
    }

    fun removeFromWatchlist(imdbId: String) {

        viewModelScope.launch(Dispatchers.IO) {

            database.removeFromWatchlist(imdbId)

            loadWatchlist()
        }
    }
}