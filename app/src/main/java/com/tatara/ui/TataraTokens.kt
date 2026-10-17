package com.tatara.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stable dimensions shared by every Tatara screen. */
object TataraSpacing {
    val Micro = 4.dp
    val Tight = 8.dp
    val Control = 12.dp
    val Standard = 16.dp
    val Group = 24.dp
    val Section = 32.dp
    val Major = 48.dp
    val ScreenHorizontal = 20.dp
}

object TataraSizing {
    val MinimumTouch = 48.dp
    val ControlHeight = 52.dp
    val BottomNavigationHeight = 64.dp
    val Divider = 1.dp
    val SelectionMarker = 3.dp
}

object TataraShapes {
    val Control = RoundedCornerShape(5.dp)
    val Surface = RoundedCornerShape(6.dp)
}

object TataraType {
    val Label = 11.sp
    val Body = 14.sp
    val Action = 14.sp
    val Metric = 15.sp
    val SectionLabel = 12.sp
}
