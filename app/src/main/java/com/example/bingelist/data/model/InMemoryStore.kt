package com.example.bingelist.data.model

object InMemoryStore {
    val favorites = mutableSetOf<String>()
    val watchlist = mutableSetOf<String>()

    // movie details cached to avoid re-fetching on the watchlist/favorites screens
    val movieCache = mutableMapOf<String, MovieDetail>()
}
