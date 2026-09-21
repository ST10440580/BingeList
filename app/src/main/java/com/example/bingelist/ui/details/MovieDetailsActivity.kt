package com.example.bingelist.ui.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.bingelist.R
import com.example.bingelist.databinding.ActivityMovieDetailsBinding
import kotlinx.coroutines.launch

class MovieDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailsBinding
    private val viewModel: MovieDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imdbId = intent.getStringExtra(EXTRA_IMDB_ID)
        if (imdbId.isNullOrBlank()) {
            finish()
            return
        }

        binding.backButton.setOnClickListener { finish() }
        binding.favoriteIcon.setOnClickListener { viewModel.toggleFavorite() }
        binding.watchlistButton.setOnClickListener {
            Toast.makeText(this, "Watchlist coming soon", Toast.LENGTH_SHORT).show()
        }

        observeState()
        viewModel.loadMovie(imdbId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshFavorite()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: DetailsUiState) {
        binding.loadingIndicator.visibility = View.GONE
        binding.errorText.visibility = View.GONE

        when (state) {
            is DetailsUiState.Loading -> binding.loadingIndicator.visibility = View.VISIBLE
            is DetailsUiState.Error -> {
                binding.errorText.text = state.message
                binding.errorText.visibility = View.VISIBLE
            }
            is DetailsUiState.Success -> {
                val movie = state.movie
                binding.movieTitle.text = movie.title
                binding.genreBadge.text = movie.genre?.split(",")?.firstOrNull()?.trim() ?: "Movie"
                binding.metaText.text = "${movie.year} · ${movie.runtime ?: "—"}"
                binding.ratingText.text = movie.imdbRating ?: "—"
                binding.plotText.text = movie.plot ?: "No description available."
                //binding.directorText.text = "Director: ${movie.director ?: "Unknown"}"
                //binding.actorsText.text = "Starring: ${movie.actors ?: "Unknown"}"

                Glide.with(binding.posterImage)
                    .load(movie.posterUrlOrNull)
                    .placeholder(R.drawable.ic_movie_placeholder)
                    .error(R.drawable.ic_movie_placeholder)
                    .centerCrop()
                    .into(binding.posterImage)

                binding.favoriteIcon.setImageResource(
                    if (state.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                )
            }
        }
    }

    companion object {
        private const val EXTRA_IMDB_ID = "extra_imdb_id"
        fun start(context: Context, imdbId: String) {
            context.startActivity(Intent(context, MovieDetailsActivity::class.java).putExtra(EXTRA_IMDB_ID, imdbId))
        }
    }
}