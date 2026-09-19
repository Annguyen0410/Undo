package com.zhiend.regretnote.ui.timeline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhiend.regretnote.UndoApplication
import com.zhiend.regretnote.data.Category
import com.zhiend.regretnote.data.Entry
import com.zhiend.regretnote.util.Dates
import com.zhiend.regretnote.util.TIMELINE_PAGE_SIZE
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One entry in the \"jump to month\" list. */
data class MonthOption(
    /** `\"2006-08\"` bucket key. */
    val yearMonth: String,
    /** `\"Aug 2006\"` for the dialog. */
    val label: String,
    val count: Int,
)

/** What the journal screen is showing right now. */
data class JournalState(
    /** The notes loaded so far, newest first. Grows as you scroll. */
    val notes: List<Entry> = emptyList(),
    /** Notes matching the current filter / search, no older than [ceiling]. */
    val matching: Int = 0,
    /** Notes in the whole journal, whatever the filter or the ceiling is. */
    val total: Int = 0,
    /** ISO date of the oldest note, for the "since" line. */
    val firstDate: String? = null,
    val query: String = "",
    val category: Category? = null,
    /**
     * Nothing older than this is loaded — the journal can be pointed at any month
     * in a twenty-year archive without reading the years in front of it. `null`
     * means "the newest notes", which is also the ceiling's default sentinel.
     */
    val ceiling: String? = null,
    val loading: Boolean = false,
    val hasMore: Boolean = false,
) {
    val filtered: Boolean get() = query.isNotBlank() || category != null

    /** True while the reader is looking at an older window rather than the top. */
    val jumped: Boolean get() = ceiling != null

    /** True once the first page has come back and produced nothing. */
    val empty: Boolean get() = notes.isEmpty() && !loading
}

/**
 * Journal state: pages, not the whole table.
 *
 * The timeline used to hold every entry in memory through a Room Flow, which is
 * fine for a demo journal and hopeless for the one this app is for — twenty years
 * of daily notes is tens of thousands of rows. So it now keeps a page window:
 * 50 notes at a time, extended as you scroll, refreshed to exactly the window it
 * has loaded whenever something is written elsewhere in the app.
 *
 * Scrolling is not the only way to move, either. With decades of notes, getting
 * to August 2014 by scrolling is not navigation, it is archaeology, so the journal
 * can be pointed at any month that actually holds notes and every query behind it
 * stays an indexed range scan.
 */
class TimelineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as UndoApplication).container.repository

    private val _state = MutableStateFlow(JournalState())
    val state: StateFlow<JournalState> = _state.asStateFlow()

    /** Every month that holds at least one note, newest first. */
    val months: StateFlow<List<MonthOption>> = repository.observeMonthlyCounts()
        .map { rows ->
            rows.asReversed().map { row ->
                MonthOption(
                    yearMonth = row.yearMonth,
                    label = Dates.monthYearLabelOfKey(row.yearMonth),
                    count = row.count,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Current filter, read at load time so a page always matches what is shown. */
    private var filter: Filter = Filter("", "")

    /** Bumped on every fresh load so a late page can tell it is obsolete. */
    private var generation = 0

    private var debounceJob: Job? = null

    private data class Filter(val query: String, val category: String)

    init {
        refresh()
        viewModelScope.launch {
            repository.observeCount().collect { total -> _state.update { it.copy(total = total) } }
        }
        viewModelScope.launch {
            repository.observeFirstDate().collect { first -> _state.update { it.copy(firstDate = first) } }
        }
        // Any write elsewhere (a new note on the home screen, a seeded journal)
        // re-reads the window we are showing, so the list never goes stale.
        viewModelScope.launch {
            repository.changes.drop(1).collect { refresh() }
        }
    }

    fun setQuery(query: String) {
        if (query == filter.query) return
        filter = filter.copy(query = query.trim())
        _state.update { it.copy(query = query) }
        scheduleRefresh()
    }

    fun setCategory(category: Category?) {
        val name = category?.name.orEmpty()
        if (name == filter.category) return
        filter = filter.copy(category = name)
        _state.update { it.copy(category = category) }
        scheduleRefresh()
    }

    fun clearFilters() {
        if (!_state.value.filtered) return
        filter = Filter("", "")
        _state.update { it.copy(query = "", category = null) }
        scheduleRefresh()
    }

    /** Points the journal at one month, skipping straight past everything newer. */
    fun jumpToMonth(yearMonth: String) {
        val ceiling = Dates.monthEndOfKey(yearMonth)
        if (ceiling == _state.value.ceiling) return
        _state.update { it.copy(ceiling = ceiling, notes = emptyList()) }
        refresh()
    }

    /** Points the journal at one day — used when a memory is opened. */
    fun jumpToDate(date: String) {
        if (date == _state.value.ceiling) return
        _state.update { it.copy(ceiling = date, notes = emptyList()) }
        refresh()
    }

    /** Back to the newest notes. */
    fun showNewest() {
        if (_state.value.ceiling == null) return
        _state.update { it.copy(ceiling = null, notes = emptyList()) }
        refresh()
    }

    /** Called when the list nears its end. No-op while a load is in flight. */
    fun loadMore() {
        val current = _state.value
        if (current.loading || !current.hasMore) return
        load(offset = current.notes.size, limit = TIMELINE_PAGE_SIZE)
    }

    fun retry() = refresh()

    /**
     * Re-reads everything currently on screen: the first page, or as many pages
     * as the user has scrolled through. Called on filter changes and on writes.
     */
    private fun refresh() {
        val window = maxOf(TIMELINE_PAGE_SIZE, _state.value.notes.size)
        load(offset = 0, limit = window)
    }

    /** Typing should not fire a query per keystroke. */
    private fun scheduleRefresh() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            refresh()
        }
    }

    private fun load(offset: Int, limit: Int) {
        val requestFilter = filter
        val requestedCeiling = _state.value.ceiling
        val ceiling = requestedCeiling ?: Dates.MAX_ISO
        val request = ++generation
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val page = repository.page(requestFilter.query, requestFilter.category, ceiling, limit, offset)
            val matching = repository.countMatching(requestFilter.query, requestFilter.category, ceiling)
            if (request != generation) return@launch // a newer load already answered
            val merged = if (offset == 0) page else (_state.value.notes + page).distinctBy { it.id }
            _state.update {
                it.copy(
                    notes = merged,
                    matching = matching,
                    loading = false,
                    hasMore = merged.size < matching,
                )
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 200L
    }
}
