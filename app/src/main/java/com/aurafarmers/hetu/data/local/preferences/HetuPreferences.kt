package com.aurafarmers.hetu.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Singleton
class HetuPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hetu_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _notificationFrequency = MutableStateFlow(getNotificationFrequency())
    val notificationFrequency: StateFlow<String> = _notificationFrequency.asStateFlow()

    private val _notificationPersonality = MutableStateFlow(getNotificationPersonality())
    val notificationPersonality: StateFlow<String> = _notificationPersonality.asStateFlow()

    private fun getThemeMode(): ThemeMode {
        val modeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    private fun getNotificationFrequency(): String {
        return prefs.getString("notif_frequency", "Daily") ?: "Daily"
    }

    fun setNotificationFrequency(frequency: String) {
        prefs.edit().putString("notif_frequency", frequency).apply()
        _notificationFrequency.value = frequency
    }

    private fun getNotificationPersonality(): String {
        return prefs.getString("notif_personality", "Friendly") ?: "Friendly"
    }

    fun setNotificationPersonality(personality: String) {
        prefs.edit().putString("notif_personality", personality).apply()
        _notificationPersonality.value = personality
    }
}
