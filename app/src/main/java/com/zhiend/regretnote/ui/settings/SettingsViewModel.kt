package com.zhiend.regretnote.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhiend.regretnote.UndoApplication
import com.zhiend.regretnote.data.Entry
import com.zhiend.regretnote.data.JournalCsv
import com.zhiend.regretnote.data.Settings
import com.zhiend.regretnote.devtools.SampleJournal
import com.zhiend.regretnote.notification.ReminderScheduler
import com.zhiend.regretnote.purchase.RevenueCatManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Settings screen state + actions (reminder, theme, export). */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as UndoApplication).container.repository

    val settings: StateFlow<Settings> = repository.settings

    val isPremium: StateFlow<Boolean> = RevenueCatManager.isPremium

    /** Size and start of the archive — the journal is built to hold decades, so
     * it should be able to say how much it is holding. Both are aggregates, not
     * the notes themselves. */
    val totalNotes: StateFlow<Int> = repository.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val firstDate: StateFlow<String?> = repository.observeFirstDate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateSettings(transform: (Settings) -> Settings) {
        val before = settings.value
        val after = transform(before)
        if (after == before) return
        repository.updateSettings { after }
        if (before.reminderEnabled != after.reminderEnabled) {
            if (after.reminderEnabled) {
                ReminderScheduler.schedule(getApplication(), after.reminderHour, after.reminderMinute)
            } else {
                ReminderScheduler.cancel(getApplication())
            }
        } else if (after.reminderEnabled &&
            (before.reminderHour != after.reminderHour || before.reminderMinute != after.reminderMinute)
        ) {
            ReminderScheduler.schedule(getApplication(), after.reminderHour, after.reminderMinute)
        }
    }

    /**
     * Debug-only: back-fills the journal with ~90 days of sample notes (some days
     * hold two or three, so the multi-note layout is visible in demos). Days that
     * already hold a note are left untouched, so it is safe to run more than once
     * and it never overwrites anything you wrote yourself.
     */
    fun seedSampleJournal(onDone: (Int) -> Unit) {
        seed(onDone) { taken -> SampleJournal.recentDays().filter { it.date !in taken } }
    }

    /**
     * Debug-only: adds ~20 years of history (~8,700 notes) so the paging, the
     * aggregates and the search can be judged on a journal the size this app is
     * actually meant to hold.
     */
    fun seedDecades(years: Int = 20, onDone: (Int) -> Unit) {
        // Decades alone stop ninety days short, so that the two seeders can never
        // argue over the same evening. A single tap should still leave a complete
        // looking journal, so the recent window rides along.
        seed(onDone) { taken ->
            (SampleJournal.decades(years) + SampleJournal.recentDays()).filter { it.date !in taken }
        }
    }

    /**
     * Both seeders: read which days are already written, generate off the main
     * thread (twenty years is ~8,700 notes to build), then insert them in one
     * transaction so the app emits a single change signal instead of thousands.
     */
    private fun seed(onDone: (Int) -> Unit, generate: (Set<String>) -> List<Entry>) {
        viewModelScope.launch {
            val taken = repository.datesWithNotes()
            val fresh = withContext(Dispatchers.Default) { generate(taken) }
            repository.addNotes(fresh)
            onDone(fresh.size)
        }
    }

    /** Debug-only: removes every note — sample data and your own alike. */
    fun clearJournal(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            onDone(repository.deleteAllNotes())
        }
    }

    /**
     * Writes every entry as CSV to [uri] (picked via SAF).
     *
     * The file is written one row at a time straight into the stream, so a
     * decades-long journal never has to exist as one giant string. The row format
     * itself lives in [JournalCsv], which is the same code [importCsv] reads back.
     */
    fun exportCsv(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching {
                val entries = repository.allForExport()
                val output = getApplication<Application>().contentResolver.openOutputStream(uri)
                    ?: error("Could not open $uri for writing")
                output.bufferedWriter().use { writer ->
                    writer.appendLine(JournalCsv.HEADER)
                    entries.forEach { writer.appendLine(JournalCsv.row(it)) }
                }
            }.isSuccess
            onDone(ok)
        }
    }

    /** How an import went — enough to tell the user the truth about what happened. */
    data class ImportOutcome(val added: Int, val duplicates: Int, val skipped: Int)

    /**
     * Reads a journal CSV back in (picked via SAF).
     *
     * Two things make this safe to run against a file the user may have edited by
     * hand: notes already in the journal are skipped rather than appended twice, and
     * the whole file goes in as one transaction so a large import emits a single
     * change signal instead of one per note.
     *
     * [onDone] receives `null` when the file could not be read or written at all,
     * which is a different failure from "read it fine, recognised nothing in it".
     */
    fun importCsv(uri: Uri, onDone: (ImportOutcome?) -> Unit) {
        viewModelScope.launch {
            val outcome = runCatching {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                } ?: error("Could not read $uri")

                val parsed = withContext(Dispatchers.Default) { JournalCsv.parse(text) }

                // `seen` starts as everything already in the journal and absorbs each
                // accepted row, so one pass de-duplicates against the journal *and*
                // against the rest of the file.
                val seen = repository.existingKeys().toMutableSet()
                val fresh = parsed.entries.filter {
                    seen.add(JournalCsv.key(it.date, it.createdAt))
                }

                repository.addNotes(fresh)
                ImportOutcome(
                    added = fresh.size,
                    duplicates = parsed.entries.size - fresh.size,
                    skipped = parsed.skipped,
                )
            }.getOrNull()
            onDone(outcome)
        }
    }
}
