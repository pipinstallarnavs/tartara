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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.tatara.ui.LegacyHero
import com.tatara.ui.theme.LocalThemeColors
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
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "treeBreath",
    )

    Box(modifier = modifier) {
        SamskaraBackground(Modifier.fillMaxSize())
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
    val colors = LocalThemeColors.current
    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = breath
            scaleY = breath
            transformOrigin = TransformOrigin(0.5f, 0.82f)
        },
    ) {
        drawArambhaSapling(
            colors = colors,
            dailyCompletion = dailyCompletion,
            breath = breath,
        )
    }
}

private fun DrawScope.drawArambhaSapling(
    colors: com.tatara.ui.theme.ThemeColors,
    dailyCompletion: Float,
    breath: Float,
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val groundY = h * 0.83f
    val topY = h * 0.48f
    val trunkHalfWidth = w * 0.009f

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                colors.ember.copy(alpha = 0.42f),
                colors.surface.copy(alpha = 0.18f),
            ),
            center = Offset(cx, groundY),
            radius = w * 0.22f,
        ),
        topLeft = Offset(cx - w * 0.22f, groundY - h * 0.018f),
        size = Size(w * 0.44f, h * 0.055f),
    )

    val roots = listOf(
        Offset(cx - w * 0.095f, groundY + h * 0.018f),
        Offset(cx + w * 0.105f, groundY + h * 0.015f),
        Offset(cx - w * 0.045f, groundY + h * 0.028f),
    )
    roots.forEachIndexed { index, end ->
        val path = Path().apply {
            moveTo(cx + (index - 1) * trunkHalfWidth, groundY - h * 0.015f)
            quadraticTo((cx + end.x) * 0.5f, groundY, end.x, end.y)
        }
        drawPath(
            path = path,
            color = colors.muted.copy(alpha = 0.42f),
            style = Stroke(width = w * 0.006f, cap = StrokeCap.Round),
        )
    }

    val trunk = Path().apply {
        moveTo(cx - trunkHalfWidth * 1.55f, groundY)
        cubicTo(
            cx - trunkHalfWidth * 0.75f, h * 0.72f,
            cx + trunkHalfWidth * 0.20f, h * 0.60f,
            cx - trunkHalfWidth * 0.18f, topY,
        )
        lineTo(cx + trunkHalfWidth * 0.58f, topY)
        cubicTo(
            cx + trunkHalfWidth * 0.95f, h * 0.60f,
            cx + trunkHalfWidth * 0.90f, h * 0.72f,
            cx + trunkHalfWidth * 1.55f, groundY,
        )
        close()
    }
    drawPath(
        path = trunk,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.ground, colors.muted, colors.hairline),
            startX = cx - trunkHalfWidth * 2f,
            endX = cx + trunkHalfWidth * 2f,
        ),
    )
    drawPath(
        path = trunk,
        color = colors.edge.copy(alpha = 0.19f + dailyCompletion * 0.10f),
        style = Stroke(width = w * 0.0028f),
    )

    val branchColor = colors.muted.copy(alpha = 0.78f)
    fun branch(start: Offset, control: Offset, end: Offset, width: Float) {
        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(control.x, control.y, end.x, end.y)
        }
        drawPath(path, branchColor, style = Stroke(width = width, cap = StrokeCap.Round))
        drawPath(
            path,
            colors.edge.copy(alpha = 0.13f),
            style = Stroke(width = width * 0.32f, cap = StrokeCap.Round),
        )
    }

    branch(
        Offset(cx, h * 0.59f),
        Offset(cx - w * 0.035f, h * 0.55f),
        Offset(cx - w * 0.105f, h * 0.535f),
        w * 0.0075f,
    )
    branch(
        Offset(cx, h * 0.64f),
        Offset(cx + w * 0.045f, h * 0.59f),
        Offset(cx + w * 0.12f, h * 0.575f),
        w * 0.0080f,
    )
    branch(
        Offset(cx, h * 0.525f),
        Offset(cx + w * 0.025f, h * 0.49f),
        Offset(cx + w * 0.07f, h * 0.465f),
        w * 0.0058f,
    )

    val leafAlpha = 0.68f + dailyCompletion * 0.20f
    val leafColor = colors.cool.copy(alpha = leafAlpha)
    val litLeaf = colors.glow.copy(alpha = (0.24f + dailyCompletion * 0.30f) * breath)
    val leaves = listOf(
        Triple(0.392f, 0.526f, -24f),
        Triple(0.425f, 0.548f, 18f),
        Triple(0.612f, 0.564f, -16f),
        Triple(0.645f, 0.585f, 24f),
        Triple(0.565f, 0.458f, -28f),
        Triple(0.515f, 0.484f, 12f),
        Triple(0.468f, 0.507f, -8f),
    )
    leaves.forEachIndexed { index, (x, y, angle) ->
        val center = Offset(w * x, h * y)
        rotate(degrees = angle, pivot = center) {
            drawOval(
                color = if (index % 3 == 0) litLeaf else leafColor,
                topLeft = Offset(center.x - w * 0.018f, center.y - h * 0.008f),
                size = Size(w * 0.036f, h * 0.016f),
            )
            drawLine(
                color = colors.edge.copy(alpha = 0.22f),
                start = Offset(center.x - w * 0.012f, center.y),
                end = Offset(center.x + w * 0.012f, center.y),
                strokeWidth = w * 0.0018f,
            )
        }
    }

    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                colors.ground.copy(alpha = 0f),
                colors.cool.copy(alpha = 0.30f),
                colors.ground.copy(alpha = 0f),
            ),
        ),
        start = Offset(cx - w * 0.22f, groundY + h * 0.025f),
        end = Offset(cx + w * 0.22f, groundY + h * 0.025f),
        strokeWidth = h * 0.002f,
    )
}
