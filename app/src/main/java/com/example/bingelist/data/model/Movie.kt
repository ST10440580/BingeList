package com.example.bingelist.data.model

import com.google.gson.annotations.SerializedName


data class MovieSummary(
    @SerializedName("Title") val title: String,
    @SerializedName("Year") val year: String,
    @SerializedName("imdbID") val imdbId: String,
    @SerializedName("Type") val type: String?,
    @SerializedName("Poster") val poster: String?
) {

    val posterUrlOrNull: String?
        get() = poster?.takeIf { it.isNotBlank() && it != "N/A" }
}


data class MovieDetail(
    @SerializedName("Title") val title: String,
    @SerializedName("Year") val year: String,
    @SerializedName("Rated") val rated: String?,
    @SerializedName("Released") val released: String?,
    @SerializedName("Runtime") val runtime: String?,
    @SerializedName("Genre") val genre: String?,
    @SerializedName("Director") val director: String?,
    @SerializedName("Actors") val actors: String?,
    @SerializedName("Plot") val plot: String?,
    @SerializedName("Language") val language: String?,
    @SerializedName("Country") val country: String?,
    @SerializedName("Poster") val poster: String?,
    @SerializedName("imdbRating") val imdbRating: String?,
    @SerializedName("imdbVotes") val imdbVotes: String?,
    @SerializedName("imdbID") val imdbId: String,
    @SerializedName("Response") val response: String,
    @SerializedName("Error") val error: String? = null
) {
    val posterUrlOrNull: String?
        get() = poster?.takeIf { it.isNotBlank() && it != "N/A" }
}