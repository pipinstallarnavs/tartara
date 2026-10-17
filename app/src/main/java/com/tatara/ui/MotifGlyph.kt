package com.tatara.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tatara.ui.theme.LocalMotif
import com.tatara.ui.theme.LocalThemeColors
import com.tatara.ui.theme.Motif


/** A small themed glyph used to head a section — never a stock bullet. */
@Composable
fun MotifGlyph(size: androidx.compose.ui.unit.Dp = 10.dp) {
    val c = LocalThemeColors.current
    val motif = LocalMotif.current
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension
        when (motif) {
            Motif.BLADE -> {
                // A struck spark: short diagonal with a bright head.
                drawLine(
                    color = c.glow.copy(alpha = 0.85f),
                    start = androidx.compose.ui.geometry.Offset(s * 0.1f, s * 0.85f),
                    end = androidx.compose.ui.geometry.Offset(s * 0.85f, s * 0.15f),
                    strokeWidth = s * 0.16f,
                )
                drawCircle(c.hot, radius = s * 0.14f, center = center)
            }
            Motif.ASTRA -> {
                repeat(4) { i ->
                    val a = i * (Math.PI / 4).toFloat()
                    drawLine(
                        color = c.cool.copy(alpha = 0.9f),
                        start = androidx.compose.ui.geometry.Offset(
                            center.x - kotlin.math.cos(a) * s * 0.45f,
                            center.y - kotlin.math.sin(a) * s * 0.45f,
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            center.x + kotlin.math.cos(a) * s * 0.45f,
                            center.y + kotlin.math.sin(a) * s * 0.45f,
                        ),
                        strokeWidth = s * 0.12f,
                    )
                }
            }
            Motif.TREE -> {
                drawCircle(c.cool.copy(alpha = 0.85f), radius = s * 0.30f, center = center)
                drawLine(
                    color = c.muted,
                    start = androidx.compose.ui.geometry.Offset(center.x, center.y + s * 0.28f),
                    end = androidx.compose.ui.geometry.Offset(center.x, s * 0.95f),
                    strokeWidth = s * 0.11f,
                )
            }
            Motif.ALCHEMY -> {
                drawCircle(c.glow, radius = s * 0.32f, style = androidx.compose.ui.graphics.drawscope.Stroke(s * 0.09f))
                drawCircle(c.cool, radius = s * 0.10f)
            }
            Motif.AXIOM -> {
                val points = listOf(
                    androidx.compose.ui.geometry.Offset(s * 0.5f, s * 0.08f),
                    androidx.compose.ui.geometry.Offset(s * 0.08f, s * 0.88f),
                    androidx.compose.ui.geometry.Offset(s * 0.92f, s * 0.88f),
                )
                points.indices.forEach { i ->
                    drawLine(c.glow, points[i], points[(i + 1) % points.size], strokeWidth = s * 0.09f)
                }
            }
            Motif.SIGNAL -> {
                val y = floatArrayOf(0.55f, 0.55f, 0.20f, 0.83f, 0.40f, 0.55f)
                for (i in 0 until y.lastIndex) {
                    drawLine(c.glow, androidx.compose.ui.geometry.Offset(s * i / y.lastIndex, s * y[i]),
                        androidx.compose.ui.geometry.Offset(s * (i + 1) / y.lastIndex, s * y[i + 1]), s * 0.1f)
                }
            }
            Motif.EDIFICE -> {
                drawLine(c.glow, androidx.compose.ui.geometry.Offset(s * 0.12f, s * 0.85f),
                    androidx.compose.ui.geometry.Offset(s * 0.88f, s * 0.85f), s * 0.12f)
                drawLine(c.glow, androidx.compose.ui.geometry.Offset(s * 0.27f, s * 0.85f),
                    androidx.compose.ui.geometry.Offset(s * 0.27f, s * 0.25f), s * 0.12f)
                drawLine(c.glow, androidx.compose.ui.geometry.Offset(s * 0.73f, s * 0.85f),
                    androidx.compose.ui.geometry.Offset(s * 0.73f, s * 0.25f), s * 0.12f)
                drawCircle(c.cool, s * 0.23f, center = androidx.compose.ui.geometry.Offset(s * 0.5f, s * 0.25f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(s * 0.09f))
            }
            Motif.VOYAGER -> {
                drawCircle(c.cool, s * 0.37f, style = androidx.compose.ui.graphics.drawscope.Stroke(s * 0.08f))
                drawLine(c.glow, androidx.compose.ui.geometry.Offset(s * 0.5f, s * 0.08f),
                    androidx.compose.ui.geometry.Offset(s * 0.5f, s * 0.92f), s * 0.09f)
                drawCircle(c.hot, s * 0.08f)
            }
        }
    }
}
