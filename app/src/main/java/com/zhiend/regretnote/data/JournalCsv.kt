package com.zhiend.regretnote.data

import com.zhiend.regretnote.util.Dates

/**
 * The journal's CSV dialect — export *and* import, in one place.
 *
 * Export used to be ten lines of string concatenation inside the settings view
 * model and import did not exist at all, which is the worst possible pairing for
 * a journal: the file the app wrote could not be read back, so "your notes are
 * yours" was a one-way door out of the app. Both directions now live here, so the
 * two can never drift apart, and so the whole format is testable without an
 * emulator.
 *
 * The dialect is RFC 4180: a header, then one record per note, fields separated by
 * commas, every field quoted with inner quotes doubled. Readers that are stricter
 * than that (a spreadsheet, `csv` in Python, a judge poking at the export) are the
 * reason for the quotes and for keeping the header names stable.
 */
object JournalCsv {

    const val HEADER = "date,category,intensity,text,createdAt"

    /**
     * Identity of one note across an export → import round trip.
     *
     * The CSV deliberately carries no row id, so a hand-edited file stays valid.
     * The closest thing to one is the moment the note was written, combined with
     * the day it belongs to. Two notes would have to be written in the same
     * millisecond to collide, which the UI cannot produce — every save is a
     * separate tap. Used both to dedupe a file against the journal and to dedupe
     * a file against itself.
     */
    fun key(date: String, createdAt: Long): String = "$date\u0000$createdAt"

    /** One record, exactly as [parse] expects to read it back. */
    fun row(entry: Entry): String = buildString {
        append(quote(entry.date)).append(',')
        append(quote(entry.category)).append(',')
        append(entry.intensity).append(',')
        append(quote(entry.text)).append(',')
        append(entry.createdAt)
    }

    private fun quote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    /** What [parse] made of a file, and what it refused to guess at. */
    data class Parsed(val entries: List<Entry>, val skipped: Int)

    /**
     * Reads a journal CSV, forgivingly.
     *
     * A journal is not a schema: this file may have come out of an old version of
     * the app, or out of a spreadsheet, or been edited by hand. So the reader
     * accepts what it can recognise and counts the rest:
     *
     *  - the header is optional, and recognised by name rather than by position;
     *  - blank lines and a UTF-8 BOM are ignored (Windows editors add the BOM);
     *  - an unknown category becomes [Category.OTHER] rather than dropping the note,
     *    because the text is the part that has no replacement;
     *  - intensity is clamped into 1..3, and a row whose date is not a real ISO date
     *    is skipped — a note filed under no day could never be found again;
     *  - a missing or unreadable timestamp falls back to midday on the note's own
     *    day, nudged by one millisecond per repeat so that several timeless notes on
     *    the same day stay distinct and none is silently swallowed as a duplicate.
     */
    fun parse(text: String): Parsed {
        val entries = ArrayList<Entry>()
        var skipped = 0
        val middayOffsets = HashMap<String, Int>()

        val rows = records(text).filterNot { record ->
            record.size == 1 && record[0].isBlank()
        }
        val hasHeader = rows.firstOrNull()?.firstOrNull()
            ?.trim()?.equals("date", ignoreCase = true) == true

        for (record in rows.drop(if (hasHeader) 1 else 0)) {
            if (record.size < 5) {
                skipped++
                continue
            }

            val date = record[0].trim()
            val intensity = record[2].trim().toIntOrNull()?.coerceIn(1, 3)
            if (!isIsoDate(date) || intensity == null) {
                skipped++
                continue
            }

            val offset = middayOffsets.getOrDefault(date, 0)
            middayOffsets[date] = offset + 1

            entries += Entry(
                date = date,
                text = record[3],
                category = Category.fromName(record[1].trim()).name,
                intensity = intensity,
                createdAt = record[4].trim().toLongOrNull()
                    ?: (Dates.atTime(date, 12, 0) + offset),
            )
        }
        return Parsed(entries, skipped)
    }

    /**
     * Splits CSV text into records of fields.
     *
     * A hand-rolled scanner rather than a `split(',')`, because a quoted note may
     * contain the newline the user pressed, and this file is full of prose.
     */
    private fun records(text: String): List<List<String>> {
        val source = text.removePrefix("\uFEFF")
        val out = ArrayList<List<String>>()
        var record = ArrayList<String>()
        var field = StringBuilder()
        var quoted = false
        var i = 0

        fun endField() {
            record.add(field.toString())
            field = StringBuilder()
        }

        while (i < source.length) {
            val c = source[i]
            when {
                quoted -> when {
                    // A doubled quote inside a quoted field is one literal quote.
                    c == '"' && i + 1 < source.length && source[i + 1] == '"' -> {
                        field.append('"')
                        i++
                    }
                    c == '"' -> quoted = false
                    else -> field.append(c)
                }
                c == '"' -> quoted = true
                c == ',' -> endField()
                // Swallowed, so a CRLF file and an LF file read the same.
                c == '\r' -> Unit
                c == '\n' -> {
                    endField()
                    out.add(record)
                    record = ArrayList()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || record.isNotEmpty()) {
            endField()
            out.add(record)
        }
        return out
    }

    /** `"2026-08-31"`, with the month and day checked so a typo cannot land in no month. */
    private fun isIsoDate(value: String): Boolean {
        if (value.length != 10 || value[4] != '-' || value[7] != '-') return false
        for (index in value.indices) {
            if (index == 4 || index == 7) continue
            if (!value[index].isDigit()) return false
        }
        return value.substring(5, 7).toInt() in 1..12 &&
            value.substring(8, 10).toInt() in 1..31
    }
}
