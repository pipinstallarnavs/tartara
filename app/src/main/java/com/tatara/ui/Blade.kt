package com.tatara.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.tatara.ui.theme.LocalThemeColors
import kotlin.math.sin

/**
 * §2.5/§7.1 — one blade, two readings. The silhouette's finish is the tier
 * (band 0–8): a rough lump at Tamahagane, sharpening through the bands, hamon
 * line from band 4, tang marks from band 6. The fill is today: the bright
 * region rises from the tip as the day's requirements complete (0–1).
 * Drawn as a Canvas path so both are parameters. This replaces the daily ring.
 */
@Composable
fun Blade(band: Int, fill: Float, modifier: Modifier = Modifier) {
    val c = LocalThemeColors.current
    Canvas(modifier = modifier.size(width = 90.dp, height = 320.dp)) {
        val w = size.width
        val h = size.height
        val tangTop = h * 0.84f
        val refinement = (band / 8f).coerceIn(0f, 1f)

        // Rough at low bands: wide, blunt; refined: narrow with a drawn tip.
        val halfWidth = w * (0.30f - 0.12f * refinement)
        val cx = w / 2f
        val tipY = h * (0.10f * (1f - refinement))
        val jag = w * 0.06f * (1f - refinement)

        fun edgeX(y: Float, side: Int): Float {
            val t = ((y - tipY) / (tangTop - tipY)).coerceIn(0f, 1f)
            // Taper toward the tip; slight curvature (sori) on the spine side.
            val width = halfWidth * (0.25f + 0.75f * t)
            val curve = if (side < 0) w * 0.03f * refinement * sin(t * 3.1f) else 0f
            val rough = jag * sin(y / h * 40f + side * 2f)
            return cx + side * width + curve + rough
        }

        val blade = Path().apply {
            moveTo(cx, tipY)
            var y = tipY
            while (y < tangTop) {
                lineTo(edgeX(y, 1), y)
                y += h / 40f
            }
            // Tang: a narrower rectangle below the blade proper.
            lineTo(cx + w * 0.12f, tangTop)
            lineTo(cx + w * 0.10f, h)
            lineTo(cx - w * 0.10f, h)
            lineTo(cx - w * 0.12f, tangTop)
            y = tangTop
            while (y > tipY) {
                lineTo(edgeX(y, -1), y)
                y -= h / 40f
            }
            close()
        }

        // Unearned steel.
        drawPath(blade, c.surface)

        // Earned region grows from the tip toward the tang (§7.1).
        val boundaryY = tipY + (tangTop - tipY) * fill.coerceIn(0f, 1f)
        if (fill > 0f) {
            clipPath(blade) {
                drawRect(
                    color = c.primary,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(w, boundaryY),
                )
            }
        }

        // The hamon: the moving line between the two steels. Wavy once the
        // temper line has emerged (band ≥ 4), straight before.
        if (fill > 0f && fill < 1f) {
            clipPath(blade) {
                val hamon = Path().apply {
                    moveTo(0f, boundaryY)
                    var x = 0f
                    while (x < w) {
                        val wave = if (band >= 4) h * 0.012f * sin(x / w * 12f) else 0f
                        lineTo(x, boundaryY + wave)
                        x += w / 24f
                    }
                    lineTo(w, boundaryY)
                }
                drawPath(hamon, c.cool, style = Stroke(width = 2.dp.toPx()))
            }
        }

        // Mei: the smith's signature on the tang, from band 6.
        if (band >= 6) {
            var y = tangTop + h * 0.03f
            repeat(3) {
                drawLine(
                    color = c.muted,
                    start = Offset(cx - w * 0.03f, y),
                    end = Offset(cx + w * 0.03f, y + h * 0.015f),
                    strokeWidth = 1.dp.toPx(),
                )
                y += h * 0.035f
            }
        }
    }
}
