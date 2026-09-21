package com.example.bingelist.ui.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.data.model.MovieDetail
import com.example.bingelist.data.model.MovieSummary
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

    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _newThisWeek = MutableStateFlow<List<MovieCardUiModel>>(emptyList())
    val newThisWeek: StateFlow<List<MovieCardUiModel>> = _newThisWeek.asStateFlow()

    private val _selectedGenre = MutableStateFlow(GENRES.first())
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private var lastQuery = ""
    private val knownDetails = mutableMapOf<String, MovieDetail>()
    private var currentMovieIds: List<String> = emptyList()
    private var newThisWeekIds: List<String> = emptyList()
    private var favoriteIds: Set<String> = emptySet()
    private var watchlistIds: Set<String> = emptySet()

    // Representative franchises/titles that guarantee rich, genre-matched results
    private val genreKeywords = mapOf(
        "All" to listOf("Avengers", "Batman"),
        "Action" to listOf("John Wick", "Fast"),
        "Animation" to listOf("Toy Story", "Shrek"),
        "Comedy" to listOf("Hangover", "Superbad"),
        "Drama" to listOf("Godfather", "Shawshank")
    )

    init {
        refreshFavorites()
        refreshWatchlist()
        loadNewThisWeek()
        // Automatically load "All" category movies on launch
        loadGenreMovies(GENRES.first())
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

    /** Called when the user taps any genre button (Action, Drama, Comedy, etc.) */
    fun selectGenre(genre: String) {
        _selectedGenre.value = genre
        lastQuery = "" // Reset manual search so category browsing takes over
        loadGenreMovies(genre)
    }

    /** Loads movies for the selected category without requiring a manual search */
    private fun loadGenreMovies(genre: String) {
        _uiState.value = DiscoverUiState.Loading
        viewModelScope.launch {
            val keywords = genreKeywords[genre] ?: listOf("Avengers")
            val summaries = mutableListOf<MovieSummary>()

            for (kw in keywords) {
                repository.searchMovies(kw).onSuccess { list ->
                    summaries.addAll(list)
                }
            }

            if (summaries.isEmpty()) {
                _uiState.value = DiscoverUiState.Empty("No movies found for $genre.")
                return@launch
            }

            val topSummaries = summaries.distinctBy { it.imdbId }.take(10)
            val details = repository.getMovieDetailsBatch(topSummaries)
            details.forEach { knownDetails[it.imdbId] = it }
            currentMovieIds = details.map { it.imdbId }
            render()
        }
    }

    /** Optional: if the user does choose to type a title in the search bar */
    fun searchMovies(query: String) {
        val trimmed = query.trim()
        lastQuery = trimmed

        if (trimmed.isEmpty()) {
            // If the user clears the search bar, revert to the active category
            loadGenreMovies(_selectedGenre.value)
            return
        }

        _uiState.value = DiscoverUiState.Loading
        viewModelScope.launch {
            repository.searchMovies(trimmed)
                .onSuccess { summaries ->
                    val details = repository.getMovieDetailsBatch(summaries)
                    details.forEach { knownDetails[it.imdbId] = it }
                    currentMovieIds = details.map { it.imdbId }
                    render()
                }
                .onFailure { error ->
                    currentMovieIds = emptyList()
                    _uiState.value = DiscoverUiState.Error(error.message ?: "Something went wrong. Please try again.")
                }
        }
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
        val details = currentMovieIds.mapNotNull { knownDetails[it] }

        val filtered = if (genre == "All" || lastQuery.isNotEmpty()) {
            details
        } else {
            val matching = details.filter { it.genre?.contains(genre, ignoreCase = true) == true }
            matching.ifEmpty { details }
        }

        _uiState.value = if (filtered.isEmpty()) {
            DiscoverUiState.Empty(
                if (lastQuery.isNotEmpty()) "No results found for \"$lastQuery\"." else "No $genre movies found."
            )
        } else {
            DiscoverUiState.Success(
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