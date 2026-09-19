package com.zhiend.regretnote.util

import com.zhiend.regretnote.data.Entry

/** One day of the journal, with its notes in the order the query returned them. */
data class DayGroup(
    val date: String,
    val notes: List<Entry>,
)

/**
 * Groups a (newest-first) list of notes into days, preserving order.
 *
 * The timeline shows one page at a time, so a day can straddle two pages. Because
 * pages are appended to the same list before grouping, the day simply gains its
 * remaining notes on the next load — which is why grouping happens here, on the
 * accumulated list, and not per page.
 */
fun List<Entry>.groupedByDay(): List<DayGroup> =
    groupBy { it.date }
        .map { (date, notes) -> DayGroup(date, notes) }

/**
 * How many notes the first screen of the timeline is worth. Small enough that a
 * cold start renders immediately, large enough that scrolling rarely waits.
 */
const val TIMELINE_PAGE_SIZE = 50
