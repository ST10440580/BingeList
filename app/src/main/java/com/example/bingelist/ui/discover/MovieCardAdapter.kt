package com.example.bingelist.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bingelist.R
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.databinding.ItemMovieCardBinding

class MovieCardAdapter(
    private val onCardClick: (MovieCardUiModel) -> Unit,
    private val onFavoriteClick: (MovieCardUiModel) -> Unit,
    private val onWatchlistClick: (MovieCardUiModel) -> Unit
) : ListAdapter<MovieCardUiModel, MovieCardAdapter.CardViewHolder>(DIFF_CALLBACK) {

    inner class CardViewHolder(private val binding: ItemMovieCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: MovieCardUiModel) {
            binding.movieTitle.text = movie.title
            binding.genreBadge.text = movie.genre.ifEmpty { "Movie" }
            binding.metaText.text = if (movie.runtime.isNotEmpty()) "${movie.year} · ${movie.runtime}" else movie.year
            binding.plotText.text = movie.plot
            binding.ratingText.text = movie.rating

            Glide.with(binding.posterImage)
                .load(movie.posterUrl)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .centerCrop()
                .into(binding.posterImage)

            binding.favoriteIcon.setImageResource(
                if (movie.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )

            if (movie.isInWatchlist) {
                binding.watchlistButton.setBackgroundResource(R.drawable.chip_background_unselected)
                binding.watchlistIcon.setImageResource(R.drawable.ic_check_small)
                binding.watchlistText.text = "In watchlist"
                binding.watchlistText.setTextColor(binding.root.context.getColor(R.color.bl_chip_unselected_text))
            } else {
                binding.watchlistButton.setBackgroundResource(R.drawable.chip_background_selected)
                binding.watchlistIcon.setImageResource(R.drawable.ic_add_small)
                binding.watchlistText.text = "Add to watchlist"
                binding.watchlistText.setTextColor(binding.root.context.getColor(R.color.bl_chip_selected_text))
            }

            binding.root.setOnClickListener { onCardClick(movie) }
            binding.favoriteIcon.setOnClickListener { onFavoriteClick(movie) }
            binding.watchlistButton.setOnClickListener { onWatchlistClick(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemMovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MovieCardUiModel>() {
            override fun areItemsTheSame(old: MovieCardUiModel, new: MovieCardUiModel) = old.imdbId == new.imdbId
            override fun areContentsTheSame(old: MovieCardUiModel, new: MovieCardUiModel) = old == new
        }
    }
}