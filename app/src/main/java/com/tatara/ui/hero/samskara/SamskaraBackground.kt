package com.tatara.ui.hero.samskara

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntSize
import com.tatara.R
import com.tatara.ui.theme.LocalThemeColors

/** Decoded once by Compose; static art is separate from animated overlays. */
@Composable
internal fun SamskaraBackground(modifier: Modifier = Modifier) {
    val colors = LocalThemeColors.current
    val forest = ImageBitmap.imageResource(R.drawable.samskara_forest)
    Canvas(modifier) {
        drawImage(forest, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
        drawRect(Brush.verticalGradient(
            0f to colors.ground,
            0.13f to Color.Transparent,
            0.80f to Color.Transparent,
            1f to colors.ground,
        ))
    }
}
