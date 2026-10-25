package com.example.bingelist.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bingelist.databinding.ActivitySettingsBinding
import com.example.bingelist.notifications.DemoNotificationService
import com.example.bingelist.ui.account.LoginActivity
import com.example.bingelist.ui.discover.DiscoverActivity
import com.example.bingelist.ui.favorites.FavoritesActivity
import com.example.bingelist.ui.watchlist.WatchlistActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val demoPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startDemo() else binding.switchDemo.isChecked = false
        }

    private lateinit var languageAdapter: LanguageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupBottomNav()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Show the real state (the demo service may have stopped itself) without
        // triggering the listener, then attach the listener again.
        binding.switchDemo.setOnCheckedChangeListener(null)
        binding.switchDemo.isChecked = DemoNotificationService.isRunning
        setupDemoSwitch()
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun setupDemoSwitch() {
        binding.switchDemo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    demoPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    startDemo()
                }
            } else {
                stopService(Intent(this, DemoNotificationService::class.java))
            }
        }
    }

    private fun startDemo() {
        ContextCompat.startForegroundService(this, Intent(this, DemoNotificationService::class.java))
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

        // Restore the saved switch state without firing the listener, then attach it
        binding.switchNotifications.setOnCheckedChangeListener(null)
        binding.switchNotifications.isChecked = state.notificationsEnabled
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
            if (isChecked && Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}