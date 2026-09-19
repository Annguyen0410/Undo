package com.zhiend.regretnote.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The app's spacing scale. Screens used to hand-pick 10/14/18/24/28/32dp, which
 * is why no two screens had the same rhythm; these six steps cover every gap in
 * the product. When a value feels "almost right", move it to the next step
 * rather than inventing a number.
 */
object Spacing {
    /** 4dp — label to its value, dot to its text. */
    val xxs = 4.dp

    /** 8dp — inside a chip, icon to label. */
    val xs = 8.dp

    /** 12dp — rows inside a panel. */
    val sm = 12.dp

    /** 16dp — panel inner padding, gap between related panels. */
    val md = 16.dp

    /** 24dp — new section. */
    val lg = 24.dp

    /** 32dp — new block / above a primary action. */
    val xl = 32.dp
}

/** Horizontal page margin. Every screen uses this, nothing else. */
val ScreenPadding = 20.dp

/**
 * Height of the app's primary call-to-action. Also the minimum tap target for
 * anything that is not an icon button.
 */
val PrimaryActionHeight = 54.dp
