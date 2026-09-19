package com.zhiend.regretnote.devtools

import com.zhiend.regretnote.data.Category
import com.zhiend.regretnote.data.Entry
import com.zhiend.regretnote.util.Dates
import kotlin.random.Random

/**
 * Deterministic sample history, used only from the debug-only section of the settings
 * screen. A brand-new install starts empty by design — notes written by hand — which
 * makes the timeline, the category donut and the monthly trend impossible to demo,
 * screenshot or record a video with. This fills in ~90 days of plausible notes.
 *
 * Some days deliberately hold two or three notes (a second look at the same evening
 * is exactly what the unlimited journal is for), which also gives the timeline and
 * the note timestamps something real to show.
 *
 * Deterministic output (fixed seed) means the demo screenshots stay reproducible.
 */
object SampleJournal {

    /** How many days of history to generate, ending today. */
    private const val DAYS = 90

    /** Roughly one evening in five passes without writing anything down. */
    private const val SKIP_CHANCE = 0.18

    /** A second note on the same evening — the "actually, also this" note. */
    private const val SECOND_NOTE_CHANCE = 0.18

    /** A third, rarer: a day that kept giving. */
    private const val THIRD_NOTE_CHANCE = 0.05

    /** Always keep the most recent days so the streak and the 7-day insights have shape. */
    private const val ALWAYS_FILL_RECENT_DAYS = 5

    /** Wall-clock times notes get on past days; ascending, like a real evening. */
    private val noteTimes = listOf(19 to 40, 21 to 15, 23 to 5)

    /** Categories are not equally represented — that is the whole point of the app. */
    private val categoryWeights: List<Pair<Category, Int>> = listOf(
        Category.COMMUNICATION to 5,
        Category.TIME to 4,
        Category.WORK to 4,
        Category.COURAGE to 3,
        Category.RELATIONSHIPS to 3,
        Category.HEALTH to 3,
        Category.MONEY to 2,
        Category.OTHER to 2,
    )

    private val lines: Map<Category, List<String>> = mapOf(
        Category.COMMUNICATION to listOf(
            "Should have replied properly instead of leaving it on read.",
            "Said yes to a call I didn't have the energy for, then half-listened.",
            "Wish I'd asked the question instead of assuming I knew the answer.",
        ),
        Category.TIME to listOf(
            "Two hours of scrolling instead of the book on my desk.",
            "Meant to start at four. Started at ten.",
            "In bed at 1am again — the morning paid for it.",
        ),
        Category.WORK to listOf(
            "Stayed quiet in the standup when I actually had the answer.",
            "Spent an hour polishing a slide nobody will read.",
            "Should have said no to that extra ticket.",
        ),
        Category.COURAGE to listOf(
            "Didn't send the message I drafted four times.",
            "Let the moment pass instead of introducing myself.",
            "Stayed on the safe version of the plan again.",
        ),
        Category.RELATIONSHIPS to listOf(
            "Was short on the phone for no real reason.",
            "Forgot to ask how their interview went.",
            "Let a small thing turn into a long silence.",
        ),
        Category.HEALTH to listOf(
            "Skipped the walk because it looked like rain.",
            "Third coffee after 3pm. Again.",
            "Promised myself an early night and didn't.",
        ),
        Category.MONEY to listOf(
            "Bought the upgrade I didn't need at checkout.",
            "Never checked the subscription I've been paying since March.",
            "Should have looked at the numbers before saying yes.",
        ),
        Category.OTHER to listOf(
            "Took the photo in my head instead of with the camera.",
            "Left the room before the good part of the evening.",
            "Wrote none of it down at the time.",
        ),
    )

    /**
     * A deliberately huge journal: [years] of history ending where [recentDays]
     * picks up, ~1.2 notes a day (roughly 8,700 notes for 20 years). This exists
     * to prove the claim that the journal never loads itself into memory — a
     * paging bug or an accidental "read all entries" shows up here as a stall,
     * and nowhere else.
     */
    fun decades(years: Int = 20, today: String = Dates.today()): List<Entry> {
        val random = Random(1999)
        val pool = categoryWeights.flatMap { (category, weight) -> List(weight) { category } }
        val days = years * 365
        val notes = ArrayList<Entry>((days * 1.2).toInt())

        // Start strictly before the recent window so the two seeders cannot
        // produce competing notes for the same evening.
        for (offset in DAYS + 1 until DAYS + 1 + days) {
            val count = when {
                random.nextDouble() < 0.62 -> 1
                random.nextDouble() < 0.45 -> 2
                else -> 0
            }
            if (count == 0) continue
            val date = Dates.addDays(today, -offset)
            for (index in 0 until count) {
                val category = pool[random.nextInt(pool.size)]
                val options = lines.getValue(category)
                val (hour, minute) = noteTimes[index.coerceAtMost(noteTimes.lastIndex)]
                notes += Entry(
                    date = date,
                    text = options[random.nextInt(options.size)],
                    category = category.name,
                    intensity = 1 + random.nextInt(3),
                    createdAt = Dates.atTime(date, hour, minute),
                )
            }
        }
        return notes
    }

    /**
     * Notes for the last [DAYS] days, ending today. Within a day the notes are in
     * writing order (oldest first) and never share a timestamp.
     */
    fun recentDays(today: String = Dates.today()): List<Entry> {
        val random = Random(20260930)
        val pool = categoryWeights.flatMap { (category, weight) -> List(weight) { category } }
        val now = System.currentTimeMillis()
        val notes = ArrayList<Entry>(DAYS * 2)

        for (offset in 0 until DAYS) {
            if (offset >= ALWAYS_FILL_RECENT_DAYS && random.nextDouble() < SKIP_CHANCE) continue

            var count = 1
            if (random.nextDouble() < SECOND_NOTE_CHANCE) count++
            if (random.nextDouble() < THIRD_NOTE_CHANCE) count++

            val date = Dates.addDays(today, -offset)
            for (index in 0 until count) {
                val category = pool[random.nextInt(pool.size)]
                val options = lines.getValue(category)
                notes += Entry(
                    date = date,
                    text = options[random.nextInt(options.size)],
                    category = category.name,
                    intensity = random.nextInt(100).let { roll ->
                        when {
                            roll < 20 -> 3
                            roll < 60 -> 2
                            else -> 1
                        }
                    },
                    createdAt = if (offset == 0) {
                        // Today: relative to now, oldest of the day first.
                        now - (count - 1 - index) * 45L * 60_000L
                    } else {
                        val (hour, minute) = noteTimes[index.coerceAtMost(noteTimes.lastIndex)]
                        Dates.atTime(date, hour, minute)
                    },
                )
            }
        }
        return notes
    }
}
