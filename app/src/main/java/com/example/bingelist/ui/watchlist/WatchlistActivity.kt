package com.example.bingelist.ui.watchlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bingelist.databinding.ActivityWatchlistBinding
import com.example.bingelist.ui.account.LoginActivity
import com.example.bingelist.ui.details.MovieDetailsActivity
import com.example.bingelist.ui.discover.DiscoverActivity
import com.example.bingelist.ui.discover.MovieCardAdapter
import com.google.firebase.auth.FirebaseAuth
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
            finish()
        }

        // Watchlist is already the active screen
        binding.navWatchList.setOnClickListener {
            binding.watchlistRecyclerView.smoothScrollToPosition(0)
        }

        binding.navFavorites.setOnClickListener {
            Toast.makeText(this, "Favorites screen coming soon", Toast.LENGTH_SHORT).show()
        }

        // Settings / Log Out Dialog
        binding.navSettings.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Account")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
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