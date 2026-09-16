package com.example.bingelist.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bingelist.R

class LanguageAdapter(
    private val languages: List<String>,
    private var selectedLanguage: String,
    private val onLanguageSelected: (String) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvLanguageName)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language_option, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = languages[position]
        val isSelected = language == selectedLanguage

        holder.tvName.text = language

        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.chip_background_selected)
            holder.tvName.setTextColor(holder.itemView.context.getColor(R.color.bl_chip_selected_text))
            holder.ivCheck.visibility = View.VISIBLE
        } else {
            holder.itemView.setBackgroundResource(R.drawable.chip_background_unselected)
            holder.tvName.setTextColor(holder.itemView.context.getColor(R.color.bl_chip_unselected_text))
            holder.ivCheck.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (selectedLanguage != language) {
                val previous = selectedLanguage
                selectedLanguage = language
                notifyItemChanged(languages.indexOf(previous))
                notifyItemChanged(position)
                onLanguageSelected(language)
            }
        }
    }

    override fun getItemCount() = languages.size
}