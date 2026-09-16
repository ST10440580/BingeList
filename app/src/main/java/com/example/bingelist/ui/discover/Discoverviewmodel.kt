package com.example.bingelist.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.data.model.MovieDetail
import com.example.bingelist.data.model.toCardUiModel
import com.example.bingelist.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DiscoverUiState {
    object Idle : DiscoverUiState()
    object Loading : DiscoverUiState()
    data class Success(val movies: List<MovieCardUiModel>) : DiscoverUiState()
    data class Empty(val message: String) : DiscoverUiState()
    data class Error(val message: String) : DiscoverUiState()
}

class DiscoverViewModel(
    private val repository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Idle)
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _newThisWeek = MutableStateFlow<List<MovieCardUiModel>>(emptyList())
    val newThisWeek: StateFlow<List<MovieCardUiModel>> = _newThisWeek.asStateFlow()

    private val _selectedGenre = MutableStateFlow(GENRES.first())
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    // In-memory for now — swap for Room/Firestore when the Favorites/Watchlist
    // screens are built, per the Part 1 data model.
    private val favorites = mutableSetOf<String>()
    private val watchlist = mutableSetOf<String>()

    private var lastQuery = ""
    private var allDetails: List<MovieDetail> = emptyList()

    init {
        loadNewThisWeek()
    }

    private fun loadNewThisWeek() {
        viewModelScope.launch {
            val ids = repository.getWeeklyNewReleaseIds(limit = 4)
            val details = repository.getMovieDetailsByIds(ids)
            _newThisWeek.value = details.map {
                it.toCardUiModel(favorites.contains(it.imdbId), watchlist.contains(it.imdbId))
            }
        }
    }

    fun refreshState() {
        render()
    }

    fun searchMovies(query: String) {
        val trimmed = query.trim()
        lastQuery = trimmed

        if (trimmed.isEmpty()) {
            allDetails = emptyList()
            _uiState.value = DiscoverUiState.Idle
            return
        }

        _uiState.value = DiscoverUiState.Loading
        viewModelScope.launch {
            repository.searchMovies(trimmed)
                .onSuccess { summaries ->
                    allDetails = repository.getMovieDetailsBatch(summaries)
                    render()
                }
                .onFailure { error ->
                    allDetails = emptyList()
                    _uiState.value = DiscoverUiState.Error(error.message ?: "Something went wrong. Please try again.")
                }
        }
    }

    fun selectGenre(genre: String) {
        _selectedGenre.value = genre
        if (lastQuery.isNotEmpty()) render()
    }

    fun toggleFavorite(imdbId: String) {
        if (!favorites.add(imdbId)) favorites.remove(imdbId)
        render()
        _newThisWeek.value = _newThisWeek.value.map {
            if (it.imdbId == imdbId) it.copy(isFavorite = favorites.contains(imdbId)) else it
        }
    }

    fun toggleWatchlist(imdbId: String) {
        if (!watchlist.add(imdbId)) watchlist.remove(imdbId)
        render()
    }

    private fun render() {
        val genre = _selectedGenre.value
        val filtered = if (genre == "All") {
            allDetails
        } else {
            allDetails.filter { it.genre?.contains(genre, ignoreCase = true) == true }
        }

        _uiState.value = when {
            lastQuery.isEmpty() -> DiscoverUiState.Idle
            filtered.isEmpty() -> DiscoverUiState.Empty(
                if (genre == "All") "No movies found for \"$lastQuery\"." else "No $genre movies found for \"$lastQuery\"."
            )
            else -> DiscoverUiState.Success(
                filtered.map { it.toCardUiModel(favorites.contains(it.imdbId), watchlist.contains(it.imdbId)) }
            )
        }
    }

    companion object {
        val GENRES = listOf("All", "Action", "Animation", "Comedy", "Drama")
        private val NEW_THIS_WEEK_IDS = listOf(
            "tt15239678", "tt13238346", "tt14849194", "tt9362722"
        )
    }
}