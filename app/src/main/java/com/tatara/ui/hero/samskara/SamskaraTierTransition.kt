package com.tatara.ui.hero.samskara

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.tatara.ui.theme.LocalThemeColors

/**
 * Transition seam for future growth cinematics. For now a tier change crossfades
 * the hero and produces one restrained ground-up glow pulse.
 */
@Composable
internal fun SamskaraTierTransition(
    tier: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(tier: Int) -> Unit,
) {
    val colors = LocalThemeColors.current
    val pulse = remember { Animatable(0f) }
    var previousTier by remember { mutableIntStateOf(tier) }

    LaunchedEffect(tier) {
        if (tier != previousTier) {
            previousTier = tier
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            pulse.animateTo(0f, tween(780, easing = FastOutSlowInEasing))
        }
    }

    Box(modifier = modifier) {
        Crossfade(
            targetState = tier,
            animationSpec = tween(900, easing = FastOutSlowInEasing),
            label = "samskaraTierCrossfade",
        ) { currentTier ->
            Box(modifier = Modifier.fillMaxSize()) {
                content(currentTier)
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.79f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.hot.copy(alpha = pulse.value * 0.22f),
                        colors.glow.copy(alpha = pulse.value * 0.12f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = center,
            )
        }
    }
}
