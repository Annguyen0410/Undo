package com.zhiend.regretnote.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User preferences that live outside the single-entry table. */
data class Settings(
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val midnightTheme: Boolean = false,
)

/** Thin SharedPreferences wrapper exposing settings as a [StateFlow]. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, next.reminderEnabled)
            .putInt(KEY_REMINDER_HOUR, next.reminderHour)
            .putInt(KEY_REMINDER_MINUTE, next.reminderMinute)
            .putBoolean(KEY_MIDNIGHT_THEME, next.midnightTheme)
            .apply()
        _settings.value = next
    }

    private fun load(): Settings = Settings(
        reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
        reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 20),
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0),
        midnightTheme = prefs.getBoolean(KEY_MIDNIGHT_THEME, false),
    )

    private companion object {
        const val PREFS_NAME = "undo_settings"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_MIDNIGHT_THEME = "midnight_theme"
    }
}
