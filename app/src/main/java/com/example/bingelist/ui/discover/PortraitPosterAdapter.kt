package com.example.bingelist.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bingelist.R
import com.example.bingelist.data.model.MovieCardUiModel
import com.example.bingelist.databinding.ItemPosterPotraitBinding

class PortraitPosterAdapter(
    private val onCardClick: (MovieCardUiModel) -> Unit
) : ListAdapter<MovieCardUiModel, PortraitPosterAdapter.PosterViewHolder>(DIFF_CALLBACK) {

    inner class PosterViewHolder(private val binding: ItemPosterPotraitBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: MovieCardUiModel) {
            binding.movieTitle.text = movie.title
            binding.ratingText.text = movie.rating
            Glide.with(binding.posterImage)
                .load(movie.posterUrl)
                .placeholder(R.drawable.ic_movie_placeholder)
                .error(R.drawable.ic_movie_placeholder)
                .centerCrop()
                .into(binding.posterImage)
            binding.root.setOnClickListener { onCardClick(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        val binding = ItemPosterPotraitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PosterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MovieCardUiModel>() {
            override fun areItemsTheSame(old: MovieCardUiModel, new: MovieCardUiModel) = old.imdbId == new.imdbId
            override fun areContentsTheSame(old: MovieCardUiModel, new: MovieCardUiModel) = old == new
        }
    }
}