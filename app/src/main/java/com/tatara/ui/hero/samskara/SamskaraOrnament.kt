package com.tatara.ui.hero.samskara

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tatara.ui.theme.LocalThemeColors
import kotlin.math.cos
import kotlin.math.sin

/** Decorative only; tier titles and progress are always live Compose UI. */
@Composable
internal fun SamskaraOrnament(modifier: Modifier = Modifier) {
    val c = LocalThemeColors.current
    Canvas(modifier) {
        val center = Offset(size.width * 0.5f, size.height * 0.39f)
        val radius = size.width * 0.34f
        drawCircle(c.cool.copy(alpha = 0.22f), radius, center, style = Stroke(0.65.dp.toPx()))
        drawCircle(c.edge.copy(alpha = 0.09f), radius + 4.dp.toPx(), center, style = Stroke(0.4.dp.toPx()))
        repeat(12) { i ->
            val angle = i * Math.PI.toFloat() / 6f
            val direction = Offset(cos(angle), sin(angle))
            drawLine(c.edge.copy(alpha = 0.23f), center + direction * (radius - 2.dp.toPx()),
                center + direction * (radius + 2.dp.toPx()), 0.6.dp.toPx())
        }
        val top = Offset(center.x, center.y - radius)
        val diamond = Path().apply {
            moveTo(top.x, top.y - 9.dp.toPx())
            lineTo(top.x + 4.dp.toPx(), top.y)
            lineTo(top.x, top.y + 9.dp.toPx())
            lineTo(top.x - 4.dp.toPx(), top.y)
            close()
        }
        drawPath(diamond, c.edge.copy(alpha = 0.42f), style = Stroke(0.7.dp.toPx()))
    }
}
