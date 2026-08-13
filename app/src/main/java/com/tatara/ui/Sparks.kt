package com.tatara.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.ui.theme.LocalThemeColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * §7 — the per-action reward. Completing a set, a meal, or a habit throws a short
 * ember burst from the tap point: sparks rise, arc over, and cool to nothing in
 * well under a second. Physical and restrained — no confetti, no badges.
 *
 * Drive it by incrementing [key]; every change fires one burst.
 */
@Composable
fun EmberBurst(key: Int, modifier: Modifier = Modifier, count: Int = 12) {
    if (key <= 0) return
    val c = LocalThemeColors.current
    val progress = remember { Animatable(1f) }
    val sparks = remember(key) {
        List(count) {
            val angle = (-PI / 2 + (Random.nextFloat() - 0.5f) * 1.9f).toFloat()
            Spark(
                angle = angle,
                speed = 0.55f + Random.nextFloat() * 0.75f,
                size = 0.30f + Random.nextFloat() * 0.85f,
                spin = (Random.nextFloat() - 0.5f) * 0.9f,
            )
        }
    }

    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(620, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val origin = Offset(size.width * 0.5f, size.height * 0.5f)
        val reach = size.minDimension * 0.9f

        sparks.forEach { s ->
            // Ballistic: outward at speed, gravity pulling the arc over.
            val dist = s.speed * reach * t
            val gravity = reach * 0.85f * t * t
            val x = origin.x + cos(s.angle + s.spin * t) * dist
            val y = origin.y + sin(s.angle) * dist + gravity
            val fade = (1f - t).coerceIn(0f, 1f)
            // Sparks cool as they fly: hot → glow → ember.
            val colour = when {
                t < 0.30f -> lerpColor(c.hot, c.glow, t / 0.30f)
                else -> lerpColor(c.glow, c.ember, (t - 0.30f) / 0.70f)
            }
            drawCircle(
                color = colour.copy(alpha = fade * 0.95f),
                radius = size.minDimension * 0.026f * s.size * (0.45f + 0.55f * fade),
                center = Offset(x, y),
            )
        }
    }
}

private data class Spark(val angle: Float, val speed: Float, val size: Float, val spin: Float)

/**
 * Wraps content with an ember burst centred on it. The burst is drawn in an
 * overlay that does not take layout space or intercept touches.
 */
@Composable
fun WithEmbers(key: Int, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier) {
        content()
        EmberBurst(key = key, modifier = Modifier.fillMaxSize())
    }
}

/**
 * §7.3 — the rare one. Crossing into a new tier is a fold: the steel goes
 * white-hot, the screen holds its breath, and the object resolves brighter and
 * sharper than before. Because it happens a handful of times a year it is
 * allowed to be far bigger than anything else in the app.
 */
@Composable
fun FoldOverlay(tierName: String, onDone: () -> Unit) {
    val c = LocalThemeColors.current
    val t = remember { Animatable(0f) }

    LaunchedEffect(tierName) {
        t.snapTo(0f)
        t.animateTo(1f, animationSpec = tween(3200, easing = LinearEasing))
        onDone()
    }

    // Swallows taps for the duration so a stray touch can't fire something behind it.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.Center,
    ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val p = t.value
        // Three acts: heat up (0–0.35), white-out fold (0.35–0.55), cool and settle.
        val heat = when {
            p < 0.35f -> p / 0.35f
            p < 0.55f -> 1f
            else -> (1f - (p - 0.55f) / 0.45f).coerceAtLeast(0f)
        }
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    c.hot.copy(alpha = 0.55f * heat),
                    c.glow.copy(alpha = 0.34f * heat),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.42f),
                radius = size.minDimension * (0.5f + 0.8f * heat),
            ),
        )
        // The fold line sweeps through at the white-out.
        if (p in 0.32f..0.62f) {
            val k = (p - 0.32f) / 0.30f
            val y = size.height * (0.15f + 0.6f * k)
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, c.hot.copy(alpha = 0.9f), Color.Transparent),
                ),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = size.minDimension * 0.012f,
            )
        }
        // Rising embers through the cool-down. The golden-ratio hash spreads them
        // across the full width — a naive multiplier clusters them down one side.
        if (p > 0.45f) {
            val k = (p - 0.45f) / 0.55f
            repeat(40) { i ->
                val hx = (i * 0.6180339887f) % 1f
                val hy = (i * 0.2447f) % 1f
                val x = size.width * hx
                val y = size.height * (0.95f - k * (0.5f + 0.5f * hy))
                drawCircle(
                    color = lerpColor(c.hot, c.ember, k).copy(alpha = (1f - k) * 0.8f),
                    radius = size.minDimension * 0.007f * (0.5f + hy),
                    center = Offset(x, y),
                )
            }
        }

        // Scrim behind the name. By this point the white-out has faded, so without
        // it the tier name reads on top of live dashboard text.
        val nameScrim = when {
            p < 0.42f -> 0f
            p < 0.55f -> (p - 0.42f) / 0.13f
            p < 0.86f -> 1f
            else -> (1f - (p - 0.86f) / 0.14f).coerceAtLeast(0f)
        }
        if (nameScrim > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        c.ground.copy(alpha = 0.55f * nameScrim),
                        c.ground.copy(alpha = 0.93f * nameScrim),
                        c.ground.copy(alpha = 0.55f * nameScrim),
                    ),
                    startY = size.height * 0.24f,
                    endY = size.height * 0.76f,
                ),
            )
        }
    }

        // The name resolves out of the white-out and holds while the steel cools.
        val p = t.value
        val nameAlpha = when {
            p < 0.42f -> 0f
            p < 0.55f -> (p - 0.42f) / 0.13f
            p < 0.86f -> 1f
            else -> (1f - (p - 0.86f) / 0.14f).coerceAtLeast(0f)
        }
        if (nameAlpha > 0f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "FOLDED",
                    color = c.hot.copy(alpha = nameAlpha * 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 5.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    tierName,
                    color = c.hot.copy(alpha = nameAlpha),
                    fontSize = 34.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Light,
                )
            }
        }
    }
}

/** Convenience: a counter that callers bump to fire a burst. */
@Composable
fun rememberBurstKey(): androidx.compose.runtime.MutableIntState = remember { mutableIntStateOf(0) }
