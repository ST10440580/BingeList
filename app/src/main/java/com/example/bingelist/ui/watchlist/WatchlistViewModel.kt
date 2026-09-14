package com.example.bingelist.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.model.InMemoryStore
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.data.model.toCardUiModel
import com.example.bingelist.data.repository.MovieRepository
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
    private val repository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WatchlistUiState>(WatchlistUiState.Loading)
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    fun loadWatchlist() {
        val ids = InMemoryStore.watchlist.toList()
        if (ids.isEmpty()) {
            _uiState.value = WatchlistUiState.Empty
            return
        }

        _uiState.value = WatchlistUiState.Loading
        viewModelScope.launch {
            // Check cache first for efficiency
            val cachedDetails = ids.mapNotNull { InMemoryStore.movieCache[it] }
            val missingIds = ids.filter { id -> !InMemoryStore.movieCache.containsKey(id) }

            val allDetails = if (missingIds.isNotEmpty()) {
                val fetchedDetails = repository.getMovieDetailsByIds(missingIds)
                fetchedDetails.forEach { InMemoryStore.movieCache[it.imdbId] = it }
                (cachedDetails + fetchedDetails).sortedBy { ids.indexOf(it.imdbId) }
            } else {
                cachedDetails
            }

            if (allDetails.isEmpty() && ids.isNotEmpty()) {
                 _uiState.value = WatchlistUiState.Error("Could not load watchlist items.")
            } else if (allDetails.isEmpty()) {
                _uiState.value = WatchlistUiState.Empty
            } else {
                _uiState.value = WatchlistUiState.Success(
                    allDetails.map { it.toCardUiModel(
                        isFavorite = InMemoryStore.favorites.contains(it.imdbId),
                        isInWatchlist = true
                    )}
                )
            }
        }
    }

    fun toggleFavorite(imdbId: String) {
        if (!InMemoryStore.favorites.add(imdbId)) {
            InMemoryStore.favorites.remove(imdbId)
        }
        loadWatchlist() // Refresh to update favorite icon
    }

    fun removeFromWatchlist(imdbId: String) {
        InMemoryStore.watchlist.remove(imdbId)
        loadWatchlist()
    }
}
