package com.example.bingelist.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bingelist.R

class GenreAdapter(
    private val genres: List<String>,
    private val onGenreSelected: (String) -> Unit
) : RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {

    private var selectedIndex = 0

    inner class GenreViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_genre_chip, parent, false) as TextView
        return GenreViewHolder(view)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val genre = genres[position]
        val isSelected = position == selectedIndex

        holder.textView.text = genre
        holder.textView.setBackgroundResource(
            if (isSelected) R.drawable.chip_background_selected else R.drawable.chip_background_unselected
        )
        holder.textView.setTextColor(
            holder.textView.context.getColor(
                if (isSelected) R.color.bl_chip_selected_text else R.color.bl_chip_unselected_text
            )
        )

        holder.textView.setOnClickListener {
            if (selectedIndex != position) {
                val previous = selectedIndex
                selectedIndex = position
                notifyItemChanged(previous)
                notifyItemChanged(selectedIndex)
                onGenreSelected(genre)
            }
        }
    }

    override fun getItemCount() = genres.size
}