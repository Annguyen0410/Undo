package com.zhiend.regretnote.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

// Editorial pairing: a warm serif for display/headlines (a journal's voice),
// clean sans for everything else. Serif keeps the "reflect at night" soul
// without the neon gimmick — it reads as a considered, human product.
//
// Two rules keep the screen calm:
//   1. Serif is for *content* (the question, entry text, headings) — never for
//      labels, buttons or numbers.
//   2. Sans labels come in exactly three sizes (14 / 12 / 11sp) with wide
//      tracking when uppercase, so the eye can tell "chrome" from "content"
//      at a glance.

private val Display = FontFamily.Serif

private fun display(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = Display,
    fontWeight = FontWeight.Light,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = (-size * 0.01f).sp,
)

private fun headline(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Display,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = (-size * 0.008f).sp,
)

private fun title(size: Double, lineHeight: Double, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.1.sp,
)

private fun body(size: Double, lineHeight: Double, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.15.sp,
)

val Typography = Typography(
    displayLarge = display(46, 52),
    displayMedium = display(40, 46),
    displaySmall = display(34, 40),
    headlineLarge = headline(32, 40),
    headlineMedium = headline(26, 33),
    headlineSmall = headline(21, 28, FontWeight.Medium),
    titleLarge = title(19.0, 26.0),
    titleMedium = title(15.5, 22.0),
    titleSmall = title(14.0, 20.0),
    bodyLarge = body(15.5, 25.0),
    bodyMedium = body(14.0, 22.0),
    bodySmall = body(12.5, 19.0),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.9.sp,
    ),
)

/**
 * Small, wide-tracked uppercase text. Used for every section heading in the app
 * (and only there) so the eye learns it as "chrome" and skips it when scanning.
 * Prefer [SectionLabel] over using this directly.
 */
val SectionLabelText = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 1.6.sp,
)

/** The nightly question — the largest serif line on any screen. */
val SerifHero = TextStyle(
    fontFamily = Display,
    fontWeight = FontWeight.Normal,
    fontSize = 27.sp,
    lineHeight = 37.sp,
    letterSpacing = (-0.3).sp,
)

/**
 * Big numbers in stat tiles. Tabular figures keep the digits from shifting as
 * the value changes from "9" to "10".
 */
val NumericValue = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 25.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.2).sp,
    fontFeatureSettings = "tnum",
)
