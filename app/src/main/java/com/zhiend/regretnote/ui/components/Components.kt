package com.zhiend.regretnote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhiend.regretnote.data.Category

/** The one palette for "how heavy does it feel?" — shared by tags and the picker. */
object IntensityPalette {
    val light = Color(0xFF7FC79B)
    val medium = Color(0xFFDBB974)
    val heavy = Color(0xFFD98A8A)

    /** [intensity] is 1..3; anything else falls back to medium. */
    fun color(intensity: Int): Color = when (intensity) {
        1 -> light
        3 -> heavy
        else -> medium
    }

    fun label(intensity: Int): String = when (intensity) {
        1 -> "Light"
        3 -> "Heavy"
        else -> "Medium"
    }
}

/**
 * A restrained halo painted behind a composable (clipped to its bounds). Used
 * sparingly — a soft breath of colour, never a neon bloom.
 */
fun Modifier.glow(
    color: Color,
    radius: Dp = 10.dp,
    alpha: Float = 0.25f,
): Modifier = drawBehind {
    val px = radius.toPx()
    if (px <= 0f || alpha <= 0f) return@drawBehind
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = px,
        ),
        radius = px,
        center = center,
    )
}

/**
 * A small accent orb — the colour identity of the app. Kept tiny and matte so
 * it reads as a considered accent, not a filled balloon.
 */
@Composable
fun GlowDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    haloAlpha: Float = 0.22f,
) {
    Box(modifier = modifier.size(size * 1.7f), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val px = size.toPx()
                    if (px > 0f && haloAlpha > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(color.copy(alpha = haloAlpha), Color.Transparent),
                                center = center,
                                radius = px,
                            ),
                            radius = px,
                            center = center,
                        )
                    }
                },
        )
        Box(
            modifier = Modifier
                .size(size)
                .background(color, CircleShape),
        )
    }
}

/** Small colored dot used inside chips and legends. */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

/**
 * Category as a tinted tag. A coloured dot plus plain text (the previous
 * treatment) disappeared into the panel; tinting the tag itself gives every
 * entry a scannable colour without adding chrome.
 */
@Composable
fun CategoryTag(category: Category, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = modifier
            .clip(shape)
            .background(category.color.copy(alpha = 0.14f))
            .border(1.dp, category.color.copy(alpha = 0.32f), shape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(category.color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = category.color,
        )
    }
}

/**
 * Intensity as a neutral tag: the colour lives in the dot, not the text, so it
 * never competes with the category tag next to it.
 */
@Composable
fun IntensityTag(intensity: Int, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(IntensityPalette.color(intensity), CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = IntensityPalette.label(intensity),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
