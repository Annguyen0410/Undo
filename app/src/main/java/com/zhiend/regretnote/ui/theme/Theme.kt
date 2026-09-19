package com.zhiend.regretnote.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * The default canvas: a calm, near-black night. The whole app sits on a dark
 * plum wash — there is no light scheme. Every screen is a place to reflect
 * without jumping out at you.
 */
private val UndoColorScheme = darkColorScheme(
    primary = UndoPrimary,
    onPrimary = UndoOnPrimary,
    primaryContainer = UndoPrimaryContainer,
    onPrimaryContainer = UndoOnPrimaryContainer,
    secondary = UndoSecondary,
    onSecondary = UndoOnSecondary,
    secondaryContainer = UndoSecondaryContainer,
    onSecondaryContainer = UndoOnSecondaryContainer,
    tertiary = UndoTertiary,
    onTertiary = UndoOnTertiary,
    tertiaryContainer = UndoTertiaryContainer,
    onTertiaryContainer = UndoOnTertiaryContainer,
    error = UndoError,
    onError = UndoOnError,
    errorContainer = UndoErrorContainer,
    onErrorContainer = UndoTertiaryContainer,
    background = UndoBackground,
    onBackground = UndoOnBackground,
    surface = UndoSurface,
    onSurface = UndoOnSurface,
    surfaceVariant = UndoSurfaceVariant,
    onSurfaceVariant = UndoOnSurfaceVariant,
    surfaceContainerLowest = UndoSurfaceContainerLowest,
    surfaceContainerLow = UndoSurfaceContainerLow,
    surfaceContainer = UndoSurfaceContainer,
    surfaceContainerHigh = UndoSurfaceContainerHigh,
    surfaceContainerHighest = UndoSurfaceContainerHighest,
    outline = UndoOutline,
    outlineVariant = UndoOutlineVariant,
)

/** Premium-only "Midnight Reflection" scheme: deeper, colder, near-monochrome. */
private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = MidnightOnPrimary,
    primaryContainer = MidnightPrimaryContainer,
    onPrimaryContainer = MidnightOnPrimaryContainer,
    secondary = MidnightSecondary,
    onSecondary = MidnightOnSecondary,
    secondaryContainer = MidnightSecondaryContainer,
    onSecondaryContainer = MidnightOnSecondaryContainer,
    tertiary = MidnightTertiary,
    onTertiary = MidnightOnTertiary,
    tertiaryContainer = MidnightTertiaryContainer,
    onTertiaryContainer = MidnightOnTertiaryContainer,
    error = UndoError,
    onError = UndoOnError,
    errorContainer = UndoErrorContainer,
    onErrorContainer = UndoTertiaryContainer,
    background = MidnightBackground,
    onBackground = MidnightOnBackground,
    surface = MidnightSurface,
    onSurface = MidnightOnSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightOnSurfaceVariant,
    surfaceContainerLowest = MidnightSurfaceContainerLowest,
    surfaceContainerLow = MidnightSurfaceContainerLow,
    surfaceContainer = MidnightSurfaceContainer,
    surfaceContainerHigh = MidnightSurfaceContainerHigh,
    surfaceContainerHighest = MidnightSurfaceContainerHighest,
    outline = MidnightOutline,
    outlineVariant = MidnightOutlineVariant,
)

/** Softer, larger, non-"chunky" roundness — calm, not bubbly. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun RegretNoteTheme(
    darkTheme: Boolean = true,
    midnight: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (midnight) MidnightColorScheme else UndoColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
