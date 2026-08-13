package com.tatara.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.tatara.ui.theme.LocalThemeColors
import com.tatara.ui.theme.Motif
import com.tatara.ui.theme.ThemeColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * §2.5/§7.1 — the hero. One object, two readings: the *finish* is the tier
 * (band 0–8, rough billet → finished treasure), the *heat* is today (0–1, how
 * much of the day's work has landed). Heat is the reward language — the object
 * literally cools when you are behind and runs bright when you are on pace.
 *
 * Each theme draws its own: Nie forges a katana, Astra looses a divine arrow,
 * Saṃskāra grows a tree. All three are built from real geometry (curvature,
 * taper, layered gradients, rim light) rather than a flat silhouette, because
 * this is the strongest screen in the app and it should carry the most craft.
 */
@Composable
fun Hero(motif: Motif, band: Int, heat: Float, modifier: Modifier = Modifier) {
    val c = LocalThemeColors.current
    val refinement = (band / 8f).coerceIn(0f, 1f)
    val target = heat.coerceIn(0f, 1f)

    // Heat eases in rather than snapping — steel takes time to take colour.
    val animatedHeat = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animatedHeat.animateTo(target, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }

    // A slow breath on the glow so a lit object never looks like a static image.
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathValue",
    )

    Canvas(modifier = modifier.size(width = 150.dp, height = 330.dp)) {
        val h = animatedHeat.value
        when (motif) {
            Motif.BLADE -> drawKatana(c, refinement, h, breath)
            Motif.ASTRA -> drawAstra(c, refinement, h, breath)
            Motif.TREE -> drawTree(c, refinement, h, breath)
        }
    }
}

// ---------------------------------------------------------------- Nie: katana

/**
 * Built the way a blade is: tsuka (handle) → tsuba (guard) → habaki (collar) →
 * blade with sori (curvature), shinogi (ridge), and a kissaki (tip) closed by a
 * yokote line. The hamon is a real wavy boundary between hardened edge steel and
 * the softer spine, not a straight rule.
 */
