package com.tatara.ui.hero.samskara

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import com.tatara.ui.theme.LocalThemeColors

/** Slow fog bands. The transition exists only while this world is composed. */
@Composable
internal fun SamskaraFog(modifier: Modifier = Modifier) {
    val colors = LocalThemeColors.current
    val transition = rememberInfiniteTransition(label = "samskaraFog")
    val drift by transition.animateFloat(
        initialValue = -0.08f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fogDrift",
    )

    Canvas(modifier = modifier) {
        fun fogBand(y: Float, width: Float, height: Float, offset: Float, alpha: Float) {
            val left = size.width * (offset + drift) - width * 0.5f
            val center = Offset(left + width / 2f, y + height / 2f)
            scale(1f, height / width, center) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.edge.copy(alpha = alpha), Color.Transparent),
                        center = center,
                        radius = width / 2f,
                    ),
                    radius = width / 2f,
                    center = center,
                )
            }
        }
        fogBand(size.height * 0.46f, size.width * 0.92f, size.height * 0.10f, 0.30f, 0.055f)
        fogBand(size.height * 0.65f, size.width * 1.12f, size.height * 0.13f, 0.72f, 0.040f)
        fogBand(size.height * 0.79f, size.width * 0.88f, size.height * 0.08f, 0.42f, 0.035f)
    }
}

/** Restrained aura whose strength responds naturally to today's completion. */
@Composable
internal fun SamskaraGlow(
    dailyCompletion: Float,
    breath: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalThemeColors.current
    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.5f, size.height * 0.62f)
        val radius = size.minDimension * 0.35f
        val strength = (0.17f + dailyCompletion * 0.10f) * breath
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.glow.copy(alpha = strength),
                    colors.ember.copy(alpha = strength * 0.44f),
                    Color.Transparent,
                ),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}
