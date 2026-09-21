package com.example.bingelist.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bingelist.databinding.ActivitySettingsBinding
import com.example.bingelist.ui.account.LoginActivity
import com.example.bingelist.ui.discover.DiscoverActivity
import com.example.bingelist.ui.favorites.FavoritesActivity
import com.example.bingelist.ui.watchlist.WatchlistActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var languageAdapter: LanguageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupBottomNav()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to log out of BingeList?")
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

    private fun setupBottomNav() {
        binding.navDiscover.setOnClickListener {
            val intent = Intent(this, DiscoverActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        binding.navWatchList.setOnClickListener {
            val intent = Intent(this, WatchlistActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        binding.navFavorites.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        binding.navSettings.setOnClickListener {
            // Already on Settings screen
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

    private fun render(state: SettingsUiState) {
        // Setup / update Language RecyclerView
        languageAdapter = LanguageAdapter(
            languages = state.availableLanguages,
            selectedLanguage = state.selectedLanguage
        ) { selectedLang ->
            viewModel.selectLanguage(selectedLang)
        }
        binding.languageRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.languageRecyclerView.adapter = languageAdapter

        // Update Switch state
        binding.switchNotifications.setOnCheckedChangeListener(null)
        binding.switchNotifications.isChecked = state.notificationsEnabled
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
        }
    }
}