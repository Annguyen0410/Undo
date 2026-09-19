package com.zhiend.regretnote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// NOTE ON THE FOUR PAGE QUERIES
// SQLite cannot prove that `(:query = '' OR text LIKE ...)` is a no-op for bound
// parameters, so a single clever query would plan as a scan-and-sort for *every*
// page. The three cases that can use the index therefore get their own query, and
// only a real text search falls back to a LIKE scan (which is what searching is).

/** Raw `GROUP BY category` projection — see [EntryDao.observeCategoryCounts]. */
data class CategoryCount(
    val category: String,
    val count: Int,
)

/** Raw `GROUP BY month` projection — see [EntryDao.observeMonthlyCounts]. */
data class MonthCountRow(
    val yearMonth: String,
    val count: Int,
)

/**
 * Queries are split into three groups on purpose, because the journal is
 * supposed to hold decades of notes:
 *
 *  1. **Pages** — the timeline asks for 50 rows at a time and never for "all".
 *  2. **Aggregates** — insights get counts from SQL (`GROUP BY`), so a 50k-note
 *     journal costs the same as a 50-note one.
 *  3. **Cheap lists** — distinct dates (one short string per day) for the streak.
 *
 * Only the CSV export reads every row, and that is an explicit user action.
 *
 * Empty-string and sentinel-date arguments stand in for "no filter" / "all time"
 * so no parameter is nullable and every query keeps using the index.
 */
@Dao
interface EntryDao {

    // --- Today ---------------------------------------------------------------

    /** Every note for one day, newest first. */
    @Query("SELECT * FROM entries WHERE date = :date ORDER BY createdAt DESC, id DESC")
    fun observeByDate(date: String): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date = :date ORDER BY createdAt DESC, id DESC")
    suspend fun notesOn(date: String): List<Entry>

    @Query("SELECT COUNT(*) FROM entries WHERE date = :date")
    suspend fun countForDate(date: String): Int

    /** Distinct dates that hold at least one note, newest first. */
    @Query("SELECT date FROM entries GROUP BY date ORDER BY date DESC")
    fun observeDates(): Flow<List<String>>

    /** The same list, for one-off suspend reads (the sample seeder). */
    @Query("SELECT date FROM entries GROUP BY date")
    suspend fun distinctDates(): List<String>

    // --- Pages ---------------------------------------------------------------

    /**
     * One page of the journal, newest first, going back no further than
     * [ceiling] (`"9999-12-31"` when the reader has asked for no limit).
     *
     * The ceiling is what makes twenty years of notes navigable: the journal can
     * be pointed at any month and every query stays a bounded range scan on the
     * `(date, createdAt)` index — it is also why the date range is a plain bound
     * on the leading index column rather than an `OR` the planner cannot fold.
     */
    /** Unfiltered browse — the common path, straight down the index. */
    @Query(
        """
        SELECT * FROM entries WHERE date <= :ceiling
        ORDER BY date DESC, createdAt DESC, id DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun pageAll(ceiling: String, limit: Int, offset: Int): List<Entry>

    @Query(
        """
        SELECT * FROM entries WHERE category = :category AND date <= :ceiling
        ORDER BY date DESC, createdAt DESC, id DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun pageByCategory(category: String, ceiling: String, limit: Int, offset: Int): List<Entry>

    /** Text search (optionally narrowed to a category). This one scans; that is what search costs. */
    @Query(
        """
        SELECT * FROM entries
        WHERE text LIKE '%' || :query || '%'
          AND (:category = '' OR category = :category)
          AND date <= :ceiling
        ORDER BY date DESC, createdAt DESC, id DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun pageMatching(query: String, category: String, ceiling: String, limit: Int, offset: Int): List<Entry>

    @Query("SELECT COUNT(*) FROM entries WHERE date <= :ceiling")
    suspend fun countUpTo(ceiling: String): Int

    @Query("SELECT COUNT(*) FROM entries WHERE category = :category AND date <= :ceiling")
    suspend fun countByCategoryUpTo(category: String, ceiling: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM entries
        WHERE text LIKE '%' || :query || '%'
          AND (:category = '' OR category = :category)
          AND date <= :ceiling
        """,
    )
    suspend fun countMatchingUpTo(query: String, category: String, ceiling: String): Int

    /** Total rows in the journal, whatever the reader is currently looking at. */
    @Query("SELECT COUNT(*) FROM entries")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM entries")
    fun observeCount(): Flow<Int>

    // --- Aggregates ----------------------------------------------------------

    @Query(
        """
        SELECT category AS category, COUNT(*) AS count FROM entries
        WHERE date >= :fromDate AND date <= :toDate
        GROUP BY category
        """,
    )
    fun observeCategoryCounts(fromDate: String, toDate: String): Flow<List<CategoryCount>>

    @Query("SELECT COUNT(*) FROM entries WHERE date >= :fromDate AND date <= :toDate")
    fun observeCountInRange(fromDate: String, toDate: String): Flow<Int>

    @Query(
        """
        SELECT substr(date, 1, 7) AS yearMonth, COUNT(*) AS count FROM entries
        GROUP BY yearMonth ORDER BY yearMonth
        """,
    )
    fun observeMonthlyCounts(): Flow<List<MonthCountRow>>

    /** ISO date of the oldest note, or null when the journal is empty. */
    @Query("SELECT MIN(date) FROM entries")
    fun observeFirstDate(): Flow<String?>

    // --- Writes --------------------------------------------------------------

    @Insert
    suspend fun insert(entry: Entry): Long

    @Insert
    suspend fun insertAll(entries: List<Entry>)

    @Update
    suspend fun update(entry: Entry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Returns how many notes were removed. */
    @Query("DELETE FROM entries")
    suspend fun deleteAll(): Int

    // --- Export / import ------------------------------------------------------

    @Query("SELECT * FROM entries ORDER BY date ASC, createdAt ASC")
    suspend fun allForExport(): List<Entry>

    /**
     * Just the two columns that identify a note, for de-duplicating an import.
     *
     * Deliberately not `SELECT *`: the point of the import is to compare against
     * what is already here, and there is no reason to pull decades of prose back
     * out of SQLite to do that.
     */
    @Query("SELECT date AS date, createdAt AS createdAt FROM entries")
    suspend fun allKeys(): List<EntryKey>
}

/** Projection for [EntryDao.allKeys]. */
data class EntryKey(
    val date: String,
    val createdAt: Long,
)
