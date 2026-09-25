package com.tatara.ui.hero.samskara

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.tatara.ui.theme.LocalThemeColors
import kotlin.math.sin

private data class Mote(
    val x: Float,
    val y: Float,
    val speed: Float,
    val phase: Float,
    val radius: Float,
)

private val motes = listOf(
    Mote(0.30f, 0.71f, 0.13f, 0.07f, 0.0050f),
    Mote(0.42f, 0.59f, 0.10f, 0.44f, 0.0035f),
    Mote(0.57f, 0.68f, 0.15f, 0.76f, 0.0045f),
    Mote(0.68f, 0.55f, 0.09f, 0.28f, 0.0030f),
    Mote(0.36f, 0.48f, 0.08f, 0.63f, 0.0032f),
    Mote(0.61f, 0.43f, 0.12f, 0.15f, 0.0038f),
    Mote(0.49f, 0.76f, 0.11f, 0.91f, 0.0028f),
)

/** A small, fixed particle budget: motes plus two occasional leaf-like flecks. */
@Composable
internal fun SamskaraParticles(
    dailyCompletion: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalThemeColors.current
    val transition = rememberInfiniteTransition(label = "samskaraParticles")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
        ),
        label = "particleTime",
    )

    Canvas(modifier = modifier) {
        val visibleMotes = 4 + (dailyCompletion * 3f).toInt()
        motes.take(visibleMotes).forEach { mote ->
            val cycle = (time * mote.speed * 4f + mote.phase) % 1f
            val sway = sin((time + mote.phase) * 6.283f) * size.width * 0.018f
            val center = Offset(
                x = size.width * mote.x + sway,
                y = size.height * (mote.y - cycle * 0.20f),
            )
            val radius = size.minDimension * mote.radius
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.hot.copy(alpha = 0.42f),
                        colors.glow.copy(alpha = 0.13f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius * 4f,
                ),
                radius = radius * 4f,
                center = center,
            )
            drawCircle(colors.edge.copy(alpha = 0.60f), radius = radius, center = center)
        }

        repeat(2) { index ->
            val phase = (time + index * 0.53f) % 1f
            if (phase in 0.12f..0.72f) {
                val x = size.width * (0.30f + index * 0.34f) +
                    sin(phase * 10f + index) * size.width * 0.035f
                val y = size.height * (0.44f + phase * 0.30f)
                rotate(degrees = phase * 150f + index * 35f, pivot = Offset(x, y)) {
                    drawOval(
                        color = colors.cool.copy(alpha = 0.20f),
                        topLeft = Offset(x - size.width * 0.009f, y - size.height * 0.004f),
                        size = Size(size.width * 0.018f, size.height * 0.008f),
                    )
                }
            }
        }
    }
}
