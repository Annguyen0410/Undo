package com.zhiend.regretnote.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A note that has been started but not saved.
 *
 * [date] matters: a draft belongs to the evening it was written for, so a draft
 * left open overnight is recognised as yesterday's and is not silently offered
 * as today's answer to tonight's question.
 */
data class Draft(
    val date: String,
    val text: String,
    val category: String?,
    val intensity: Int,
)

/**
 * Keeps the composer's text on disk.
 *
 * A journal is the one app where losing half a paragraph is unforgivable, and
 * `rememberSaveable` only survives a configuration change — not the process being
 * killed while the phone is in a pocket. There is no per-day limit on notes and
 * no limit on how long the journal gets, so there is exactly one draft slot: the
 * note currently being written.
 *
 * Stored in its own preferences file rather than in [SettingsStore] on purpose —
 * a `StateFlow<Settings>` that re-emits on every keystroke would recompose every
 * screen that observes a setting.
 */
class DraftStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _draft = MutableStateFlow(load())
    val draft: StateFlow<Draft?> = _draft.asStateFlow()

    /** Called on every keystroke. Blank drafts are dropped, not stored. */
    fun save(date: String, text: String, category: String?, intensity: Int) {
        if (text.isBlank()) {
            clear()
            return
        }
        val next = Draft(date = date, text = text, category = category, intensity = intensity)
        if (next == _draft.value) return
        prefs.edit()
            .putString(KEY_DATE, next.date)
            .putString(KEY_TEXT, next.text)
            .putString(KEY_CATEGORY, next.category)
            .putInt(KEY_INTENSITY, next.intensity)
            .apply()
        _draft.value = next
    }

    fun clear() {
        if (_draft.value == null) return
        prefs.edit().clear().apply()
        _draft.value = null
    }

    private fun load(): Draft? {
        val text = prefs.getString(KEY_TEXT, null) ?: return null
        if (text.isBlank()) return null
        return Draft(
            date = prefs.getString(KEY_DATE, null) ?: return null,
            text = text,
            category = prefs.getString(KEY_CATEGORY, null),
            intensity = prefs.getInt(KEY_INTENSITY, 2),
        )
    }

    private companion object {
        const val PREFS_NAME = "undo_draft"
        const val KEY_DATE = "date"
        const val KEY_TEXT = "text"
        const val KEY_CATEGORY = "category"
        const val KEY_INTENSITY = "intensity"
    }
}