private fun DrawScope.drawKatana(c: ThemeColors, refinement: Float, heat: Float, breath: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f

    val tipY = h * 0.05f
    val habakiY = h * 0.665f
    val tsubaY = h * 0.70f
    val buttY = h * 0.97f

    // Sori: the spine bows away from the edge. Deeper as the smith gets better.
    val sori = w * (0.020f + 0.055f * refinement)
    // A real katana is slender: a rough billet is only slightly heavier than a
    // finished blade, and the difference reads through surface, not bulk.
    val halfW = w * (0.070f - 0.016f * refinement)
    // Low tiers carry hammer texture — fine, high-frequency pitting, not waves.
    val rough = w * 0.0035f * (1f - refinement)

    fun t(y: Float) = ((y - tipY) / (habakiY - tipY)).coerceIn(0f, 1f)
    fun centre(y: Float): Float {
        val k = t(y)
        return cx + sori * sin(k * PI.toFloat() * 0.72f)
    }
    // The kissaki comes to a true point: width climbs steeply over the first
    // tenth, then holds nearly constant with a gentle taper toward the habaki.
    fun width(y: Float): Float {
        val k = t(y)
        val kissaki = (k / 0.11f).coerceIn(0f, 1f).pow(0.62f)
        val body = 0.86f + 0.14f * k
        val texture = rough * (sin(y / h * 61f) + 0.5f * sin(y / h * 143f))
        return (halfW * kissaki * body + texture).coerceAtLeast(0f)
    }

    val step = h / 150f

    // ---- silhouette ----------------------------------------------------------
    val blade = Path().apply {
        moveTo(centre(tipY), tipY)
        var y = tipY
        while (y < habakiY) { lineTo(centre(y) + width(y), y); y += step }
        lineTo(centre(habakiY) + width(habakiY), habakiY)
        y = habakiY
        while (y > tipY) { lineTo(centre(y) - width(y), y); y -= step }
        close()
    }

    // ---- forge glow: hugs the steel rather than washing the frame. Several
    // small blooms stacked along the blade read as light coming off the metal.
    if (heat > 0.01f) {
        val bloom = (0.20f + 0.42f * heat) * breath
        var y = tipY
        while (y < habakiY) {
            val k = t(y)
            // Heat pools toward the base, where the billet is thickest.
            val local = bloom * (0.45f + 0.55f * k)
            val r = w * 0.20f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.glow.copy(alpha = local * 0.5f), Color.Transparent),
                    center = Offset(centre(y) + width(y) * 0.35f, y),
                    radius = r,
                ),
                radius = r,
                center = Offset(centre(y) + width(y) * 0.35f, y),
            )
            y += h * 0.028f
        }
    }

    // ---- the steel body ------------------------------------------------------
    // Across the blade: dark mune (spine) → bright ji → brightest ha (edge).
    drawPath(
        blade,
        Brush.horizontalGradient(
            // Spine side stays dark so the hamon boundary reads as a real edge.
            colorStops = arrayOf(
                0.00f to c.ground,
                0.22f to c.surface,
                0.52f to lerpColor(c.surface, c.hairline, 0.75f),
                0.80f to lerpColor(c.hairline, c.muted, 0.35f),
                1.00f to c.ground,
            ),
            startX = cx - halfW * 1.15f,
            endX = cx + halfW * 1.15f,
        ),
    )

    clipPath(blade) {
        // Hardened edge steel, warmed by whatever heat is in the billet.
        val hamon = Path().apply {
            moveTo(cx + w * 0.5f, tipY)
            var y = tipY
            while (y < habakiY) {
                val k = t(y)
                val wave = if (band(refinement) >= 4) sin(y / h * 15f) * w * 0.030f else 0f
                val drift = sin(y / h * 5.5f) * w * 0.014f
                lineTo(centre(y) + width(y) * (0.02f + 0.30f * k) + wave + drift, y)
                y += step
            }
            lineTo(cx + w * 0.5f, habakiY)
            close()
        }
        // The hardened edge steel is the brightest plane on the blade — this
        // boundary is the whole point of a hamon, so it gets real contrast.
        drawPath(
            hamon,
            Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to lerpColor(c.muted, c.primary, 0.45f + 0.30f * refinement),
                    0.6f to lerpColor(c.primary, c.edge, 0.35f + 0.40f * refinement),
                    1.0f to c.edge,
                ),
                startX = cx - halfW, endX = cx + halfW * 1.3f,
            ),
        )
        // A soft nioi band traces just inside the boundary.
        drawPath(
            hamon,
            c.hot.copy(alpha = 0.10f + 0.20f * refinement),
            style = Stroke(width = w * 0.012f),
        )
        // Heat: the whole billet takes colour, but the hardened steel takes it
        // hardest and holds it longest. This is the reward signal — at full heat
        // the blade should read unmistakably as metal that has been in a fire.
        if (heat > 0.01f) {
            drawPath(
                blade,
                Brush.verticalGradient(
                    colors = listOf(
                        c.ember.copy(alpha = 0.30f * heat),
                        c.glow.copy(alpha = 0.55f * heat * breath),
                    ),
                    startY = tipY, endY = habakiY,
                ),
            )
            drawPath(
                hamon,
                Brush.verticalGradient(
                    colors = listOf(
                        lerpColor(c.glow, c.hot, 0.35f).copy(alpha = 0.62f * heat * breath),
                        c.glow.copy(alpha = 0.70f * heat),
                    ),
                    startY = tipY, endY = habakiY,
                ),
            )
        }
        // Nie/nioi: the crystalline sparkle scattered along the hamon.
        if (refinement > 0.3f) {
            var y = tipY + h * 0.02f
            var i = 0
            while (y < habakiY) {
                val k = t(y)
                val x = centre(y) + width(y) * (0.10f + 0.26f * k) + sin(y / h * 15f) * w * 0.030f
                if (i % 3 == 0) {
                    drawCircle(
                        color = c.hot.copy(alpha = (0.10f + 0.34f * heat) * refinement),
                        radius = w * 0.007f,
                        center = Offset(x, y),
                    )
                }
                y += h * 0.014f; i++
            }
        }
        // Shinogi: the ridge line between spine and edge planes.
        val ridge = Path().apply {
            var y = tipY
            var first = true
            while (y < habakiY) {
                val x = centre(y) - width(y) * 0.34f
                if (first) { moveTo(x, y); first = false } else lineTo(x, y)
                y += step
            }
        }
        drawPath(ridge, c.primary.copy(alpha = 0.10f + 0.16f * refinement), style = Stroke(width = w * 0.010f))
    }

    // ---- rim light: the single brightest line, along the cutting edge --------
    val edgeLine = Path().apply {
        var y = tipY
        var first = true
        while (y < habakiY) {
            val x = centre(y) + width(y)
            if (first) { moveTo(x, y); first = false } else lineTo(x, y)
            y += step
        }
    }
    drawPath(
        edgeLine,
        Brush.verticalGradient(
            colors = listOf(
                c.edge.copy(alpha = 0.30f + 0.55f * refinement),
                lerpColor(c.edge, c.hot, heat).copy(alpha = 0.45f + 0.45f * heat),
            ),
            startY = tipY, endY = habakiY,
        ),
        style = Stroke(width = w * (0.011f + 0.008f * refinement)),
    )

    // Yokote: the hard line closing the tip. Only a finished blade has one.
    if (refinement > 0.35f) {
        val yy = tipY + (habakiY - tipY) * 0.115f
        drawLine(
            color = c.primary.copy(alpha = 0.30f + 0.40f * refinement),
            start = Offset(centre(yy) - width(yy) * 0.34f, yy),
            end = Offset(centre(yy) + width(yy), yy),
            strokeWidth = w * 0.009f,
        )
    }

    // ---- habaki, tsuba, tsuka ------------------------------------------------
    val hx = centre(habakiY)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(c.muted.copy(alpha = 0.55f), lerpColor(c.muted, c.primary, 0.5f), c.muted.copy(alpha = 0.55f)),
        ),
        topLeft = Offset(hx - halfW * 1.05f, habakiY),
        size = androidx.compose.ui.geometry.Size(halfW * 2.1f, h * 0.028f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.006f),
    )

    // Tsuba appears once the blade is worth mounting.
    if (refinement > 0.12f) {
        val guardW = w * (0.16f + 0.16f * refinement)
        val guardH = h * 0.016f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(lerpColor(c.muted, c.primary, 0.35f), c.surface)),
            topLeft = Offset(cx - guardW, tsubaY - guardH * 0.5f),
            size = androidx.compose.ui.geometry.Size(guardW * 2f, guardH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(guardH * 0.5f),
        )
    }

    val tsukaW = halfW * 0.92f
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(c.ground, c.surface, c.ground)),
        topLeft = Offset(cx - tsukaW, tsubaY + h * 0.010f),
        size = androidx.compose.ui.geometry.Size(tsukaW * 2f, buttY - tsubaY - h * 0.010f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.012f),
    )
    // Tsuka-ito: the diamond wrap. Tighter and more regular at higher tiers.
    val wraps = 5 + (refinement * 5).toInt()
    val top = tsubaY + h * 0.016f
    val span = (buttY - top) / wraps
    repeat(wraps) { i ->
        val y0 = top + i * span
        val alpha = 0.30f + 0.34f * refinement
        drawLine(c.muted.copy(alpha = alpha), Offset(cx - tsukaW, y0), Offset(cx + tsukaW, y0 + span * 0.62f), w * 0.011f)
        drawLine(c.muted.copy(alpha = alpha), Offset(cx + tsukaW, y0), Offset(cx - tsukaW, y0 + span * 0.62f), w * 0.011f)
    }

    // Mei: the smith's signature, cut into the tang. Earned at band 6.
    if (refinement > 0.72f) {
        var y = tsubaY + h * 0.055f
        repeat(3) {
            drawLine(
                color = c.primary.copy(alpha = 0.45f),
                start = Offset(cx - w * 0.022f, y),
                end = Offset(cx + w * 0.022f, y + h * 0.012f),
                strokeWidth = w * 0.007f,
            )
            y += h * 0.030f
        }
    }
}

