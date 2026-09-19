package com.zhiend.regretnote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private const val STAR_COUNT = 38

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
)

/**
 * The backdrop: a near-black wash, two very soft blooms and a faint star field.
 * Deliberately static — it draws once per size change instead of redrawing every
 * frame, keeping battery and jank in check.
 *
 * The earlier version ran 64 stars at up to 28% alpha and three blooms; on a
 * real device that reads as sensor noise rather than night sky, so both counts
 * and alphas came down. Every colour is derived from the active scheme (the
 * middle of the wash used to be a hardcoded hex, which is why the premium theme
 * looked slightly wrong at the top of the screen).
 */
@Composable
fun CosmicBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        val random = Random(1337)
        List(STAR_COUNT) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.4f + random.nextFloat() * 0.8f,
                alpha = 0.04f + random.nextFloat() * 0.11f,
            )
        }
    }

    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val starTint = Color(0xD0CEDC)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    background,
                    lerp(background, Color.Black, 0.20f),
                    background,
                ),
            ),
        )

        bloom(primary, 0.075f, Offset(w * 0.12f, h * 0.14f), w * 0.58f)
        bloom(secondary, 0.035f, Offset(w * 0.86f, h * 0.78f), w * 0.48f)

        stars.forEach { star ->
            drawCircle(
                color = starTint.copy(alpha = star.alpha),
                radius = star.radius.dp.toPx(),
                center = Offset(star.x * w, star.y * h),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.bloom(
    color: Color,
    alpha: Float,
    center: Offset,
    radius: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.3f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
