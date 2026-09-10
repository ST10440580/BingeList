package com.example.bingelist.data.remote

import com.example.bingelist.data.model.MovieDetail
import com.example.bingelist.data.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Query


interface OMDbApiService {

    @GET("/")
    suspend fun searchMovies(
        @Query("s") query: String,
        @Query("type") type: String = "movie",
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("/")
    suspend fun getMovieDetails(
        @Query("i") imdbId: String
    ): MovieDetail
}