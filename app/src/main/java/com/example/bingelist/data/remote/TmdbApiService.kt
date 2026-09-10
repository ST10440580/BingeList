package com.example.bingelist.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("movie/now_playing")
    suspend fun getNowPlaying(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("region") region: String = "US"
    ): TmdbNowPlayingResponse

    @GET("movie/{id}/external_ids")
    suspend fun getExternalIds(@Path("id") id: Int): TmdbExternalIdsResponse
}