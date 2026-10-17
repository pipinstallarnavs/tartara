package com.tatara.ui.hero

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tatara.R
import com.tatara.data.dashboard.Levels
import com.tatara.ui.theme.LocalThemeColors
import com.tatara.ui.theme.Motif
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Each tier has its own permanent central object; worlds may advance by chapter. */
internal class SkinArt(@DrawableRes val worlds: IntArray, @DrawableRes val motifs: IntArray) {
    init {
        require(worlds.size == 3 && motifs.size == 9)
    }
}

private val nieArt = SkinArt(
    intArrayOf(R.drawable.art_nie_world_t01, R.drawable.art_nie_world_t04, R.drawable.art_nie_world_t07),
    intArrayOf(R.drawable.art_nie_motif_t01, R.drawable.art_nie_motif_t02, R.drawable.art_nie_motif_t03,
        R.drawable.art_nie_motif_t04, R.drawable.art_nie_motif_t05, R.drawable.art_nie_motif_t06,
        R.drawable.art_nie_motif_t07, R.drawable.art_nie_motif_t08, R.drawable.art_nie_motif_t09),
)
private val astraArt = SkinArt(
    intArrayOf(R.drawable.art_astra_world_t01, R.drawable.art_astra_world_t01, R.drawable.art_astra_world_t01),
    intArrayOf(R.drawable.art_astra_motif_t01, R.drawable.art_astra_motif_t02, R.drawable.art_astra_motif_t03,
        R.drawable.art_astra_motif_t04, R.drawable.art_astra_motif_t05, R.drawable.art_astra_motif_t06,
        R.drawable.art_astra_motif_t07, R.drawable.art_astra_motif_t08, R.drawable.art_astra_motif_t09),
)
private val samskaraArt = SkinArt(
    intArrayOf(R.drawable.art_samskara_world_t01, R.drawable.art_samskara_world_t04, R.drawable.art_samskara_world_t07),
    intArrayOf(R.drawable.art_samskara_motif_t01, R.drawable.art_samskara_motif_t02, R.drawable.art_samskara_motif_t03,
        R.drawable.art_samskara_motif_t04, R.drawable.art_samskara_motif_t05, R.drawable.art_samskara_motif_t06,
        R.drawable.art_samskara_motif_t07, R.drawable.art_samskara_motif_t08, R.drawable.art_samskara_motif_t09),
)
private val alchemyArt = SkinArt(
    intArrayOf(R.drawable.art_alchemy_world_t01, R.drawable.art_alchemy_world_t04, R.drawable.art_alchemy_world_t07),
    intArrayOf(R.drawable.art_alchemy_motif_t01, R.drawable.art_alchemy_motif_t02, R.drawable.art_alchemy_motif_t03,
        R.drawable.art_alchemy_motif_t04, R.drawable.art_alchemy_motif_t05, R.drawable.art_alchemy_motif_t06,
        R.drawable.art_alchemy_motif_t07, R.drawable.art_alchemy_motif_t08, R.drawable.art_alchemy_motif_t09),
)
private val axiomArt = SkinArt(
    intArrayOf(R.drawable.art_axiom_world_t01, R.drawable.art_axiom_world_t04, R.drawable.art_axiom_world_t07),
    intArrayOf(R.drawable.art_axiom_motif_t01, R.drawable.art_axiom_motif_t02, R.drawable.art_axiom_motif_t03,
        R.drawable.art_axiom_motif_t04, R.drawable.art_axiom_motif_t05, R.drawable.art_axiom_motif_t06,
        R.drawable.art_axiom_motif_t07, R.drawable.art_axiom_motif_t08, R.drawable.art_axiom_motif_t09),
)
private val signalArt = SkinArt(
    intArrayOf(R.drawable.art_signal_world_t01, R.drawable.art_signal_world_t04, R.drawable.art_signal_world_t07),
    intArrayOf(R.drawable.art_signal_motif_t01, R.drawable.art_signal_motif_t02, R.drawable.art_signal_motif_t03,
        R.drawable.art_signal_motif_t04, R.drawable.art_signal_motif_t05, R.drawable.art_signal_motif_t06,
        R.drawable.art_signal_motif_t07, R.drawable.art_signal_motif_t08, R.drawable.art_signal_motif_t09),
)
private val edificeArt = SkinArt(
    intArrayOf(R.drawable.art_edifice_world_t01, R.drawable.art_edifice_world_t04, R.drawable.art_edifice_world_t07),
    intArrayOf(R.drawable.art_edifice_motif_t01, R.drawable.art_edifice_motif_t02, R.drawable.art_edifice_motif_t03,
        R.drawable.art_edifice_motif_t04, R.drawable.art_edifice_motif_t05, R.drawable.art_edifice_motif_t06,
        R.drawable.art_edifice_motif_t07, R.drawable.art_edifice_motif_t08, R.drawable.art_edifice_motif_t09),
)
private val voyagerArt = SkinArt(
    intArrayOf(R.drawable.art_voyager_world_t01, R.drawable.art_voyager_world_t04, R.drawable.art_voyager_world_t07),
    intArrayOf(R.drawable.art_voyager_motif_t01, R.drawable.art_voyager_motif_t02, R.drawable.art_voyager_motif_t03,
        R.drawable.art_voyager_motif_t04, R.drawable.art_voyager_motif_t05, R.drawable.art_voyager_motif_t06,
        R.drawable.art_voyager_motif_t07, R.drawable.art_voyager_motif_t08, R.drawable.art_voyager_motif_t09),
)

