package com.tatara.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tatara.ui.theme.LocalMotif
import com.tatara.ui.theme.LocalThemeColors
import com.tatara.ui.theme.Motif
import kotlin.math.cos
import kotlin.math.sin

/**
 * §2.5 — the completion mark. Nothing in this app is a stock checkbox: each theme
 * stamps its own glyph, drawn on Canvas so it can carry the heat ramp and animate.
 * Blade = a tempered slash, Astra = an eight-pointed star, Tree = a leaf.
 *
 * Pending is an empty vessel — outline only, never a filled block that could be
 * mistaken for done. Completing runs a short scale-and-heat pass, and the caller
 * may hang an ember burst off the same tap.
 */
@Composable
fun MotifMark(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    frozen: Boolean = false,
) {
    val c = LocalThemeColors.current
    val motif = LocalMotif.current
    val progress = remember { Animatable(if (checked) 1f else 0f) }

    LaunchedEffect(checked) {
        progress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = tween(durationMillis = if (checked) 260 else 140, easing = FastOutSlowInEasing),
        )
    }

    Canvas(modifier = modifier.size(size).clickable(onClick = onClick)) {
        val p = progress.value
        val frameColor = when {
            frozen -> c.hairline
            p > 0f -> lerpColor(c.muted, c.glow, p)
            else -> c.muted
        }
        // The vessel: a hairline frame that warms as the mark lands.
        drawRoundedFrame(frameColor, alpha = if (p > 0f) 1f else 0.7f)
        if (frozen) {
            drawFrozenBar(c.cool)
            return@Canvas
        }
        if (p <= 0.01f) return@Canvas

        // Heat bloom behind the glyph — the mark arrives hot, then settles.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c.glow.copy(alpha = 0.34f * p), Color.Transparent),
                center = center,
                radius = this.size.minDimension * 0.62f,
            ),
            radius = this.size.minDimension * 0.62f,
            center = center,
        )
        when (motif) {
            Motif.BLADE -> drawBladeMark(c.hot, c.glow, p)
            Motif.ASTRA -> drawAstraMark(c.hot, c.glow, p)
            Motif.TREE -> drawTreeMark(c.hot, c.glow, p)
        }
    }
}

private fun DrawScope.drawRoundedFrame(color: Color, alpha: Float) {
    val inset = size.minDimension * 0.06f
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.12f),
        style = Stroke(width = size.minDimension * 0.055f),
    )
}

/** A frozen day reads as neutral: one held bar, no heat. */
private fun DrawScope.drawFrozenBar(color: Color) {
    val w = size.width
    val h = size.height
    drawLine(
        color = color,
        start = Offset(w * 0.3f, h * 0.5f),
        end = Offset(w * 0.7f, h * 0.5f),
        strokeWidth = size.minDimension * 0.09f,
    )
}

/** Nie — the tempered slash: a stroke with a drawn tip, like a kissaki. */
private fun DrawScope.drawBladeMark(hot: Color, glow: Color, p: Float) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.24f, h * 0.54f)
        lineTo(w * 0.43f, h * 0.72f)
        lineTo(w * 0.78f, h * 0.28f)
    }
    // Under-stroke carries the working heat; the top stroke is the white edge.
    drawPath(path, glow.copy(alpha = 0.85f * p), style = Stroke(width = size.minDimension * 0.17f))
    drawPath(path, hot.copy(alpha = p), style = Stroke(width = size.minDimension * 0.085f))
}

/** Astra — an eight-pointed star: four long rays, four short. */
private fun DrawScope.drawAstraMark(hot: Color, glow: Color, p: Float) {
    val r = size.minDimension * 0.34f * p
    repeat(8) { i ->
        val angle = i * (Math.PI / 4).toFloat()
        val len = if (i % 2 == 0) r else r * 0.5f
        val end = Offset(center.x + cos(angle) * len, center.y + sin(angle) * len)
        drawLine(
            color = glow.copy(alpha = 0.9f * p),
            start = center, end = end,
            strokeWidth = size.minDimension * 0.11f,
        )
        drawLine(
            color = hot.copy(alpha = p),
            start = center, end = end,
            strokeWidth = size.minDimension * 0.05f,
        )
    }
    drawCircle(hot.copy(alpha = p), radius = size.minDimension * 0.07f, center = center)
}

/** Saṃskāra — a leaf: two mirrored curves with a midrib. */
private fun DrawScope.drawTreeMark(hot: Color, glow: Color, p: Float) {
    val w = size.width
    val h = size.height
    // Pointed at both ends and narrow through the middle — at 26dp a fat almond
    // reads as a blob, so the silhouette has to be unmistakably a leaf.
    rotate(degrees = -12f) {
        val leaf = Path().apply {
            moveTo(w * 0.5f, h * 0.16f)
            cubicTo(w * 0.76f, h * 0.34f, w * 0.76f, h * 0.62f, w * 0.5f, h * 0.84f)
            cubicTo(w * 0.24f, h * 0.62f, w * 0.24f, h * 0.34f, w * 0.5f, h * 0.16f)
            close()
        }
        drawPath(leaf, glow.copy(alpha = 0.85f * p))
        drawPath(leaf, hot.copy(alpha = 0.95f * p), style = Stroke(width = size.minDimension * 0.045f))
        // Midrib runs the full length, stopping just short of each point.
        drawLine(
            color = hot.copy(alpha = 0.9f * p),
            start = Offset(w * 0.5f, h * 0.78f),
            end = Offset(w * 0.5f, h * 0.22f),
            strokeWidth = size.minDimension * 0.04f,
        )
    }
}

/** Small section ornament, themed — replaces the bare 1px rule where it earns it. */
@Composable
fun MotifRule(modifier: Modifier = Modifier) {
    val c = LocalThemeColors.current
    Canvas(modifier = modifier.size(width = 0.dp, height = 1.dp)) {
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(c.hairline, c.hairline.copy(alpha = 0.15f)),
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = size.height,
        )
    }
}

internal fun lerpColor(from: Color, to: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * k,
        green = from.green + (to.green - from.green) * k,
        blue = from.blue + (to.blue - from.blue) * k,
        alpha = from.alpha + (to.alpha - from.alpha) * k,
    )
}
