package com.tatara.ui.hero

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tatara.ui.theme.Motif

/**
 * Stable boundary between progression state and a theme's visual world.
 *
 * A world receives only presentation inputs. XP, levels, tier floors, and daily
 * completion remain owned by the dashboard and data layers.
 */
@Composable
fun ProgressionHero(
    motif: Motif,
    tier: Int,
    level: Int,
    dailyCompletion: Float,
    modifier: Modifier = Modifier,
) {
    IllustratedHero(
        motif = motif,
        tier = tier,
        level = level,
        dailyCompletion = dailyCompletion,
        modifier = modifier.fillMaxWidth().height(480.dp),
    )
}