internal fun artFor(motif: Motif): SkinArt = when (motif) {
    Motif.BLADE -> nieArt
    Motif.ASTRA -> astraArt
    Motif.TREE -> samskaraArt
    Motif.ALCHEMY -> alchemyArt
    Motif.AXIOM -> axiomArt
    Motif.SIGNAL -> signalArt
    Motif.EDIFICE -> edificeArt
    Motif.VOYAGER -> voyagerArt
}

/** Low-resolution precomposited previews avoid decoding sixteen full-size images in Settings. */
@DrawableRes
internal fun thumbnailFor(motif: Motif): Int = when (motif) {
    Motif.BLADE -> R.drawable.art_nie_preview
    Motif.ASTRA -> R.drawable.art_astra_preview
    Motif.TREE -> R.drawable.art_samskara_preview
    Motif.ALCHEMY -> R.drawable.art_alchemy_preview
    Motif.AXIOM -> R.drawable.art_axiom_preview
    Motif.SIGNAL -> R.drawable.art_signal_preview
    Motif.EDIFICE -> R.drawable.art_edifice_preview
    Motif.VOYAGER -> R.drawable.art_voyager_preview
}

/** The final tier spans levels 89–100; earlier tiers span eleven levels each. */
internal fun refinementStep(level: Int, tier: Int): Int =
    (level - Levels.bandStartLevel(tier)).coerceIn(0, if (tier == 8) 11 else 10)

