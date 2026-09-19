package com.zhiend.regretnote.insight

import com.zhiend.regretnote.data.Category
import com.zhiend.regretnote.data.CategoryCount
import com.zhiend.regretnote.data.MonthCountRow
import com.zhiend.regretnote.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The insight engine is the one part of the app that is pure arithmetic and
 * string building, so it gets the real tests: everything else needs a database
 * or a device.
 */
class InsightEngineTest {

    private val today = "2026-09-18"

    // --- Streak --------------------------------------------------------------

    @Test
    fun `streak counts consecutive days ending today`() {
        val dates = listOf("2026-09-18", "2026-09-17", "2026-09-16")
        assertEquals(3, InsightEngine.streak(dates, today))
    }

    @Test
    fun `streak survives a gap`() {
        val dates = listOf("2026-09-18", "2026-09-17", "2026-09-14", "2026-09-13")
        assertEquals(2, InsightEngine.streak(dates, today))
    }

    @Test
    fun `streak still counts when today is not written yet`() {
        val dates = listOf("2026-09-17", "2026-09-16", "2026-09-15")
        assertEquals(3, InsightEngine.streak(dates, today))
    }

    @Test
    fun `streak is zero when yesterday and today are both blank`() {
        assertEquals(0, InsightEngine.streak(listOf("2026-09-10"), today))
        assertEquals(0, InsightEngine.streak(emptyList(), today))
    }

    @Test
    fun `several notes in one day count as one day of streak`() {
        // The UI passes distinct dates, but the engine must not be fooled if it
        // ever receives repeats: days, not notes.
        val dates = listOf("2026-09-18", "2026-09-18", "2026-09-18", "2026-09-17")
        assertEquals(2, InsightEngine.streak(dates, today))
    }

    @Test
    fun `streak crosses a month boundary`() {
        val dates = listOf("2026-09-01", "2026-08-31", "2026-08-30")
        assertEquals(3, InsightEngine.streak(dates, "2026-09-01"))
    }

    // --- Category percentages ------------------------------------------------

    @Test
    fun `percentages are rounded shares of the total`() {
        val stats = InsightEngine.categoryStats(
            listOf(
                CategoryCount(Category.HEALTH.name, 2),
                CategoryCount(Category.MONEY.name, 1),
                CategoryCount(Category.TIME.name, 1),
            ),
        )
        assertEquals(4, stats.sumOf { it.count })
        assertEquals(50, stats.first().percentage)
        assertEquals(Category.HEALTH, stats.first().category)
    }

    @Test
    fun `ties are ordered deterministically`() {
        val stats = InsightEngine.categoryStats(
            listOf(
                CategoryCount(Category.WORK.name, 1),
                CategoryCount(Category.HEALTH.name, 1),
            ),
        )
        // Same count twice: order must not depend on the query.
        assertEquals(listOf(Category.HEALTH, Category.WORK), stats.map { it.category })
    }

    @Test
    fun `unknown category names fall back to other`() {
        val stats = InsightEngine.categoryStats(listOf(CategoryCount("SOMETHING_OLD", 3)))
        assertEquals(Category.OTHER, stats.single().category)
    }

    @Test
    fun `no counts means no stats`() {
        assertTrue(InsightEngine.categoryStats(emptyList()).isEmpty())
    }

    // --- Monthly trend -------------------------------------------------------

    @Test
    fun `trend has the requested months, oldest first, including empty ones`() {
        val rows = listOf(
            MonthCountRow("2026-07", 28),
            MonthCountRow("2026-09", 14),
            MonthCountRow("2020-01", 999), // far outside the window
        )
        val trend = InsightEngine.monthTrend(rows, today, months = 6)
        assertEquals(6, trend.size)
        assertEquals(listOf("Apr", "May", "Jun", "Jul", "Aug", "Sep"), trend.map { it.label })
        assertEquals(listOf(0, 0, 0, 28, 0, 14), trend.map { it.count })
    }

    @Test
    fun `trend walks back across a year boundary`() {
        val trend = InsightEngine.monthTrend(emptyList(), "2026-02-10", months = 4)
        assertEquals(listOf("Nov", "Dec", "Jan", "Feb"), trend.map { it.label })
    }

    // --- Reports -------------------------------------------------------------

