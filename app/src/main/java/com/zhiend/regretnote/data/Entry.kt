package com.zhiend.regretnote.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One note in the journal.
 *
 * There is deliberately no "one entry per day" rule and no cap anywhere: the
 * journal is meant to hold decades of notes, several a day if you feel like it.
 * Everything that reads entries therefore either
 *  - reads a *page* of them (the timeline), or
 *  - reads SQL aggregates (insights), or
 *  - reads only the distinct dates (streak),
 * and never materialises the whole table.
 *
 * The `(date, createdAt)` index backs both the timeline's
 * `ORDER BY date DESC, createdAt DESC` scan and the per-day lookups.
 */
@Entity(
    tableName = "entries",
    indices = [Index(value = ["date", "createdAt"])],
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** ISO local date the note belongs to, e.g. "2026-08-31". */
    val date: String,
    /** The free-text note. */
    val text: String,
    /** Stored as [Category.name]. */
    val category: String,
    /** Regret intensity from 1 (light) to 3 (heavy). */
    val intensity: Int,
    /** Epoch millis when the note was written — also the within-day ordering. */
    val createdAt: Long,
)
