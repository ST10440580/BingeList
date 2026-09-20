package com.example.bingelist.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedLanguage: String = "English",
    val notificationsEnabled: Boolean = true,
    val availableLanguages: List<String> = listOf("English", "Afrikaans", "isiZulu", "Español")
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("binge_list_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            selectedLanguage = prefs.getString("language", "English") ?: "English",
            notificationsEnabled = prefs.getBoolean("notifications", true)
        )
    )

    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun selectLanguage(language: String) {
        prefs.edit().putString("language", language).apply()
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun toggleNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("notifications", enabled).apply()
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }
}