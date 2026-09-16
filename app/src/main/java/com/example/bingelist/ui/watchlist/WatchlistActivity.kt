package com.example.bingelist.ui.watchlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bingelist.databinding.ActivityWatchlistBinding
import com.example.bingelist.ui.details.MovieDetailsActivity
import com.example.bingelist.ui.discover.DiscoverActivity
import com.example.bingelist.ui.discover.MovieCardAdapter
import com.example.bingelist.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class WatchlistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWatchlistBinding
    private val viewModel: WatchlistViewModel by viewModels()
    private lateinit var adapter: MovieCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchlistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupBottomNav()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadWatchlist()
    }

    private fun setupRecyclerView() {
        adapter = MovieCardAdapter(
            onCardClick = { movie -> MovieDetailsActivity.start(this, movie.imdbId) },
            onFavoriteClick = { movie -> viewModel.toggleFavorite(movie.imdbId) },
            onWatchlistClick = { movie -> viewModel.removeFromWatchlist(movie.imdbId) }
        )
        binding.watchlistRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.watchlistRecyclerView.adapter = adapter
    }

    private fun setupBottomNav() {
        binding.navDiscover.setOnClickListener {
            val intent = Intent(this, DiscoverActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
        // Watchlist is already active
        binding.navFavorites.setOnClickListener {
            // Placeholder
        }
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: WatchlistUiState) {
        when (state) {
            is WatchlistUiState.Loading -> {
                binding.watchlistRecyclerView.visibility = View.GONE
                binding.emptyStateMessage.visibility = View.GONE
                // Could add a progress bar if needed
            }
            is WatchlistUiState.Success -> {
                binding.watchlistRecyclerView.visibility = View.VISIBLE
                binding.emptyStateMessage.visibility = View.GONE
                adapter.submitList(state.movies)
            }
            is WatchlistUiState.Empty -> {
                binding.watchlistRecyclerView.visibility = View.GONE
                binding.emptyStateMessage.visibility = View.VISIBLE
            }
            is WatchlistUiState.Error -> {
                binding.watchlistRecyclerView.visibility = View.GONE
                binding.emptyStateMessage.visibility = View.VISIBLE
                binding.emptyStateMessage.text = state.message
            }
        }
    }
}
