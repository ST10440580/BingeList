package com.example.bingelist.ui.settings

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import com.example.bingelist.notifications.NotificationHelper
import com.example.bingelist.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val selectedLanguage: String = "English",
    val notificationsEnabled: Boolean = true,
    val availableLanguages: List<String> = listOf("English", "isiZulu", "Sepedi", "Español")
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("binge_list_prefs", Context.MODE_PRIVATE)

    private val languageMap = mapOf(
        "en" to "English",
        "zu" to "isiZulu",
        "nso" to "Sepedi",
        "es" to "Español"
    )

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            selectedLanguage = getCurrentLanguageName(),
            notificationsEnabled = prefs.getBoolean("notifications", true)
        )
    )

    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun getCurrentLanguageName(): String {
        val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"
        return languageMap[currentLocale] ?: "English"
    }

    fun selectLanguage(languageName: String) {
        val localeCode = languageMap.entries.find { it.value == languageName }?.key ?: "en"
        
        // 1. Apply language globally using AppCompatDelegate
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(localeCode)
        AppCompatDelegate.setApplicationLocales(appLocale)

        // 2. Persist in state and prefs
        prefs.edit().putString("language", languageName).apply()
        _uiState.value = _uiState.value.copy(selectedLanguage = languageName)
    }

    fun toggleNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("notifications", enabled).apply()
        NotificationHelper.applyTopicSubscription(enabled)
        if (enabled) NotificationScheduler.runNow(getApplication<Application>())
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }
}