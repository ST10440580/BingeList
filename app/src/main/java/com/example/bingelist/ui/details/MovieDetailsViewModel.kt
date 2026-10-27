package com.example.bingelist.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bingelist.data.database.BingeListDatabaseHelper
import com.example.bingelist.data.model.MovieDetail
import com.example.bingelist.data.remote.LibraryList
import com.example.bingelist.data.repository.MovieRepository
import com.example.bingelist.data.sync.LibrarySync
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DetailsUiState {
    object Loading : DetailsUiState()
    data class Success(
        val movie: MovieDetail,
        val isFavorite: Boolean,
        val isInWatchlist: Boolean
    ) : DetailsUiState()
    data class Error(val message: String) : DetailsUiState()
}

class MovieDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovieRepository()
    private val dbHelper = BingeListDatabaseHelper(application)
    private val sync = LibrarySync(dbHelper)
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var currentMovie: MovieDetail? = null

    fun loadMovie(imdbId: String) {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            repository.getMovieDetails(imdbId)
                .onSuccess { movie ->
                    currentMovie = movie
                    withContext(Dispatchers.IO) { dbHelper.cacheMovie(movie) }
                    emitCurrentState(movie)
                }
                .onFailure { e ->
                    _uiState.value = DetailsUiState.Error(e.message ?: "Couldn't load this movie.")
                }
        }
    }

    /** Re-reads favorite and watchlist flags without re-fetching from OMDb. */
    fun refreshState() {
        val movie = currentMovie ?: return
        viewModelScope.launch { emitCurrentState(movie) }
    }

    fun toggleFavorite() {
        val movie = currentMovie ?: return
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val nowFavorite = withContext(Dispatchers.IO) {
                val wasFavorite = dbHelper.isFavorite(uid, movie.imdbId)
                if (wasFavorite) dbHelper.removeFromFavorites(uid, movie.imdbId)
                else dbHelper.addToFavorites(uid, movie.imdbId)
                !wasFavorite
            }
            emitCurrentState(movie)
            sync.push(uid, LibraryList.FAVORITES, movie.imdbId, nowFavorite)
        }
    }

    fun toggleWatchlist() {
        val movie = currentMovie ?: return
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val nowInWatchlist = withContext(Dispatchers.IO) {
                val inWatchlist = dbHelper.isInWatchlist(uid, movie.imdbId)
                if (inWatchlist) dbHelper.removeFromWatchlist(uid, movie.imdbId)
                else dbHelper.addToWatchlist(uid, movie.imdbId)
                !inWatchlist
            }
            emitCurrentState(movie)
            sync.push(uid, LibraryList.WATCHLIST, movie.imdbId, nowInWatchlist)
        }
    }

    fun removeFromWatchlist() {
        val movie = currentMovie ?: return
        val uid = currentUserId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.removeFromWatchlist(uid, movie.imdbId)
            }
            emitCurrentState(movie)
            sync.push(uid, LibraryList.WATCHLIST, movie.imdbId, false)
        }
    }

    private suspend fun emitCurrentState(movie: MovieDetail) {
        val uid = currentUserId
        val isFav = if (uid != null) {
            withContext(Dispatchers.IO) { dbHelper.isFavorite(uid, movie.imdbId) }
        } else {
            false
        }
        val inWatchlist = if (uid != null) {
            withContext(Dispatchers.IO) { dbHelper.isInWatchlist(uid, movie.imdbId) }
        } else {
            false
        }
        _uiState.value = DetailsUiState.Success(movie, isFav, inWatchlist)
    }
}