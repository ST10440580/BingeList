package com.example.bingelist.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.databinding.ActivityFavoritesBinding
import com.example.bingelist.ui.details.MovieDetailsActivity
import com.example.bingelist.ui.discover.DiscoverActivity
import com.example.bingelist.ui.discover.MovieCardAdapter
import com.example.bingelist.ui.settings.SettingsActivity
import com.example.bingelist.ui.watchlist.WatchlistActivity
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var adapter: MovieCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MovieCardAdapter(
            onCardClick = { movie -> MovieDetailsActivity.start(this, movie.imdbId) },
            onFavoriteClick = { movie -> viewModel.toggleFavorite(movie.imdbId) },
            onWatchlistClick = { _ -> Toast.makeText(this, "Watchlist coming soon", Toast.LENGTH_SHORT).show() }
        )
        binding.favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.favoritesRecyclerView.adapter = adapter

        observeFavorites()
        setupBottomNav()
    }

    private fun observeFavorites() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favorites.collect { list -> render(list) }
            }
        }
    }

    private fun render(movies: List<MovieCardUiModel>) {
        val isEmpty = movies.isEmpty()
        binding.favoritesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        adapter.submitList(movies)
    }


    private fun setupBottomNav() {

        binding.navDiscover.setOnClickListener {
            startActivity(Intent(this, DiscoverActivity::class.java))
        }

        binding.navWatchList.setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
        }


        binding.navFavorites.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }


        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavorites()
    }
}