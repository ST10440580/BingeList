package com.example.bingelist.ui.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.data.model.MovieDetail
import com.example.bingelist.data.model.toCardUiModel
import com.example.bingelist.data.remote.FavoritesCloudSync
import com.example.bingelist.data.repository.MovieRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DiscoverUiState {
    object Idle : DiscoverUiState()
    object Loading : DiscoverUiState()
    data class Success(val movies: List<MovieCardUiModel>) : DiscoverUiState()
    data class Empty(val message: String) : DiscoverUiState()
    data class Error(val message: String) : DiscoverUiState()
}

class DiscoverViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovieRepository()
    private val dbHelper = BingeListDatabaseHelper(application)
    private val cloudSync = FavoritesCloudSync()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Idle)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _newThisWeek = MutableStateFlow<List<MovieCardUiModel>>(emptyList())
    val newThisWeek: StateFlow<List<MovieCardUiModel>> = _newThisWeek.asStateFlow()

    private val _selectedGenre = MutableStateFlow(GENRES.first())
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private var lastQuery = ""
    private val knownDetails = mutableMapOf<String, MovieDetail>()
    private var currentSearchIds: List<String> = emptyList()
    private var newThisWeekIds: List<String> = emptyList()
    private var favoriteIds: Set<String> = emptySet()
    private var watchlistIds: Set<String> = emptySet()

    init {
        refreshFavorites()
        refreshWatchlist()
        loadNewThisWeek()
    }

    /** Re-reads user favorites from SQLite */
    fun refreshFavorites() {
        val uid = currentUserId
        if (uid == null) {
            favoriteIds = emptySet()
            render()
            applyFlagsToNewThisWeek()
            return
        }
        viewModelScope.launch {
            favoriteIds = withContext(Dispatchers.IO) {
                // If your helper supports favorites by UID:
                runCatching { dbHelper.getFavoriteIds(uid).toSet() }.getOrDefault(emptySet())
            }
            render()
            applyFlagsToNewThisWeek()
        }
    }

    /** Re-reads watchlist from SQLite */
    fun refreshWatchlist() {
        viewModelScope.launch {
            watchlistIds = withContext(Dispatchers.IO) {
                dbHelper.getWatchlistIds().toSet()
            }
            render()
            applyFlagsToNewThisWeek()
        }
    }

    private fun loadNewThisWeek() {
        viewModelScope.launch {
            val ids = repository.getWeeklyNewReleaseIds(limit = 4)
            newThisWeekIds = ids
            val details = repository.getMovieDetailsByIds(ids)
            details.forEach { knownDetails[it.imdbId] = it }
            applyFlagsToNewThisWeek()
        }
    }

    private fun applyFlagsToNewThisWeek() {
        val details = newThisWeekIds.mapNotNull { knownDetails[it] }
        if (details.isNotEmpty()) {
            _newThisWeek.value = details.map {
                it.toCardUiModel(
                    isFavorite = favoriteIds.contains(it.imdbId),
                    isInWatchlist = watchlistIds.contains(it.imdbId)
                )
            }
        }
    }

    fun searchMovies(query: String) {
        val trimmed = query.trim()
        lastQuery = trimmed

        if (trimmed.isEmpty()) {
            currentSearchIds = emptyList()
            _uiState.value = DiscoverUiState.Idle
            return
        }

        _uiState.value = DiscoverUiState.Loading
        viewModelScope.launch {
            repository.searchMovies(trimmed)
                .onSuccess { summaries ->
                    val details = repository.getMovieDetailsBatch(summaries)
                    details.forEach { knownDetails[it.imdbId] = it }
                    currentSearchIds = details.map { it.imdbId }
                    render()
                }
                .onFailure { error ->
                    currentSearchIds = emptyList()
                    _uiState.value = DiscoverUiState.Error(error.message ?: "Something went wrong. Please try again.")
                }
        }
    }

    fun selectGenre(genre: String) {
        _selectedGenre.value = genre
        if (lastQuery.isNotEmpty()) render()
    }

    fun toggleFavorite(imdbId: String) {
        val detail = knownDetails[imdbId] ?: return
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val nowFavorite = withContext(Dispatchers.IO) {
                runCatching { dbHelper.cacheMovie(detail) }
                val wasFavorite = runCatching { dbHelper.isFavorite(uid, imdbId) }.getOrDefault(false)
                if (wasFavorite) {
                    dbHelper.removeFromFavorites(uid, imdbId)
                } else {
                    dbHelper.addToFavorites(uid, imdbId)
                }
                !wasFavorite
            }
            refreshFavorites()
            runCatching { cloudSync.pushFavorite(uid, imdbId, nowFavorite) }
        }
    }

    fun toggleWatchlist(imdbId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isInWatchlist = dbHelper.isInWatchlist(imdbId)
            if (isInWatchlist) {
                dbHelper.removeFromWatchlist(imdbId)
            } else {
                dbHelper.addToWatchlist(imdbId)
            }
            withContext(Dispatchers.Main) {
                refreshWatchlist()
            }
        }
    }

    private fun render() {
        val genre = _selectedGenre.value
        val details = currentSearchIds.mapNotNull { knownDetails[it] }
        val filtered = if (genre == "All") details else details.filter {
            it.genre?.contains(genre, ignoreCase = true) == true
        }

        _uiState.value = when {
            lastQuery.isEmpty() -> DiscoverUiState.Idle
            filtered.isEmpty() -> DiscoverUiState.Empty(
                if (genre == "All") "No movies found for \"$lastQuery\"." else "No $genre movies found for \"$lastQuery\"."
            )
            else -> DiscoverUiState.Success(
                filtered.map {
                    it.toCardUiModel(
                        isFavorite = favoriteIds.contains(it.imdbId),
                        isInWatchlist = watchlistIds.contains(it.imdbId)
                    )
                }
            )
        }
    }

    companion object {
        val GENRES = listOf("All", "Action", "Animation", "Comedy", "Drama")
    }
}