    @Test
    fun `report names the top category and writes a sentence`() {
        val stats = InsightEngine.categoryStats(
            listOf(
                CategoryCount(Category.HEALTH.name, 3),
                CategoryCount(Category.WORK.name, 1),
            ),
        )
        val report = InsightEngine.report(InsightRange.THIS_WEEK, stats, totalEntries = 4, daysCovered = 7)
        assertEquals(Category.HEALTH, report.topCategory)
        assertEquals(75, report.topPercentage)
        assertTrue(report.sentence.contains("the last 7 days"))
        assertEquals("What is one tiny habit you could restart tomorrow?", report.followUp)
    }

    @Test
    fun `empty range says so instead of inventing an insight`() {
        val report = InsightEngine.report(InsightRange.THIS_WEEK, emptyList(), totalEntries = 0, daysCovered = 7)
        assertNull(report.topCategory)
        assertNull(report.followUp)
        assertEquals("No check-ins yet in this range.", report.sentence)
    }

    @Test
    fun `all time phrasing differs from the weekly one`() {
        val stats = InsightEngine.categoryStats(listOf(CategoryCount(Category.TIME.name, 4)))
        val report = InsightEngine.report(InsightRange.ALL_TIME, stats, totalEntries = 4, daysCovered = 900)
        assertTrue(report.sentence.contains("your journal so far"))
        assertEquals(900, report.daysCovered)
    }

    @Test
    fun `week start is six days before today`() {
        assertEquals("2026-09-12", InsightEngine.weekStart(today))
    }

    // --- Resurfaced memories --------------------------------------------------

    @Test
    fun `a memory needs history to come from`() {
        assertNull(InsightEngine.anniversary(emptyList(), today))
        // Everything written inside the last fortnight is not a memory yet.
        assertNull(InsightEngine.anniversary(listOf("2026-09-18", "2026-09-14", "2026-09-12"), today))
    }

    @Test
    fun `a year ago is found exactly`() {
        val memory = InsightEngine.anniversary(listOf("2025-09-18", "2026-09-17"), today)
        assertEquals("2025-09-18", memory?.date)
        assertEquals("A year ago tonight", memory?.label)
    }

    @Test
    fun `a near miss inside the window still counts`() {
        // Nothing written on the 18th last year; the 16th is three days off and
        // must still be offered, otherwise most nights would show nothing.
        val memory = InsightEngine.anniversary(listOf("2025-09-16"), today)
        assertEquals("2025-09-16", memory?.date)
        assertEquals("A year ago tonight", memory?.label)
    }

    @Test
    fun `a miss outside the window is not a memory of that anniversary`() {
        // 4 Sep 2025 is a fortnight away from 18 Sep 2025 — too far to call it
        // "a year ago tonight".
        assertNull(InsightEngine.anniversary(listOf("2025-09-04"), today))
    }

    @Test
    fun `the nearest day in the window wins`() {
        val memory = InsightEngine.anniversary(listOf("2025-09-19", "2025-09-15"), today)
        assertEquals("2025-09-19", memory?.date)
    }

    @Test
    fun `shorter distances fill in when the long ones are empty`() {
        val memory = InsightEngine.anniversary(listOf("2026-08-19"), today)
        assertEquals("A month ago tonight", memory?.label)
        assertEquals("2026-08-19", memory?.date)
    }

    @Test
    fun `with several eras available the same day always picks the same one`() {
        val dates = listOf("2025-09-18", "2024-09-18", "2021-09-18", "2016-09-18", "2026-08-18")
        val first = InsightEngine.anniversary(dates, today)
        repeat(5) { assertEquals(first, InsightEngine.anniversary(dates, today)) }
        assertTrue(first != null)
    }

    @Test
    fun `decades of history surface more than one era over time`() {
        // The point of the archive: it should not show the same memory for a
        // month of evenings. Walk September and collect what it serves up.
        val dates = listOf("2025-09-18", "2024-09-18", "2021-09-18", "2016-09-18", "2008-09-18")
        val labels = (1..28).mapNotNull { day ->
            val date = "2026-09-%02d".format(day)
            InsightEngine.anniversary(dates, date)?.label
        }
        assertTrue("expected several eras, got $labels", labels.distinct().size >= 3)
    }

    @Test
    fun `the oldest era does not crowd out the recent ones`() {
        // Five years of notes will contain every milestone; the pick must still
        // reach the one-year anniversary on some evenings.
        val dates = (0..365 * 6).map { Dates.addDays(today, -it) }.toSet()
        val labels = (0..60).mapNotNull { offset ->
            InsightEngine.anniversary(dates, Dates.addDays(today, -offset))?.label
        }.toSet()
        assertTrue("expected the one-year era in $labels", labels.contains("A year ago tonight"))
    }
}
