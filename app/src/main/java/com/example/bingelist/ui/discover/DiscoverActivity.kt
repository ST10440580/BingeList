package com.example.bingelist.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.databinding.ActivityDiscoverBinding
import com.example.bingelist.ui.details.MovieDetailsActivity
import com.example.bingelist.ui.favorites.FavoritesActivity
import com.example.bingelist.ui.settings.SettingsActivity
import com.example.bingelist.ui.watchlist.WatchlistActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class DiscoverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiscoverBinding
    private val viewModel: DiscoverViewModel by viewModels()

    private lateinit var newThisWeekAdapter: PortraitPosterAdapter
    private lateinit var resultsAdapter: MovieCardAdapter
    private lateinit var genreAdapter: GenreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscoverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Set up profile button to open ProfileActivity
        binding.btnProfile.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, com.example.bingelist.ui.account.LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        setupNewThisWeek()
        setupGenreChips()
        setupResultsList()
        setupSearchView()
        setupBottomNav()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh watchlist and favorites status icons when returning to Discover
        viewModel.refreshWatchlist()
        viewModel.refreshFavorites()
    }

    private fun setupNewThisWeek() {
        newThisWeekAdapter = PortraitPosterAdapter { movie ->
            MovieDetailsActivity.start(this, movie.imdbId)
        }
        binding.newThisWeekRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.newThisWeekRecyclerView.adapter = newThisWeekAdapter
    }

    private fun setupGenreChips() {
        genreAdapter = GenreAdapter(DiscoverViewModel.GENRES) { genre ->
            // Clear manual search query so category browsing takes over
            binding.searchView.setQuery("", false)
            binding.searchView.clearFocus()
            viewModel.selectGenre(genre)
        }
        binding.genreRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.genreRecyclerView.adapter = genreAdapter
    }

    private fun setupResultsList() {
        resultsAdapter = MovieCardAdapter(
            onCardClick = { movie -> MovieDetailsActivity.start(this, movie.imdbId) },
            onFavoriteClick = { movie -> viewModel.toggleFavorite(movie.imdbId) },
            onWatchlistClick = { movie -> viewModel.toggleWatchlist(movie.imdbId) }
        )
        binding.movieRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.movieRecyclerView.adapter = resultsAdapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchMovies(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // If user clears the text with backspace or (X), restore the current genre view
                if (newText.isNullOrBlank()) {
                    viewModel.searchMovies("")
                }
                return false
            }
        })
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

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.newThisWeek.collect { movies -> renderNewThisWeek(movies) } }
                launch { viewModel.uiState.collect { state -> renderResults(state) } }
            }
        }
    }

    private fun renderNewThisWeek(movies: List<MovieCardUiModel>) {
        newThisWeekAdapter.submitList(movies)
        binding.newThisWeekCount.text = "${movies.size} new this week"
        binding.newThisWeekTitles.text = if (movies.isEmpty()) {
            "Loading this week's picks…"
        } else {
            movies.joinToString(" · ") { it.title }
        }
    }

    private fun renderResults(state: DiscoverUiState) {
        binding.loadingIndicator.visibility = View.GONE
        binding.statusMessage.visibility = View.GONE
        binding.movieRecyclerView.visibility = View.GONE
        binding.resultsCount.visibility = View.GONE

        when (state) {
            is DiscoverUiState.Idle -> {
                binding.statusMessage.visibility = View.VISIBLE
            }
            is DiscoverUiState.Loading -> {
                binding.loadingIndicator.visibility = View.VISIBLE
            }
            is DiscoverUiState.Success -> {
                binding.movieRecyclerView.visibility = View.VISIBLE
                binding.resultsCount.visibility = View.VISIBLE
                binding.resultsCount.text = "${state.movies.size} results"
                resultsAdapter.submitList(state.movies)
            }
            is DiscoverUiState.Empty -> {
                binding.statusMessage.text = state.message
                binding.statusMessage.visibility = View.VISIBLE
            }
            is DiscoverUiState.Error -> {
                binding.statusMessage.text = state.message
                binding.statusMessage.visibility = View.VISIBLE
            }
        }
    }
}