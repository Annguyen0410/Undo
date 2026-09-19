package com.zhiend.regretnote.data

import com.zhiend.regretnote.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The CSV format is the one part of this app designed to leave the app: a user's
 * entire journal can end up existing only as this file. So it is tested as a
 * contract — what export writes must be what import reads, including for text a
 * person typed by hand — rather than as an implementation detail.
 */
class JournalCsvTest {

    private fun export(entries: List<Entry>): String = buildString {
        appendLine(JournalCsv.HEADER)
        entries.forEach { appendLine(JournalCsv.row(it)) }
    }

    private fun note(
        date: String = "2026-08-31",
        text: String = "Should have replied properly instead of leaving it on read.",
        category: Category = Category.COMMUNICATION,
        intensity: Int = 2,
        createdAt: Long = 1_756_600_000_000L,
    ) = Entry(
        date = date,
        text = text,
        category = category.name,
        intensity = intensity,
        createdAt = createdAt,
    )

    @Test
    fun plainNoteSurvivesARoundTrip() {
        val original = note()

        val parsed = JournalCsv.parse(export(listOf(original)))

        assertEquals(0, parsed.skipped)
        val back = parsed.entries.single()
        assertEquals(original.date, back.date)
        assertEquals(original.text, back.text)
        assertEquals(original.category, back.category)
        assertEquals(original.intensity, back.intensity)
        assertEquals(original.createdAt, back.createdAt)
    }

    @Test
    fun punctuationInTheTextSurvives() {
        // Every character that is meaningful to the parser, in one note: commas
        // would split the field, quotes would end it, and the newline is a real
        // Enter the user pressed in the composer.
        val original = note(text = "Rang, then said \"never mind\"\nand left it at that — twice.")

        val back = JournalCsv.parse(export(listOf(original))).entries.single()

        assertEquals(original.text, back.text)
    }

    @Test
    fun severalNotesSurviveTogether() {
        val originals = listOf(
            note(date = "2006-06-26", category = Category.TIME, text = "Two hours of scrolling."),
            note(date = "2026-08-31", category = Category.HEALTH, text = "Third coffee after 3pm."),
            note(date = "2026-08-31", category = Category.WORK, text = "Should have said no.", intensity = 3),
        )

        val parsed = JournalCsv.parse(export(originals))

        assertEquals(0, parsed.skipped)
        assertEquals(originals.size, parsed.entries.size)
        assertEquals(originals.map { it.text }, parsed.entries.map { it.text })
    }

    @Test
    fun headerIsOptional() {
        val body = "\"2026-08-31\",\"WORK\",2,\"Spent an hour on a slide.\",1756600000000\n"

        val parsed = JournalCsv.parse(body)

        assertEquals(1, parsed.entries.size)
        assertEquals("Spent an hour on a slide.", parsed.entries.single().text)
    }

    @Test
    fun aHeaderNamedInAnyCaseIsStillAHeader() {
        val parsed = JournalCsv.parse(
            "Date,Category,Intensity,Text,CreatedAt\n" +
                "\"2026-08-31\",\"WORK\",2,\"Spent an hour on a slide.\",1756600000000\n",
        )

        assertEquals(1, parsed.entries.size)
    }

    @Test
    fun windowsLineEndingsAndAByteOrderMarkAreRead() {
        // A file that has been through a Windows text editor: BOM at the front,
        // CRLF between rows, and no trailing newline.
        val text = "\uFEFFdate,category,intensity,text,createdAt\r\n" +
            "\"2026-08-31\",\"WORK\",2,\"Spent an hour on a slide.\",1756600000000"

        val parsed = JournalCsv.parse(text)

        assertEquals(0, parsed.skipped)
        assertEquals("Spent an hour on a slide.", parsed.entries.single().text)
    }

    @Test
    fun blankLinesAndTrailingNewlinesAreIgnored() {
        val text = "\n" + JournalCsv.HEADER + "\n\n" +
            JournalCsv.row(note()) + "\n\n\n"

        val parsed = JournalCsv.parse(text)

        assertEquals(0, parsed.skipped)
        assertEquals(1, parsed.entries.size)
    }

    @Test
    fun aCategoryThisAppDoesNotKnowBecomesOther() {
        val parsed = JournalCsv.parse("\"2026-08-31\",\"NONSENSE\",2,\"Something.\",1756600000000")

        assertEquals(Category.OTHER.name, parsed.entries.single().category)
    }

    @Test
    fun intensityIsClampedIntoTheRangeTheUiCanShow() {
        fun intensityOf(raw: String) =
            JournalCsv.parse("\"2026-08-31\",\"WORK\",$raw,\"Something.\",1756600000000")
                .entries.single().intensity

        assertEquals(3, intensityOf("9"))
        assertEquals(1, intensityOf("0"))
        assertEquals(1, intensityOf("-4"))
    }

    @Test
    fun aRowWithoutARealDateIsSkippedRatherThanFiledNowhere() {
        val text = List(3) { index ->
            val date = when (index) {
                0 -> "not-a-date"
                1 -> "2026-13-01"
                else -> "2026-08-31"
            }
            "\"$date\",\"WORK\",2,\"Something.\",1756600000000"
        }.joinToString("\n")

        val parsed = JournalCsv.parse(text)

        assertEquals(2, parsed.skipped)
        assertEquals(1, parsed.entries.size)
        assertEquals("2026-08-31", parsed.entries.single().date)
    }

    @Test
    fun aRowTooShortToBeANoteIsSkipped() {
        val parsed = JournalCsv.parse("\"2026-08-31\",\"WORK\"\n")

        assertEquals(1, parsed.skipped)
        assertTrue(parsed.entries.isEmpty())
    }

    @Test
    fun aMissingTimestampLandsOnThatDayWithoutCollapsingNotesTogether() {
        // No createdAt at all. The day is the part that carries meaning, so the note
        // must still land on its day — and three timeless notes on the same evening
        // must stay three notes, not be de-duplicated into one.
        val text = listOf(
            "\"2026-08-31\",\"WORK\",2,\"One.\",",
            "\"2026-08-31\",\"TIME\",1,\"Two.\",",
            "\"2026-08-31\",\"HEALTH\",3,\"Three.\",",
        ).joinToString("\n")

        val parsed = JournalCsv.parse(text)

        assertEquals(3, parsed.entries.size)
        assertTrue(parsed.entries.all { it.date == "2026-08-31" })
        assertEquals(3, parsed.entries.map { JournalCsv.key(it.date, it.createdAt) }.toSet().size)
    }

    @Test
    fun aTimestampThatCannotBeReadFallsBackTheSameWayEveryTime() {
        val text = "\"2026-08-31\",\"WORK\",2,\"One.\",not-a-number"

        // Two parses of the same file must agree, or re-importing a hand-edited file
        // would append a copy every time instead of recognising it.
        val first = JournalCsv.parse(text).entries.single()
        val second = JournalCsv.parse(text).entries.single()

        assertEquals(first.createdAt, second.createdAt)
        assertEquals(Dates.atTime("2026-08-31", 12, 0), first.createdAt)
    }

    @Test
    fun theIdentityOfANoteIsStableAcrossARoundTrip() {
        val original = note()

        val back = JournalCsv.parse(export(listOf(original))).entries.single()

        assertEquals(
            JournalCsv.key(original.date, original.createdAt),
            JournalCsv.key(back.date, back.createdAt),
        )
        // ...and two notes written a millisecond apart are not the same note.
        assertNotEquals(
            JournalCsv.key("2026-08-31", 1_000L),
            JournalCsv.key("2026-08-31", 1_001L),
        )
    }
}
