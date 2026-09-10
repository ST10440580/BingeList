package com.example.bingelist.data.model

data class MovieCardUiModel(
    val imdbId: String,
    val title: String,
    val posterUrl: String?,
    val year: String,
    val runtime: String,
    val genre: String,
    val rating: String,
    val plot: String,
    val isFavorite: Boolean = false,
    val isInWatchlist: Boolean = false
)

fun MovieDetail.toCardUiModel(isFavorite: Boolean = false, isInWatchlist: Boolean = false) =
    MovieCardUiModel(
        imdbId = imdbId,
        title = title,
        posterUrl = posterUrlOrNull,
        year = year,
        runtime = runtime?.takeIf { it != "N/A" } ?: "",
        genre = genre?.split(",")?.firstOrNull()?.trim() ?: "",
        rating = imdbRating?.takeIf { it != "N/A" } ?: "—",
        plot = plot?.takeIf { it != "N/A" } ?: "No description available.",
        isFavorite = isFavorite,
        isInWatchlist = isInWatchlist
    )