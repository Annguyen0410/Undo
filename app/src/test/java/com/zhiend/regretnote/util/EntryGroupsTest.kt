package com.zhiend.regretnote.util

import com.zhiend.regretnote.data.Category
import com.zhiend.regretnote.data.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The journal loads 50 notes at a time, so a day can be split across two pages.
 * Grouping happens on the accumulated list, which is exactly why this needs a
 * test: the visible symptom of getting it wrong is one day rendering twice.
 */
class EntryGroupsTest {

    private fun note(id: Long, date: String, text: String = "note $id") = Entry(
        id = id,
        date = date,
        text = text,
        category = Category.WORK.name,
        intensity = 2,
        createdAt = id,
    )

    @Test
    fun `notes of the same day end up in one group`() {
        val groups = listOf(
            note(3, "2026-09-18"),
            note(2, "2026-09-18"),
            note(1, "2026-09-17"),
        ).groupedByDay()

        assertEquals(listOf("2026-09-18", "2026-09-17"), groups.map { it.date })
        assertEquals(listOf(2, 1), groups.map { it.notes.size })
    }

    @Test
    fun `a day split across two pages stays one group`() {
        val pageOne = listOf(note(5, "2026-09-18"), note(4, "2026-09-18"))
        val pageTwo = listOf(note(3, "2026-09-18"), note(2, "2026-09-17"))

        val groups = (pageOne + pageTwo).groupedByDay()

        assertEquals(listOf("2026-09-18", "2026-09-17"), groups.map { it.date })
        assertEquals(3, groups.first().notes.size)
        // Order inside the day is the query's order, untouched.
        assertEquals(listOf(5L, 4L, 3L), groups.first().notes.map { it.id })
    }

    @Test
    fun `group order follows the newest-first query`() {
        val groups = listOf(
            note(4, "2026-09-18"),
            note(3, "2026-09-16"),
            note(2, "2026-09-16"),
            note(1, "2026-08-31"),
        ).groupedByDay()
        assertEquals(listOf("2026-09-18", "2026-09-16", "2026-08-31"), groups.map { it.date })
    }

    @Test
    fun `no notes means no groups`() {
        assertTrue(emptyList<Entry>().groupedByDay().isEmpty())
    }
}
