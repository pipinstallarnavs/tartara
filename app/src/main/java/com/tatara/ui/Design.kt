package com.tatara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.ui.theme.LocalMotif
import com.tatara.ui.theme.LocalThemeColors
import com.tatara.ui.theme.Motif

/**
 * §Design — the elevation scale. Level 0 is the page (ground). Level 1 is a
 * surface that groups related controls. Level 2 (surfaceHigh) is reserved for
 * something that must read as sitting *above* its neighbours.
 *
 * Screens that stack unrelated tools use Panel so each tool is a distinct object
 * rather than a run of text separated by hairlines.
 */
@Composable
fun Panel(
    title: String? = null,
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalThemeColors.current
    Column(modifier = modifier.padding(top = 16.dp)) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MotifGlyph(size = 9.dp)
                Spacer(modifier = Modifier.size(6.dp))
                Text(title, color = c.muted, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (elevated) c.surfaceHigh else c.surface,
                    RoundedCornerShape(3.dp),
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            content = content,
        )
    }
}

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
        }
    }
}

/**
 * Empty states carry the fiction rather than apologising. Cold steel waiting for
 * the fire; an unstrung bow; bare ground before planting. Terse by design.
 */
@Composable
fun EmptyState(kind: EmptyKind, modifier: Modifier = Modifier) {
    val c = LocalThemeColors.current
    val motif = LocalMotif.current
    val (line, hint) = copyFor(motif, kind)
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MotifGlyph(size = 16.dp)
        Text(line, color = c.muted, fontSize = 14.sp)
        Text(hint, color = c.muted.copy(alpha = 0.65f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

enum class EmptyKind { FOOD, ROUTINES, TRAIN_HISTORY, HABITS }

private fun copyFor(motif: Motif, kind: EmptyKind): Pair<String, String> = when (motif) {
    Motif.BLADE -> when (kind) {
        EmptyKind.FOOD -> "Cold forge." to "nothing logged today"
        EmptyKind.ROUTINES -> "No pattern set." to "name a routine to begin"
        EmptyKind.TRAIN_HISTORY -> "The hammer is idle." to "no sessions yet"
        EmptyKind.HABITS -> "Unworked steel." to "no habits yet"
    }
    Motif.ASTRA -> when (kind) {
        EmptyKind.FOOD -> "The quiver is empty." to "nothing logged today"
        EmptyKind.ROUTINES -> "No rite inscribed." to "name a routine to begin"
        EmptyKind.TRAIN_HISTORY -> "Bow unstrung." to "no sessions yet"
        EmptyKind.HABITS -> "No vow taken." to "no habits yet"
    }
    Motif.TREE -> when (kind) {
        EmptyKind.FOOD -> "Bare ground." to "nothing logged today"
        EmptyKind.ROUTINES -> "Nothing planted." to "name a routine to begin"
        EmptyKind.TRAIN_HISTORY -> "No season behind you." to "no sessions yet"
        EmptyKind.HABITS -> "Untilled." to "no habits yet"
    }
}
