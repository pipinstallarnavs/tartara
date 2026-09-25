package com.tatara.ui.hero

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tatara.ui.LegacyHero
import com.tatara.ui.hero.samskara.SamskaraHero
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
    dailyCompletion: Float,
    modifier: Modifier = Modifier,
) {
    when (motif) {
        Motif.TREE -> SamskaraHero(
            tier = tier,
            dailyCompletion = dailyCompletion,
            modifier = modifier
                .fillMaxWidth()
                .height(392.dp),
        )

        Motif.BLADE,
        Motif.ASTRA,
        -> Box(
            modifier = modifier
                .fillMaxWidth()
                .height(342.dp),
            contentAlignment = Alignment.Center,
        ) {
            LegacyHero(
                motif = motif,
                band = tier,
                heat = dailyCompletion,
            )
        }
    }
}
