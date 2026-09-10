package com.example.bingelist.data.repository

import com.example.bingelist.data.model.MovieDetail
import com.example.bingelist.data.model.MovieSummary
import com.example.bingelist.data.remote.OMDbApiService
import com.example.bingelist.data.remote.RetrofitClient
import com.example.bingelist.data.remote.TmdbApiService
import com.example.bingelist.data.remote.TmdbRetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException

class MovieRepository(
    private val api: OMDbApiService = RetrofitClient.apiService,
    private val tmdbApi: TmdbApiService = TmdbRetrofitClient.apiService
) {
    suspend fun searchMovies(query: String): Result<List<MovieSummary>> {
        return try {
            val response = api.searchMovies(query = query)
            if (response.isSuccess && response.results != null) {
                Result.success(response.results)
            } else {
                Result.failure(NoResultsException(response.error ?: "No movies found."))
            }
        } catch (e: IOException) {
            Result.failure(NetworkException("Couldn't reach OMDb. Check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieDetails(imdbId: String): Result<MovieDetail> {
        return try {
            val detail = api.getMovieDetails(imdbId)
            if (detail.response == "True") {
                Result.success(detail)
            } else {
                Result.failure(NoResultsException(detail.error ?: "Movie details not found."))
            }
        } catch (e: IOException) {
            Result.failure(NetworkException("Couldn't reach OMDb. Check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 👇 moved inside the class so it can access api
    suspend fun getMovieDetailsBatch(movies: List<MovieSummary>): List<MovieDetail> = coroutineScope {
        movies.map { movie ->
            async { runCatching { api.getMovieDetails(movie.imdbId) }.getOrNull() }
        }.awaitAll().filterNotNull()
    }


suspend fun getMovieDetailsByIds(ids: List<String>): List<MovieDetail> = coroutineScope {
    ids.map { id -> async { runCatching { api.getMovieDetails(id) }.getOrNull() } }
        .awaitAll().filterNotNull()
}

    suspend fun getWeeklyNewReleaseIds(limit: Int = 4): List<String> = coroutineScope {
        try {
            val results = tmdbApi.getNowPlaying().results
                .filter { !it.releaseDate.isNullOrBlank() }
                .sortedByDescending { it.releaseDate }

            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

            val thisWeek = results.filter { movie ->
                runCatching { fmt.parse(movie.releaseDate!!)?.time ?: 0L }.getOrDefault(0L) >= sevenDaysAgo
            }
            val candidates = (thisWeek + results).distinctBy { it.id }.take(limit)

            candidates.map { movie -> async { runCatching { tmdbApi.getExternalIds(movie.id).imdbId }.getOrNull() } }
                .awaitAll().filterNotNull().filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}


class NoResultsException(message: String) : Exception(message)
class NetworkException(message: String) : Exception(message)
