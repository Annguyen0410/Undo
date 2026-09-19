package com.zhiend.regretnote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Signature brand gradients. Used sparingly — a hairline, a wordmark and the
 * premium banner — so they read as craft rather than decoration.
 *
 * There is no longer a gradient *button*: a two-colour wash under a dark label
 * was the lowest-contrast control on the screen. Primary actions are solid.
 */
object AppGradients {

    /** Editorial accent: violet into rose. Hero words, hairlines, the wordmark. */
    @Composable
    fun accent(): Brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        ),
    )

    /**
     * The premium wash: a soft diagonal violet that fades out. Painted *over*
     * the panel fill of the premium banner so the card still reads as a surface
     * rather than a sticker.
     */
    @Composable
    fun premiumWash(): Brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f),
            Color.Transparent,
        ),
    )
}
