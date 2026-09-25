package com.tatara.ui.hero.samskara

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.tatara.ui.theme.LocalThemeColors

/** Static, inexpensive environment layer: moon, forest depth, forest floor. */
@Composable
internal fun SamskaraBackground(modifier: Modifier = Modifier) {
    val colors = LocalThemeColors.current
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.ground, colors.surface, colors.ground),
                startY = 0f,
                endY = size.height,
            ),
        )

        val moonCenter = Offset(size.width * 0.72f, size.height * 0.18f)
        val moonRadius = size.minDimension * 0.36f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    colors.hot.copy(alpha = 0.13f),
                    colors.edge.copy(alpha = 0.035f),
                    Color.Transparent,
                ),
                center = moonCenter,
                radius = moonRadius,
            ),
            radius = moonRadius,
            center = moonCenter,
        )
        drawCircle(
            color = colors.hot.copy(alpha = 0.055f),
            radius = size.minDimension * 0.075f,
            center = moonCenter,
        )

        drawForestRidge(
            horizon = size.height * 0.58f,
            peak = size.height * 0.26f,
            color = colors.surfaceHigh.copy(alpha = 0.48f),
            phase = 0.17f,
        )
        drawForestRidge(
            horizon = size.height * 0.72f,
            peak = size.height * 0.39f,
            color = colors.ground.copy(alpha = 0.82f),
            phase = 0.61f,
        )

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(colors.ember.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.84f),
                radius = size.width * 0.58f,
            ),
            topLeft = Offset(-size.width * 0.08f, size.height * 0.71f),
            size = androidx.compose.ui.geometry.Size(size.width * 1.16f, size.height * 0.25f),
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, colors.ground.copy(alpha = 0.74f)),
                center = Offset(size.width * 0.5f, size.height * 0.48f),
                radius = size.maxDimension * 0.68f,
            ),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawForestRidge(
    horizon: Float,
    peak: Float,
    color: Color,
    phase: Float,
) {
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, horizon)
        val treeCount = 11
        repeat(treeCount + 1) { index ->
            val x = size.width * index / treeCount
            val variation = ((index * 37 + phase * 100f).toInt() % 5) / 5f
            val crownY = peak + (horizon - peak) * (0.28f + variation * 0.52f)
            lineTo(x - size.width * 0.045f, horizon)
            lineTo(x, crownY)
            lineTo(x + size.width * 0.048f, horizon)
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}