@Composable
internal fun IllustratedHero(motif: Motif, tier: Int, level: Int, dailyCompletion: Float, modifier: Modifier = Modifier) {
    val art = artFor(motif)
    val colors = LocalThemeColors.current
    val heat by animateFloatAsState(dailyCompletion.coerceIn(0f, 1f), tween(420), label = "Daily light")
    Crossfade(targetState = tier, animationSpec = tween(650), label = "Permanent tier art", modifier = modifier) { shownTier ->
        val step = refinementStep(level, shownTier)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(art.worlds[shownTier / 3]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (motif == Motif.ASTRA && shownTier >= 3) Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.5f, size.height * 0.51f)
                val radius = size.width * 0.38f
                drawCircle(colors.cool.copy(alpha = 0.35f), radius, center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                if (shownTier >= 6) {
                    drawCircle(colors.glow.copy(alpha = 0.42f), radius * 0.84f, center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
            }
            // A tree grows with every held level; unlike an orbit, its branches are
            // painted organic forms, so scaling the object beats drawing line foliage.
            val motifLayer = when {
                motif == Motif.TREE && shownTier == 0 ->
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 0.62f + step * 0.012f
                        scaleY = scaleX
                    }.offset(y = 80.dp)
                motif == Motif.TREE || motif == Motif.BLADE || (motif == Motif.EDIFICE && shownTier >= 6) ->
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 1f + step * 0.005f
                        scaleY = scaleX
                    }
                else -> Modifier.fillMaxSize()
            }
            val bladePolish = if (motif == Motif.BLADE && step > 0) remember(step) {
                val lift = step * 1.7f
                ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, lift,
                    0f, 1f, 0f, 0f, lift,
                    0f, 0f, 1f, 0f, lift,
                    0f, 0f, 0f, 1f, 0f,
                )))
            } else null
            Box(motifLayer) {
                Image(
                    painter = painterResource(art.motifs[shownTier]),
                    contentDescription = null,
                    colorFilter = bladePolish,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                if (motif != Motif.TREE && motif != Motif.BLADE && !(motif == Motif.EDIFICE && shownTier >= 6)) {
                    // Permanent instrument details stay registered to the motif.
                    Canvas(Modifier.fillMaxSize()) {
                        val scale = min(size.width / 1024f, size.height / 1536f)
                        val origin = Offset((size.width - 1024f * scale) / 2f, (size.height - 1536f * scale) / 2f)
                        drawRefinement(motif, step, scale, origin, colors.edge)
                    }
                }
            }
            // A short-lived atmosphere, driven only by today's logged actions.
            if (heat > 0.001f) Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.glow.copy(alpha = 0.18f * heat), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.52f),
                        radius = size.width * 0.75f,
                    ),
                )
            }
        }
    }
}

/** Cumulative permanent details within a band; independent of today's completion. */
private fun DrawScope.drawRefinement(motif: Motif, steps: Int, scale: Float, origin: Offset, color: Color) {
    fun at(x: Float, y: Float) = Offset(origin.x + x * scale, origin.y + y * scale)
    val ink = color.copy(alpha = 0.88f)
    val stroke = 4f * scale
    for (i in 0 until steps) {
        when (motif) {
            Motif.BLADE, Motif.TREE -> Unit // Painted objects are refined by scale / material above.
            Motif.ASTRA -> {
                // New runes and barbs attach directly to the arrow's central shaft.
                val y = 1170f - i * 78f
                drawLine(ink, at(491f, y), at(512f, y - 17f), stroke)
                drawLine(ink, at(512f, y - 17f), at(533f, y), stroke)
            }
            Motif.ALCHEMY, Motif.AXIOM, Motif.VOYAGER -> {
                // Each stage adds a calibrated node to the apparatus, proof, or navigation ring.
                val angle = (i * 2.0 * PI / 11.0 - PI / 2).toFloat()
                val x = 512f + cos(angle) * 365f
                val y = 710f + sin(angle) * 365f
                drawCircle(ink, 11f * scale, at(x, y))
                if (motif == Motif.AXIOM) drawLine(ink, at(x, y), at(512f, 710f), 1.4f * scale)
                if (motif == Motif.VOYAGER) drawLine(ink, at(x, y), at(x + cos(angle) * 27f, y + sin(angle) * 27f), stroke)
                if (motif == Motif.ALCHEMY) drawCircle(ink, 22f * scale, at(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(2f * scale))
            }
            Motif.SIGNAL -> {
                // Stable calibration points accumulate along the waveform.
                val x = 160f + i * 76f
                val y = 780f + sin(i * 1.3f) * 112f
                drawCircle(ink, 10f * scale, at(x, y))
                drawLine(ink, at(x, y - 30f), at(x, y + 30f), 2f * scale)
            }
            Motif.EDIFICE -> {
                // Masonry is laid course by course around the permanent structure.
                val y = 1290f - i * 90f
                val x = if (i % 2 == 0) 192f else 630f
                drawLine(ink, at(x, y), at(x + 180f, y), stroke)
                drawLine(ink, at(x, y), at(x, y - 32f), stroke)
                drawLine(ink, at(x + 180f, y), at(x + 180f, y - 32f), stroke)
            }
        }
    }
}
