package com.zhiend.regretnote.data

import com.zhiend.regretnote.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for notes and settings.
 *
 * The repository never exposes "all entries". Screens ask for what they can
 * afford to hold: a page, an aggregate, or the list of dates. [changes] is the
 * one escape hatch — a counter that ticks after every write, so a screen that
 * accumulated pages (the timeline) can refresh exactly the window it is showing
 * instead of waiting for a full-table Flow to re-emit.
 */
class UndoRepository(
    private val dao: EntryDao,
    private val settingsStore: SettingsStore,
) {
    val settings: StateFlow<Settings> = settingsStore.settings

    private val _changes = MutableStateFlow(0L)

    /** Ticks once per write operation (a batch seed counts as one). */
    val changes: StateFlow<Long> = _changes.asStateFlow()

    private fun signalChange() {
        _changes.value += 1
    }

    // --- Reads ---------------------------------------------------------------

    fun observeNotesOn(date: String): Flow<List<Entry>> = dao.observeByDate(date)

    fun observeDates(): Flow<List<String>> = dao.observeDates()

    fun observeCount(): Flow<Int> = dao.observeCount()

    fun observeFirstDate(): Flow<String?> = dao.observeFirstDate()

    fun observeCategoryCounts(fromDate: String, toDate: String): Flow<List<CategoryCount>> =
        dao.observeCategoryCounts(fromDate, toDate)

    fun observeCountInRange(fromDate: String, toDate: String): Flow<Int> =
        dao.observeCountInRange(fromDate, toDate)

    fun observeMonthlyCounts(): Flow<List<MonthCountRow>> = dao.observeMonthlyCounts()

    /**
     * One page of the journal, newest first, no further back than [ceiling]
     * ([Dates.MAX_ISO] for "the whole journal"). Picks the query that can actually
     * use the `(date, createdAt)` index — see the note in [EntryDao].
     */
    suspend fun page(
        query: String,
        category: String,
        ceiling: String,
        limit: Int,
        offset: Int,
    ): List<Entry> = when {
        query.isEmpty() && category.isEmpty() -> dao.pageAll(ceiling, limit, offset)
        query.isEmpty() -> dao.pageByCategory(category, ceiling, limit, offset)
        else -> dao.pageMatching(query, category, ceiling, limit, offset)
    }

    suspend fun countMatching(query: String, category: String, ceiling: String): Int = when {
        query.isEmpty() && category.isEmpty() -> dao.countUpTo(ceiling)
        query.isEmpty() -> dao.countByCategoryUpTo(category, ceiling)
        else -> dao.countMatchingUpTo(query, category, ceiling)
    }

    suspend fun totalNotes(): Int = dao.countAll()

    /** Every note for one day, newest first — used by the resurfaced memory. */
    suspend fun notesOn(date: String): List<Entry> = dao.notesOn(date)

    suspend fun hasNotesOn(date: String): Boolean = dao.countForDate(date) > 0

    /** Dates that already hold at least one note (used by the sample seeder). */
    suspend fun datesWithNotes(): Set<String> = dao.distinctDates().toSet()

    // --- Writes --------------------------------------------------------------

    /** Appends a note for [date] and returns its new id. */
    suspend fun addNote(
        date: String,
        text: String,
        category: Category,
        intensity: Int,
        createdAt: Long = System.currentTimeMillis(),
    ): Long {
        val id = dao.insert(
            Entry(
                date = date,
                text = text,
                category = category.name,
                intensity = intensity,
                createdAt = createdAt,
            ),
        )
        signalChange()
        return id
    }

    /** Re-inserts a deleted note (the undo action), keeping its id and time. */
    suspend fun restore(entry: Entry) {
        dao.insert(entry)
        signalChange()
    }

    suspend fun updateNote(entry: Entry) {
        dao.update(entry)
        signalChange()
    }

    suspend fun deleteNote(id: Long) {
        dao.deleteById(id)
        signalChange()
    }

    /** Bulk insert (the sample seeder) — one change signal for the whole batch. */
    suspend fun addNotes(entries: List<Entry>) {
        if (entries.isEmpty()) return
        dao.insertAll(entries)
        signalChange()
    }

    /** Removes every note; returns how many were deleted. */
    suspend fun deleteAllNotes(): Int {
        val removed = dao.deleteAll()
        if (removed > 0) signalChange()
        return removed
    }

    suspend fun allForExport(): List<Entry> = dao.allForExport()

    /**
     * Identity of every note already in the journal, so importing a file twice
     * appends nothing the second time. See [JournalCsv.key].
     */
    suspend fun existingKeys(): Set<String> =
        dao.allKeys().mapTo(HashSet()) { JournalCsv.key(it.date, it.createdAt) }

    fun updateSettings(transform: (Settings) -> Settings) {
        settingsStore.update(transform)
    }
}
