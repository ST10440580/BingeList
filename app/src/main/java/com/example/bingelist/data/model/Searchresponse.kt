package com.example.bingelist.data.model

import com.google.gson.annotations.SerializedName


data class SearchResponse(
    @SerializedName("Search") val results: List<MovieSummary>?,
    @SerializedName("totalResults") val totalResults: String?,
    @SerializedName("Response") val response: String,
    @SerializedName("Error") val error: String? = null
) {
    val isSuccess: Boolean get() = response == "True"
}