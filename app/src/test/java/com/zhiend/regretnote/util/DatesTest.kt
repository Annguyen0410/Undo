package com.zhiend.regretnote.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Date helpers. Everything in the app stores dates as ISO strings precisely so
 * these stay trivial, but "the last day of the month a bucket key names" is the
 * ceiling the journal pages against, so it is worth pinning down.
 */
class DatesTest {

    @Test
    fun `month end is the last day of that month`() {
        assertEquals("2026-09-30", Dates.monthEndOfKey("2026-09"))
        assertEquals("2026-02-28", Dates.monthEndOfKey("2026-02"))
        assertEquals("2026-12-31", Dates.monthEndOfKey("2026-12"))
    }

    @Test
    fun `month end handles a leap february`() {
        assertEquals("2024-02-29", Dates.monthEndOfKey("2024-02"))
    }

    @Test
    fun `month names come out of the same bucket key`() {
        assertEquals("Aug 2006", Dates.monthYearLabelOfKey("2006-08"))
    }

    @Test
    fun `a month-end ceiling sorts after every day it contains and before the next month`() {
        val ceiling = Dates.monthEndOfKey("2014-08")
        assertEquals("2014-08-31", ceiling)
        // String comparison is the whole reason ISO dates are stored this way: the
        // ceiling has to exclude September without a second filter.
        assertEquals(true, "2014-08-15" <= ceiling)
        assertEquals(false, "2014-09-01" <= ceiling)
    }

    @Test
    fun `the ceiling sentinel sorts after any real date`() {
        assertEquals(true, "2026-09-18" <= Dates.MAX_ISO)
        assertEquals(false, "0000-01-01" > Dates.MAX_ISO)
    }

    @Test
    fun `walking back across a year boundary keeps the calendar straight`() {
        assertEquals("2025-12-31", Dates.addDays("2026-01-01", -1))
        assertEquals("2028-02-29", Dates.addDays("2028-03-01", -1))
    }
}
