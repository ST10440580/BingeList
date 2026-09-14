package com.example.bingelist.ui.watchlist

import com.example.bingelist.data.model.MovieDetail

object InMemoryStore {
    val favorites = mutableSetOf<String>()
    val watchlist = mutableSetOf<String>()

    // movie details cached to avoid re-fetching on the watchlist/favorites screens
    val movieCache = mutableMapOf<String, MovieDetail>()
}