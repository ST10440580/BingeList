package com.example.bingelist.data.remote

import com.google.gson.annotations.SerializedName

data class TmdbNowPlayingResponse(
    @SerializedName("results") val results: List<TmdbMovieSummary>
)

data class TmdbMovieSummary(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("release_date") val releaseDate: String?
)

data class TmdbExternalIdsResponse(
    @SerializedName("imdb_id") val imdbId: String?
)