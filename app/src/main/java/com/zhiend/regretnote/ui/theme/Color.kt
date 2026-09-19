package com.zhiend.regretnote.ui.theme

import androidx.compose.ui.graphics.Color

// A quiet "midnight observatory" palette: near-black plum backgrounds that never
// shear into pure black, one desaturated violet accent and a handful of muted
// secondary tones. Nothing glows.
//
// Elevation ladder — the earlier version had every surface within ~9/255 of the
// background, which is why panels read as one flat mud. There are now three
// clearly different steps, and every panel that matters also carries a hairline:
//
//   background        canvas
//   surfaceContainerLow   wells *inside* a panel (text fields, chart trays)
//   surfaceContainer      panels and cards
//   surfaceContainerHigh  raised chips, icon wells, selected rows
//
// Anything lighter than +18/255 on the red channel starts to look like a light
// theme, so the ladder is deliberately short.

val UndoPrimary = Color(0xFFAFA5F7)          // desaturated lavender violet
val UndoOnPrimary = Color(0xFF170F33)
val UndoPrimaryContainer = Color(0xFF2E2765)
val UndoOnPrimaryContainer = Color(0xFFE9E4FF)

val UndoSecondary = Color(0xFF8FBFBB)        // lean sage-teal
val UndoOnSecondary = Color(0xFF102223)
val UndoSecondaryContainer = Color(0xFF1E3B3C)
val UndoOnSecondaryContainer = Color(0xFFC9ECEB)

val UndoTertiary = Color(0xFFDFA0C4)         // muted rose
val UndoOnTertiary = Color(0xFF2C1220)
val UndoTertiaryContainer = Color(0xFF44243A)
val UndoOnTertiaryContainer = Color(0xFFF4D1E4)

val UndoBackground = Color(0xFF0B0912)
val UndoOnBackground = Color(0xFFEDEBF7)
val UndoSurface = Color(0xFF100E18)
val UndoOnSurface = Color(0xFFF0EEFA)
val UndoSurfaceVariant = Color(0xFF1E1A2A)
val UndoOnSurfaceVariant = Color(0xFFACA5C8)
val UndoSurfaceContainerLowest = Color(0xFF0D0B14)
val UndoSurfaceContainerLow = Color(0xFF13101C)
val UndoSurfaceContainer = Color(0xFF1A1626)
val UndoSurfaceContainerHigh = Color(0xFF221D31)
val UndoSurfaceContainerHighest = Color(0xFF2A2440)
val UndoOutline = Color(0xFF8E87A8)
val UndoOutlineVariant = Color(0xFF322C48)

// Errors stay in the same muted register as everything else — a shout here would
// be the only loud colour in the product.
val UndoError = Color(0xFFE39A9A)
val UndoOnError = Color(0xFF2E1416)
val UndoErrorContainer = Color(0xFF3A1E22)

// Premium "Midnight Reflection" variant: the same hue, pushed deeper and colder.
val MidnightBackground = Color(0xFF08080E)
val MidnightOnBackground = Color(0xFFE7E5F0)
val MidnightSurface = Color(0xFF0E0E17)
val MidnightOnSurface = Color(0xFFF2F1F9)
val MidnightSurfaceVariant = Color(0xFF1A1A28)
val MidnightOnSurfaceVariant = Color(0xFFA9A5C0)
val MidnightPrimary = Color(0xFFBFBCFF)
val MidnightOnPrimary = Color(0xFF191850)
val MidnightPrimaryContainer = Color(0xFF3B397B)
val MidnightOnPrimaryContainer = Color(0xFFE4E1FF)
val MidnightSecondary = Color(0xFFB0B7D6)
val MidnightOnSecondary = Color(0xFF252338)
val MidnightSecondaryContainer = Color(0xFF3A3856)
val MidnightOnSecondaryContainer = Color(0xFFDDE0F4)
val MidnightTertiary = Color(0xFFE2AED3)
val MidnightOnTertiary = Color(0xFF38162C)
val MidnightTertiaryContainer = Color(0xFF502E44)
val MidnightOnTertiaryContainer = Color(0xFFF6D3EA)
val MidnightOutline = Color(0xFFA5A1BC)
val MidnightOutlineVariant = Color(0xFF303048)
val MidnightSurfaceContainerLowest = Color(0xFF0B0B12)
val MidnightSurfaceContainerLow = Color(0xFF12121D)
val MidnightSurfaceContainer = Color(0xFF191926)
val MidnightSurfaceContainerHigh = Color(0xFF212131)
val MidnightSurfaceContainerHighest = Color(0xFF29293D)
