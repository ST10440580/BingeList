package com.example.bingelist.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bingelist.databinding.ActivitySettingsBinding
import com.example.bingelist.ui.discover.DiscoverActivity
import com.example.bingelist.ui.watchlist.WatchlistActivity
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var languageAdapter: LanguageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNav()
        observeViewModel()
    }

    private fun setupBottomNav() {
        binding.navDiscover.setOnClickListener {
            val intent = Intent(this, DiscoverActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
        binding.navWatchList.setOnClickListener {
            val intent = Intent(this, WatchlistActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }
        binding.navFavorites.setOnClickListener {
            Toast.makeText(this, "Favorites screen coming soon", Toast.LENGTH_SHORT).show()
        }
        // Settings is already active
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

    private fun render(state: SettingsUiState) {
        // Setup or update Language RecyclerView
        if (!::languageAdapter.isInitialized) {
            languageAdapter = LanguageAdapter(
                languages = state.availableLanguages,
                selectedLanguage = state.selectedLanguage
            ) { selectedLang ->
                viewModel.selectLanguage(selectedLang)
            }
            binding.languageRecyclerView.layoutManager = LinearLayoutManager(this)
            binding.languageRecyclerView.adapter = languageAdapter
        } else {
            // Re-create or notify adapter if language changed externally
            languageAdapter = LanguageAdapter(
                languages = state.availableLanguages,
                selectedLanguage = state.selectedLanguage
            ) { selectedLang ->
                viewModel.selectLanguage(selectedLang)
            }
            binding.languageRecyclerView.adapter = languageAdapter
        }

        // Update Switch state
        binding.switchNotifications.setOnCheckedChangeListener(null) // Prevent loop
        binding.switchNotifications.isChecked = state.notificationsEnabled
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
        }
    }
}