package com.zhiend.regretnote.insight

import com.zhiend.regretnote.data.Category
import com.zhiend.regretnote.data.CategoryCount
import com.zhiend.regretnote.data.MonthCountRow
import com.zhiend.regretnote.util.Dates
import kotlin.math.abs

/** What slice of history an insight report covers. */
enum class InsightRange(val label: String) {
    THIS_WEEK("This week"),
    ALL_TIME("All time"),
}

data class CategoryStat(
    val category: Category,
    val count: Int,
    val percentage: Int,
)

data class MonthCount(
    val yearMonth: String,
    val label: String,
    val count: Int,
)

/** One night from the past, chosen to be read back to you this evening. */
data class Anniversary(
    val date: String,
    val label: String,
)

/**
 * A distance worth looking back at, plus how far around it to look.
 *
 * The window is what makes this usable: notes are not written every single day,
 * so waiting for the exact 365th day would usually find nothing. A few days of
 * slack around each anniversary finds a memory almost every night.
 */
private data class Milestone(val days: Int, val label: String, val window: Int)

/**
 * A computed summary used by the Insight screen. No LLM involved: the "smart"
 * sentence is pure aggregation plus a category-specific template question.
 */
data class InsightReport(
    val range: InsightRange,
    val totalEntries: Int,
    val daysCovered: Int,
    val topCategory: Category?,
    val topCount: Int,
    val topPercentage: Int,
    val categoryStats: List<CategoryStat>,
    val monthlyTrend: List<MonthCount>,
    val sentence: String,
    val followUp: String?,
)

/**
 * Turns counts into sentences.
 *
 * Everything here takes *aggregates*, never a list of entries: the journal is
 * meant to hold decades of notes, so the counting happens in SQL and this object
 * only ever sees a handful of numbers. That also makes the whole thing a pure
 * function, which is why it is the one part of the app with real unit tests.
 */
object InsightEngine {

    private const val THIS_WEEK_DAYS = 7
    private const val TREND_MONTHS = 6

    /** Oldest first: with decades of notes, the long distances are the good ones. */
    private val milestones = listOf(
        Milestone(365 * 10, "Ten years ago tonight", 5),
        Milestone(365 * 5, "Five years ago tonight", 5),
        Milestone(365 * 3, "Three years ago tonight", 5),
        Milestone(365 * 2, "Two years ago tonight", 5),
        Milestone(365, "A year ago tonight", 4),
        Milestone(182, "Six months ago tonight", 3),
        Milestone(91, "Three months ago tonight", 2),
        Milestone(30, "A month ago tonight", 1),
    )

    /** Below this, a note is not a memory — it is just last week. */
    private const val MIN_MEMORY_AGE_DAYS = 14

    /**
     * Picks the night to resurface.
     *
     * Only needs the *distinct dates* in the journal — one short string a day, so
     * a twenty-year journal is a few thousand strings and nothing else is loaded.
     * When several eras could be shown, the choice is stable for a given date: the
     * same evening always brings back the same memory, and the next evening brings
     * a different one.
     */
    fun anniversary(written: Collection<String>, today: String = Dates.today()): Anniversary? {
        val dates = written.toHashSet()
        val hits = milestones.mapNotNull { milestone ->
            val target = Dates.addDays(today, -milestone.days)
            val offsets = (-milestone.window..milestone.window).sortedBy { abs(it) }
            val date = offsets
                .map { Dates.addDays(target, it) }
                .firstOrNull { it in dates && Dates.daysBetween(it, today) >= MIN_MEMORY_AGE_DAYS }
            date?.let { Anniversary(it, milestone.label) }
        }
        if (hits.isEmpty()) return null
        return hits[Math.floorMod(today.hashCode(), hits.size)]
    }

    /**
     * Consecutive days ending today (or yesterday, if today has not been written
     * yet) that contain at least one note. [dates] must be distinct ISO dates —
     * several notes in one day still count as one day of the streak.
     */
    fun streak(dates: Collection<String>, today: String = Dates.today()): Int {
        val written = dates.toHashSet()
        var cursor = today
        if (cursor !in written) cursor = Dates.addDays(cursor, -1)
        var count = 0
        while (cursor in written) {
            count++
            cursor = Dates.addDays(cursor, -1)
        }
        return count
    }

    /** ISO date of the first day of the current week's window (7 days incl. today). */
    fun weekStart(today: String = Dates.today()): String = Dates.addDays(today, -(THIS_WEEK_DAYS - 1))

    /** Category counts with rounded percentages, biggest first. */
    fun categoryStats(counts: List<CategoryCount>): List<CategoryStat> {
        val total = counts.sumOf { it.count }
        if (total == 0) return emptyList()
        return counts
            .map { CategoryStat(Category.fromName(it.category), it.count, it.count * 100 / total) }
            .sortedWith(compareByDescending<CategoryStat> { it.count }.thenBy { it.category.ordinal })
    }

