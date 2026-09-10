package com.example.bingelist.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.model.MovieDetail
import com.example.bingelist.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailsUiState {
    object Loading : DetailsUiState()
    data class Success(val movie: MovieDetail, val isFavorite: Boolean, val isInWatchlist: Boolean) : DetailsUiState()
    data class Error(val message: String) : DetailsUiState()
}

class MovieDetailsViewModel(
    private val repository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var currentMovie: MovieDetail? = null
    private var isFavorite = false
    private var isInWatchlist = false

    fun loadMovie(imdbId: String) {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            repository.getMovieDetails(imdbId)
                .onSuccess { movie ->
                    currentMovie = movie
                    _uiState.value = DetailsUiState.Success(movie, isFavorite, isInWatchlist)
                }
                .onFailure { e ->
                    _uiState.value = DetailsUiState.Error(e.message ?: "Couldn't load this movie.")
                }
        }
    }

    fun toggleFavorite() {
        isFavorite = !isFavorite
        currentMovie?.let { _uiState.value = DetailsUiState.Success(it, isFavorite, isInWatchlist) }
    }

    fun toggleWatchlist() {
        isInWatchlist = !isInWatchlist
        currentMovie?.let { _uiState.value = DetailsUiState.Success(it, isFavorite, isInWatchlist) }
    }
}