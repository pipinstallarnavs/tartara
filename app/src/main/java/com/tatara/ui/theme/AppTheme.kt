package com.tatara.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The only file allowed to contain literal colours. Everything else reads
 * LocalThemeColors.current.<token>.
 *
 * Token semantics (§2.5): warm = in progress / at risk / over; cool = complete /
 * earned / on pace. Nothing is warm permanently. The old Nie names map as
 * sumi→ground, ji→surface, jihada→hairline, tsuchi→muted, ha→primary, hi→warm,
 * mizu→cool. The §3.6 amber state is warm at 60% opacity: warm.copy(alpha = 0.6f).
 */
data class ThemeColors(
    val ground: Color,
    val surface: Color,
    val hairline: Color,
    val muted: Color,
    val primary: Color,
    val warm: Color,
    val cool: Color,
)

enum class AppTheme(val label: String, val colors: ThemeColors) {
    NIE(
        "Nie",
        ThemeColors(
            Color(0xFF0F1418), Color(0xFF232C33), Color(0xFF33414A),
            Color(0xFF8C8070), Color(0xFFE9EFF3), Color(0xFFC4562A), Color(0xFF6FA0BE),
        )
    ),
    ASTRA(
        "Astra",
        ThemeColors(
            Color(0xFF0A0B14), Color(0xFF1A1D2E), Color(0xFF2C3049),
            Color(0xFF7A7C93), Color(0xFFEBD9A8), Color(0xFFD4622F), Color(0xFF7C8CE8),
        )
    ),
    SAMSKARA(
        "Saṃskāra",
        ThemeColors(
            Color(0xFF14100C), Color(0xFF241E18), Color(0xFF3A3128),
            Color(0xFF8B8072), Color(0xFFF0E6D2), Color(0xFFB5502F), Color(0xFF7B9B84),
        )
    ),
}

val LocalThemeColors = staticCompositionLocalOf { AppTheme.NIE.colors }

/**
 * §7.3 — the nine tier names, per theme, keyed by band index 0–8.
 * Levels and band boundaries are unchanged; only the displayed name varies.
 */
private val tierNamesByTheme: Map<AppTheme, List<String>> = mapOf(
    AppTheme.NIE to listOf(
        "Tamahagane", "Orikaeshi", "Tsuchioki", "Yaki-ire", "Hamon",
        "Togi", "Mei", "Meibutsu", "Kokuhō",
    ),
    AppTheme.ASTRA to listOf(
        "Agneyastra", "Vayavyastra", "Nagastra", "Varunastra", "Vajra",
        "Garudastra", "Brahmastra", "Pashupatastra", "Narayanastra",
    ),
    AppTheme.SAMSKARA to listOf(
        "Ārambha", "Abhyāsa", "Tapas", "Sthiti", "Saṃskāra",
        "Dhyāna", "Sthitaprajña", "Siddhi", "Svabhāva",
    ),
)

fun AppTheme.tierName(bandIndex: Int): String = tierNamesByTheme.getValue(this)[bandIndex]

/** Bands: 1–11, 12–22, …, 78–88, 89–100 (§7.3). */
fun bandIndexForLevel(level: Int): Int = com.tatara.data.dashboard.Levels.bandFor(level)