    /**
     * The last [months] months, oldest first, including months with no notes —
     * an empty month is information too. [rows] is the raw `GROUP BY month`
     * result, which may cover far more months than we display.
     */
    fun monthTrend(
        rows: List<MonthCountRow>,
        today: String = Dates.today(),
        months: Int = TREND_MONTHS,
    ): List<MonthCount> {
        val counts = rows.associate { it.yearMonth to it.count }
        val result = ArrayList<MonthCount>(months)
        var cursor = Dates.monthStart(today)
        repeat(months) {
            val key = Dates.monthKey(cursor)
            result.add(MonthCount(yearMonth = key, label = Dates.monthLabel(cursor), count = counts[key] ?: 0))
            cursor = Dates.monthStart(Dates.addDays(cursor, -1))
        }
        return result.asReversed()
    }

    fun report(
        range: InsightRange,
        categoryStats: List<CategoryStat>,
        totalEntries: Int,
        daysCovered: Int,
        monthlyTrend: List<MonthCount> = emptyList(),
    ): InsightReport {
        val top = categoryStats.firstOrNull()
        val (sentence, followUp) = when {
            totalEntries == 0 -> "No check-ins yet in this range." to null
            top == null -> "Nothing to report yet." to null
            else -> sentenceFor(top.category, top.percentage, range, totalEntries)
        }
        return InsightReport(
            range = range,
            totalEntries = totalEntries,
            daysCovered = daysCovered,
            topCategory = top?.category,
            topCount = top?.count ?: 0,
            topPercentage = top?.percentage ?: 0,
            categoryStats = categoryStats,
            monthlyTrend = monthlyTrend,
            sentence = sentence,
            followUp = followUp,
        )
    }

    private fun sentenceFor(category: Category, percentage: Int, range: InsightRange, total: Int): Pair<String, String?> {
        val rangePhrase = when (range) {
            InsightRange.THIS_WEEK -> "the last 7 days"
            InsightRange.ALL_TIME -> "your journal so far"
        }
        val templates = when (category) {
            Category.COMMUNICATION -> listOf(
                "In $rangePhrase, $percentage% of what you regretted came back to \"not saying what you felt\".",
                "$percentage% of your regrets in $rangePhrase are about communication — the words that stayed stuck in your throat.",
            )
            Category.COURAGE -> listOf(
                "In $rangePhrase, $percentage% of your regrets are about courage — moments you held yourself back.",
                "$percentage% of what you regretted in $rangePhrase comes from choosing the safe option.",
            )
            Category.HEALTH -> listOf(
                "In $rangePhrase, $percentage% of your regrets circle back to your body and health.",
                "$percentage% of your regrets in $rangePhrase are health-related — small choices, repeated.",
            )
            Category.WORK -> listOf(
                "In $rangePhrase, $percentage% of your regrets live at work.",
                "$percentage% of what you regretted in $rangePhrase is work-related.",
            )
            Category.MONEY -> listOf(
                "In $rangePhrase, $percentage% of your regrets are about money.",
                "$percentage% of your regrets in $rangePhrase come back to money.",
            )
            Category.TIME -> listOf(
                "In $rangePhrase, $percentage% of your regrets are about how you spent your time.",
                "$percentage% of what you regretted in $rangePhrase is time — the hours that slipped away.",
            )
            Category.RELATIONSHIPS -> listOf(
                "In $rangePhrase, $percentage% of your regrets are about people.",
                "$percentage% of your regrets in $rangePhrase come back to relationships.",
            )
            Category.OTHER -> listOf(
                "In $rangePhrase, $percentage% of your regrets didn't fit a neat box.",
                "$percentage% of what you regretted in $rangePhrase is hard to categorize — and that's fine.",
            )
        }
        val sentence = templates[(total + category.ordinal) % templates.size]
        return sentence to followUpFor(category)
    }

    private fun followUpFor(category: Category): String = when (category) {
        Category.COMMUNICATION -> "Is there a conversation you've been putting off?"
        Category.COURAGE -> "Is there one small risk you keep avoiding this week?"
        Category.HEALTH -> "What is one tiny habit you could restart tomorrow?"
        Category.WORK -> "What task have you been circling all week?"
        Category.MONEY -> "What money decision are you still unsure about?"
        Category.TIME -> "What kept getting postponed that only takes 10 minutes?"
        Category.RELATIONSHIPS -> "Who did you wish you'd reached out to today?"
        Category.OTHER -> "What would you do differently tomorrow?"
    }
}
