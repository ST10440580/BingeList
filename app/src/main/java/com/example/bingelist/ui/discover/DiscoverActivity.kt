package com.example.bingelist.ui.discover

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bingelist.databinding.ActivityDiscoverBinding
import com.example.bingelist.ui.watchlist.WatchlistActivity
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

        setupNewThisWeek()
        setupGenreChips()
        setupResultsList()
        setupSearchView()
        setupBottomNav()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
    }

    private fun setupNewThisWeek() {
        newThisWeekAdapter = PortraitPosterAdapter { movie ->
            com.example.bingelist.ui.details.MovieDetailsActivity.start(this, movie.imdbId)
        }
        binding.newThisWeekRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.newThisWeekRecyclerView.adapter = newThisWeekAdapter
    }

    private fun setupGenreChips() {
        genreAdapter = GenreAdapter(DiscoverViewModel.GENRES) { genre ->
            viewModel.selectGenre(genre)
        }
        binding.genreRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.genreRecyclerView.adapter = genreAdapter
    }

    private fun setupResultsList() {
        resultsAdapter = MovieCardAdapter(
            onCardClick = { movie -> com.example.bingelist.ui.details.MovieDetailsActivity.start(this, movie.imdbId) },
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

            override fun onQueryTextChange(newText: String?): Boolean = false
        })
    }

    private fun setupBottomNav() {
        binding.navWatchList.setOnClickListener {
            startActivity(Intent(this, WatchlistActivity::class.java))
        }
        binding.navFavorites.setOnClickListener {
            Toast.makeText(this, "Favorites screen coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.navSettings.setOnClickListener {
            Toast.makeText(this, "Settings screen coming soon", Toast.LENGTH_SHORT).show()
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

    private fun renderNewThisWeek(movies: List<com.example.bingelist.data.model.MovieCardUiModel>) {
        newThisWeekAdapter.submitList(movies)
        binding.newThisWeekCount.text = "${movies.size} new this week"
        binding.newThisWeekTitles.text = if (movies.isEmpty()) "Loading this week's picks…" else movies.joinToString(" · ") { it.title }
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
