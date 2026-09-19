package com.zhiend.regretnote.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Small date helpers. Dates are stored as ISO strings ("yyyy-MM-dd"), which sort
 * lexicographically, so no java.time / desugaring is required on API 24.
 */
object Dates {

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /**
     * Sentinel "start of time" for range queries that have no lower bound.
     * ISO dates sort lexicographically, so comparing against this is exact and
     * still uses the index — no nullable date parameters anywhere.
     */    const val MIN_ISO = "0000-01-01"

    /**
     * Sentinel "end of time" — the pagination ceiling when the reader has not
     * jumped to a month. Same trick as [MIN_ISO]: an ISO date that sorts after
     * every real one, so the bound stays a non-null indexed range scan.
     */
    const val MAX_ISO = "9999-12-31"

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.US)
    private val longFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val monthFormat = SimpleDateFormat("MMM", Locale.US)
    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.US)
    private val clockFormat = SimpleDateFormat("h:mm a", Locale.US)

    /** Today's ISO date in the device's local timezone. */
    fun today(): String = format(Calendar.getInstance())

    /** ISO date [days] days before/after [iso]. */
    fun addDays(iso: String, days: Int): String {
        val calendar = parse(iso)
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return format(calendar)
    }

    /** Number of days between two ISO dates (positive when [fromIso] is earlier). */
    fun daysBetween(fromIso: String, toIso: String): Int {
        val from = parse(fromIso).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val to = parse(toIso).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        return ((to.timeInMillis - from.timeInMillis) / MILLIS_PER_DAY).toInt()
    }

    /** "Mon, Aug 31" for the timeline headers. */
    fun formatDisplay(iso: String): String = parse(iso).time.let { displayFormat.format(it) }

    /** "Sep 18, 2025" — the archived night needs its year. */
    fun formatLong(iso: String): String = parse(iso).time.let { longFormat.format(it) }

    /** "Aug" for month trend labels. */
    fun monthLabel(iso: String): String = parse(iso).time.let { monthFormat.format(it) }

    /** "Aug 2016" — the journal header says how far back it goes. */
    fun monthYearLabel(iso: String): String = parse(iso).time.let { monthYearFormat.format(it) }

    /** "Aug 2016" straight from a `"2026-08"` bucket key. */
    fun monthYearLabelOfKey(yearMonth: String): String = monthYearLabel("$yearMonth-01")

    /**
     * Last day of the month a `"2026-08"` bucket key names, as an ISO date.
     * This is the pagination ceiling for "show me the journal as it stood then".
     */
    fun monthEndOfKey(yearMonth: String): String {
        val start = parse("$yearMonth-01")
        start.add(Calendar.MONTH, 1)
        return addDays(format(start), -1)
    }

    /** "9:42 PM" — shown next to every note, so a day can hold several. */
    fun formatClock(epochMillis: Long): String = clockFormat.format(Date(epochMillis))

    /** Epoch millis for [iso] at the given local wall-clock time. */
    fun atTime(iso: String, hour: Int, minute: Int): Long = parse(iso).apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** "2026-08" bucket key for monthly trends. */
    fun monthKey(iso: String): String {
        val calendar = parse(iso)
        return "%04d-%02d".format(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }

    /** ISO date of the first day of the month containing [iso]. */
    fun monthStart(iso: String): String {
        val calendar = parse(iso)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return format(calendar)
    }

    /** Human-friendly "9:00 PM". */
    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour % 12 == 0) 12 else hour % 12
        return if (minute == 0) "$h $amPm" else String.format(Locale.US, "%d:%02d %s", h, minute, amPm)
    }

    fun format(calendar: Calendar): String = isoFormat.format(calendar.time)

    fun parse(iso: String): Calendar = Calendar.getInstance().apply {
        time = isoFormat.parse(iso) ?: Date(0L)
    }
}