private fun band(refinement: Float): Int = (refinement * 8f).toInt()

// ------------------------------------------------------------- Astra: the arrow

/** A celestial arrow: ornate head, bound shaft, fletching, and an aura that turns. */
private fun DrawScope.drawAstra(c: ThemeColors, refinement: Float, heat: Float, breath: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val tipY = h * 0.045f
    val headBase = h * 0.30f
    val shaftEnd = h * 0.86f

    // Aura: concentric rings, brighter and more numerous as the astra awakens.
    if (heat > 0.01f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c.glow.copy(alpha = 0.26f * heat * breath), Color.Transparent),
                center = Offset(cx, h * 0.26f),
                radius = w * 0.92f,
            ),
            radius = w * 0.92f,
            center = Offset(cx, h * 0.26f),
        )
        val rings = 1 + (refinement * 3).toInt()
        repeat(rings) { i ->
            val r = w * (0.26f + 0.13f * i) * breath
            drawCircle(
                color = c.cool.copy(alpha = (0.20f - 0.04f * i) * heat),
                radius = r,
                center = Offset(cx, h * 0.26f),
                style = Stroke(width = w * 0.006f),
            )
        }
    }

    // Head: a leaf blade with a raised central ridge.
    val headW = w * (0.15f + 0.07f * refinement)
    val head = Path().apply {
        moveTo(cx, tipY)
        cubicTo(cx + headW * 1.15f, h * 0.13f, cx + headW, h * 0.24f, cx + headW * 0.30f, headBase)
        lineTo(cx - headW * 0.30f, headBase)
        cubicTo(cx - headW, h * 0.24f, cx - headW * 1.15f, h * 0.13f, cx, tipY)
        close()
    }
    drawPath(
        head,
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to c.surface,
                0.42f to lerpColor(c.hairline, c.primary, 0.30f + 0.30f * refinement),
                0.58f to lerpColor(c.primary, c.hot, heat * 0.5f),
                1f to c.surface,
            ),
            startX = cx - headW, endX = cx + headW,
        ),
    )
    drawPath(head, c.edge.copy(alpha = 0.35f + 0.45f * refinement), style = Stroke(width = w * 0.010f))
    drawLine(
        color = lerpColor(c.edge, c.hot, heat).copy(alpha = 0.5f + 0.4f * heat),
        start = Offset(cx, tipY + h * 0.012f), end = Offset(cx, headBase),
        strokeWidth = w * 0.012f,
    )

    // Shaft with metal bindings.
    val shaftW = w * 0.024f
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(c.surface, lerpColor(c.muted, c.primary, 0.25f), c.surface)),
        topLeft = Offset(cx - shaftW, headBase),
        size = androidx.compose.ui.geometry.Size(shaftW * 2f, shaftEnd - headBase),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(shaftW),
    )
    val bindings = 2 + (refinement * 3).toInt()
    repeat(bindings) { i ->
        val y = headBase + (shaftEnd - headBase) * (0.14f + 0.20f * i)
        drawRoundRect(
            color = lerpColor(c.muted, c.hot, heat * 0.4f).copy(alpha = 0.6f + 0.3f * refinement),
            topLeft = Offset(cx - shaftW * 2.1f, y),
            size = androidx.compose.ui.geometry.Size(shaftW * 4.2f, h * 0.011f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.004f),
        )
    }

    // Fletching: swept vanes with a cut trailing edge, not a blob. The outer
    // edge bows out; the trailing edge is notched back toward the nock.
    val fW = w * (0.085f + 0.045f * refinement)
    val fTop = shaftEnd - h * 0.15f
    val fBot = shaftEnd - h * 0.012f
    listOf(-1f, 1f).forEach { side ->
        val vane = Path().apply {
            moveTo(cx + side * shaftW * 0.9f, fTop)
            // Leading edge sweeps out to the widest point.
            cubicTo(
                cx + side * fW * 0.85f, fTop + h * 0.030f,
                cx + side * fW, fTop + h * 0.070f,
                cx + side * fW, fBot - h * 0.020f,
            )
            // Cut trailing edge, angled back in to the shaft.
            lineTo(cx + side * fW * 0.62f, fBot)
            lineTo(cx + side * shaftW * 0.9f, fBot - h * 0.045f)
            close()
        }
        drawPath(
            vane,
            Brush.horizontalGradient(
                colors = listOf(
                    lerpColor(c.cool, c.glow, heat * 0.5f).copy(alpha = 0.75f),
                    lerpColor(c.cool, c.ground, 0.45f).copy(alpha = 0.60f),
                ),
                startX = cx, endX = cx + side * fW,
            ),
        )
        drawPath(vane, c.edge.copy(alpha = 0.28f + 0.30f * refinement), style = Stroke(width = w * 0.005f))
        // Quill rib along the vane.
        drawLine(
            color = c.edge.copy(alpha = 0.22f),
            start = Offset(cx + side * shaftW * 1.2f, fTop + h * 0.012f),
            end = Offset(cx + side * fW * 0.72f, fBot - h * 0.020f),
            strokeWidth = w * 0.004f,
        )
    }
    // Nock at the very end.
    drawRoundRect(
        color = lerpColor(c.muted, c.primary, 0.30f),
        topLeft = Offset(cx - shaftW * 1.5f, shaftEnd - h * 0.010f),
        size = androidx.compose.ui.geometry.Size(shaftW * 3f, h * 0.020f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.005f),
    )

    // Orbiting motes — the weapon is awake.
    if (heat > 0.2f) {
        repeat(5) { i ->
            val a = (i / 5f) * 2f * PI.toFloat() + breath * 2.2f
            val r = w * 0.34f * breath
            drawCircle(
                color = c.hot.copy(alpha = 0.30f + 0.45f * heat),
                radius = w * 0.011f,
                center = Offset(cx + cos(a) * r, h * 0.26f + sin(a) * r * 0.55f),
            )
        }
    }
}

