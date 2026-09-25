package com.tatara.ui.hero.samskara

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.tatara.R
import androidx.compose.ui.graphics.graphicsLayer
import com.tatara.ui.LegacyHero
import com.tatara.ui.theme.Motif

/** Samskara's composited progression world. All text remains outside this layer. */
@Composable
fun SamskaraHero(
    tier: Int,
    dailyCompletion: Float,
    modifier: Modifier = Modifier,
) {
    val targetCompletion = dailyCompletion.coerceIn(0f, 1f)
    val animatedCompletion = remember { Animatable(0f) }
    LaunchedEffect(targetCompletion) {
        animatedCompletion.animateTo(
            targetCompletion,
            animationSpec = tween(1_100, easing = FastOutSlowInEasing),
        )
    }

    val ambient = rememberInfiniteTransition(label = "samskaraBreath")
    val breath by ambient.animateFloat(
        initialValue = 0.996f,
        targetValue = 1.004f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "treeBreath",
    )

    Box(modifier = modifier) {
        SamskaraBackground(Modifier.fillMaxSize())
        SamskaraOrnament(Modifier.fillMaxSize())
        SamskaraFog(Modifier.fillMaxSize())
        SamskaraGlow(
            dailyCompletion = animatedCompletion.value,
            breath = breath,
            modifier = Modifier.fillMaxSize(),
        )
        SamskaraTierTransition(tier = tier, modifier = Modifier.fillMaxSize()) { currentTier ->
            if (currentTier == 0) {
                ArambhaSapling(
                    dailyCompletion = animatedCompletion.value,
                    breath = breath,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Higher Samskara tiers intentionally retain their existing art
                // until each world receives its own focused redesign pass.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LegacyHero(
                        motif = Motif.TREE,
                        band = currentTier,
                        heat = animatedCompletion.value,
                    )
                }
            }
        }
        SamskaraParticles(
            dailyCompletion = animatedCompletion.value,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
@Composable
private fun ArambhaSapling(
    dailyCompletion: Float,
    breath: Float,
    modifier: Modifier = Modifier,
) {
    val sapling = ImageBitmap.imageResource(R.drawable.samskara_sapling)
    Canvas(modifier.graphicsLayer {
        scaleX = breath
        scaleY = breath
        transformOrigin = TransformOrigin(0.5f, 0.77f)
    }) {
        val height = size.height * 0.43f
        val width = height * sapling.width / sapling.height
        drawImage(
            sapling,
            dstOffset = IntOffset(((size.width - width) / 2f).toInt(), (size.height * 0.36f).toInt()),
            dstSize = IntSize(width.toInt(), height.toInt()),
            alpha = 0.94f + dailyCompletion * 0.06f,
        )
    }
}
