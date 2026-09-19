package com.zhiend.regretnote.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhiend.regretnote.UndoApplication
import com.zhiend.regretnote.data.Category
import com.zhiend.regretnote.data.Draft
import com.zhiend.regretnote.data.Entry
import com.zhiend.regretnote.insight.InsightEngine
import com.zhiend.regretnote.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A night from the archive, resurfaced on the home screen. */
data class Memory(
    val date: String,
    val label: String,
    /** Every note that evening, newest first. */
    val notes: List<Entry>,
) {
    /** The most recent note of that night — the one worth showing. */
    val lead: Entry get() = notes.first()

    val extraNotes: Int get() = notes.size - 1
}

/**
 * Check-in (home) screen state.
 *
 * A day holds as many notes as you want, so the screen shows *today's notes* (a
 * short list, they are all from today) rather than a single entry, and the day it
 * points at can be refreshed — leaving the app open past midnight should not
 * write tomorrow's notes into yesterday.
 *
 * Two things here exist because the journal is meant to last for decades: the
 * composer keeps a draft on disk, and the screen serves one note from years ago
 * back to you. Both are computed from the cheap list of written *dates*, so
 * neither of them loads history.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as UndoApplication).container.repository

    private val draftStore = (application as UndoApplication).container.draftStore

    private val day = MutableStateFlow(Dates.today())

    /**
     * Today's notes, newest first — or `null` while they have not been read out of
     * the database yet.
     *
     * The distinction matters and is not pedantry: "not loaded" and "empty" look
     * identical to a screen that only sees a list, and the screen uses emptiness to
     * decide whether to open the composer. Treating the first frame as "today is
     * blank" is what quietly consumed a restored draft on launch, and it also made
     * a journal with notes in it flash the composer before the list appeared.
     */
    val notes: StateFlow<List<Entry>?> = day
        .flatMapLatest { repository.observeNotesOn(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val streak: StateFlow<Int> = combine(day, repository.observeDates()) { date, dates ->
        InsightEngine.streak(dates, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * One evening from the past, chosen by [InsightEngine.anniversary]. Only the
     * distinct dates are read to choose it, and only the chosen day's notes are
     * then loaded — a twenty-year journal costs one small extra query.
     */
    val memory: StateFlow<Memory?> = day
        .flatMapLatest { date ->
            repository.observeDates()
                .map { InsightEngine.anniversary(it, date) }
                .distinctUntilChanged()
                .flatMapLatest { anniversary ->
                    if (anniversary == null) {
                        flowOf<Memory?>(null)
                    } else {
                        flow { emit(Memory(anniversary.date, anniversary.label, repository.notesOn(anniversary.date))) }
                    }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The note being written, if the app was killed before it was saved. */
    val draft: StateFlow<Draft?> = draftStore.draft

    private val _recentlyDeleted = MutableStateFlow<Entry?>(null)

    /** The last deleted note, so the screen can offer an undo. */
    val recentlyDeleted: StateFlow<Entry?> = _recentlyDeleted.asStateFlow()

    /** Re-reads the calendar; called when the screen comes back to the foreground. */
    fun refreshDay() {
        day.value = Dates.today()
    }

    fun addNote(text: String, category: Category, intensity: Int) {
        viewModelScope.launch {
            repository.addNote(
                date = day.value,
                text = text.trim(),
                category = category,
                intensity = intensity,
            )
            draftStore.clear()
        }
    }

    fun updateNote(entry: Entry, text: String, category: Category, intensity: Int) {
        viewModelScope.launch {
            repository.updateNote(
                entry.copy(
                    text = text.trim(),
                    category = category.name,
                    intensity = intensity,
                ),
            )
        }
    }

    fun deleteNote(entry: Entry) {
        viewModelScope.launch {
            repository.deleteNote(entry.id)
            _recentlyDeleted.value = entry
        }
    }

    /** Puts the last deleted note back, id and timestamp intact. */
    fun undoDelete() {
        val entry = _recentlyDeleted.value ?: return
        _recentlyDeleted.value = null
        viewModelScope.launch { repository.restore(entry) }
    }

    fun dismissUndo() {
        _recentlyDeleted.value = null
    }

    /** Keeps the half-written note on disk, so leaving the app costs nothing. */
    fun saveDraft(text: String, category: Category?, intensity: Int) {
        draftStore.save(date = day.value, text = text, category = category?.name, intensity = intensity)
    }

    /** Throws the half-written note away. */
    fun discardDraft() {
        draftStore.clear()
    }
}