// -------------------------------------------------------------- Saṃskāra: tree

/** A tree grown by practice: root flare, tapering trunk, recursive branching, canopy. */
private fun DrawScope.drawTree(c: ThemeColors, refinement: Float, heat: Float, breath: Float) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val groundY = h * 0.94f
    // A sapling is short with a small crown; a mature tree is taller but gives
    // most of its height to canopy, not trunk. Trunk never exceeds ~40% of frame.
    val trunkTop = h * (0.72f - 0.16f * refinement)

    // Warm light through the canopy.
    if (heat > 0.01f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c.glow.copy(alpha = 0.24f * heat * breath), Color.Transparent),
                center = Offset(cx, h * 0.34f),
                radius = w * 0.95f,
            ),
            radius = w * 0.95f,
            center = Offset(cx, h * 0.34f),
        )
    }

    // Root flare.
    listOf(-1f, 1f).forEach { side ->
        val root = Path().apply {
            moveTo(cx + side * w * 0.030f, groundY - h * 0.05f)
            quadraticBezierTo(
                cx + side * w * 0.10f, groundY - h * 0.010f,
                cx + side * w * (0.16f + 0.06f * refinement), groundY,
            )
            lineTo(cx + side * w * 0.02f, groundY)
            close()
        }
        drawPath(root, c.surface)
    }

    // Trunk: tapered, slightly curved. Thick enough to carry the crown.
    val baseW = w * (0.080f + 0.045f * refinement)
    val midY = trunkTop + (groundY - trunkTop) * 0.45f
    val trunk = Path().apply {
        moveTo(cx - baseW, groundY)
        cubicTo(cx - baseW * 0.72f, midY, cx - baseW * 0.50f, midY, cx - baseW * 0.34f, trunkTop)
        lineTo(cx + baseW * 0.34f, trunkTop)
        cubicTo(cx + baseW * 0.50f, midY, cx + baseW * 0.72f, midY, cx + baseW, groundY)
        close()
    }
    // Bark must read against the ground even with no heat at all — a cold tree
    // is still a tree. The lit face carries most of the value range.
    drawPath(
        trunk,
        Brush.horizontalGradient(
            listOf(
                c.ground,
                lerpColor(c.surface, c.muted, 0.30f),
                lerpColor(c.surface, c.muted, 0.72f),
                lerpColor(c.surface, c.muted, 0.24f),
                c.ground,
            ),
        ),
    )
    drawPath(trunk, c.edge.copy(alpha = 0.22f + 0.20f * refinement), style = Stroke(width = w * 0.006f))

    // Branches: recursive forks, depth grows with the tier.
    val depth = 2 + (refinement * 3).toInt()
    fun branch(x: Float, y: Float, angle: Float, len: Float, thick: Float, level: Int) {
        if (level > depth || len < h * 0.012f) return
        val ex = x + cos(angle) * len
        val ey = y - sin(angle) * len
        drawLine(
            color = lerpColor(c.surface, c.muted, 0.55f + 0.10f * level),
            start = Offset(x, y), end = Offset(ex, ey),
            strokeWidth = thick,
        )
        if (level >= depth - 1) {
            // Canopy: clustered foliage, varied in size so it reads as leaves
            // rather than lollipops. Lit clusters warm with the day's heat.
            val lit = heat > (level - 1) / (depth + 1f)
            val base = w * (0.050f + 0.028f * refinement)
            val jitter = sin(ex * 3.1f + ey * 1.7f)
            repeat(3) { j ->
                val ox = ex + jitter * w * 0.035f * (j - 1)
                val oy = ey + cos(ex * 2.3f + j) * h * 0.012f
                // Unlit foliage is dormant, not absent: it holds a cool muted
                // tone so the crown still has mass before any heat arrives.
                drawCircle(
                    color = if (lit) lerpColor(c.cool, c.glow, heat * 0.45f)
                        .copy(alpha = 0.34f + 0.30f * heat)
                    else lerpColor(c.surface, c.muted, 0.55f).copy(alpha = 0.70f),
                    radius = base * (0.75f + 0.35f * ((j + jitter) % 1f)) * (if (lit) breath else 1f),
                    center = Offset(ox, oy),
                )
            }
        }
        // Branches converge as they rise — successive forks open less than their
        // parent, which both reads as a real crown and keeps it inside the frame.
        val spread = (0.50f - 0.10f * refinement) * 0.78f.pow(level - 1)
        val wobble = sin(x * 1.9f + y * 0.7f) * 0.08f
        branch(ex, ey, angle + spread + wobble, len * 0.72f, thick * 0.68f, level + 1)
        branch(ex, ey, angle - spread + wobble, len * 0.72f, thick * 0.68f, level + 1)
    }
    val trunkThick = w * (0.050f + 0.028f * refinement)
    branch(cx, trunkTop, (PI / 2).toFloat(), h * (0.080f + 0.030f * refinement), trunkThick, 1)

    // Ground line.
    drawLine(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, c.hairline, Color.Transparent)),
        start = Offset(w * 0.08f, groundY),
        end = Offset(w * 0.92f, groundY),
        strokeWidth = h * 0.004f,
    )
}
