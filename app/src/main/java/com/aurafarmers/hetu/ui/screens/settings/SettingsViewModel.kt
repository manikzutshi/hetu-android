package com.aurafarmers.hetu.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurafarmers.hetu.data.local.preferences.HetuPreferences
import com.aurafarmers.hetu.data.local.preferences.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: HetuPreferences
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
    val notificationFrequency: StateFlow<String> = preferences.notificationFrequency
    val notificationPersonality: StateFlow<String> = preferences.notificationPersonality

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setNotificationFrequency(frequency: String) {
        viewModelScope.launch {
            preferences.setNotificationFrequency(frequency)
        }
    }

    fun setNotificationPersonality(personality: String) {
        viewModelScope.launch {
            preferences.setNotificationPersonality(personality)
        }
    }
}
