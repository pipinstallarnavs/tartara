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
 *
 * The heat ramp (ember → glow → hot) is the reward language: a cold billet at
 * rest, brightening as the day's work lands, white-hot only at a fold. Each
 * theme tunes its own ramp — Nie burns orange, Astra burns violet, Saṃskāra
 * burns amber — so the three read as different worlds, not one palette recoloured.
 */
data class ThemeColors(
    val ground: Color,
    val surface: Color,
    /** Elevation 2: cards that must sit above a surface, not beside it. */
    val surfaceHigh: Color,
    val hairline: Color,
    val muted: Color,
    val primary: Color,
    val warm: Color,
    val cool: Color,
    /** Heat ramp, coldest first. ember = barely lit, glow = working heat, hot = forge-white. */
    val ember: Color,
    val glow: Color,
    val hot: Color,
    /** Rim light along a lit edge — the brightest line in the illustration. */
    val edge: Color,
)

/** Which hero and which completion mark a theme draws. */
enum class Motif { BLADE, ASTRA, TREE }

enum class AppTheme(val label: String, val motif: Motif, val colors: ThemeColors) {
    NIE(
        "Nie",
        Motif.BLADE,
        ThemeColors(
            ground = Color(0xFF0B0F13),
            surface = Color(0xFF161D24),
            surfaceHigh = Color(0xFF1F2932),
            hairline = Color(0xFF2E3B46),
            muted = Color(0xFF8C8070),
            primary = Color(0xFFE9EFF3),
            warm = Color(0xFFC4562A),
            cool = Color(0xFF6FA0BE),
            ember = Color(0xFF7A2A10),
            glow = Color(0xFFE87433),
            hot = Color(0xFFFFF1D6),
            edge = Color(0xFFDCE9F2),
        )
    ),
    ASTRA(
        "Astra",
        Motif.ASTRA,
        ThemeColors(
            ground = Color(0xFF080A14),
            surface = Color(0xFF141830),
            surfaceHigh = Color(0xFF1E2442),
            hairline = Color(0xFF2E3559),
            muted = Color(0xFF7A7C93),
            primary = Color(0xFFEBD9A8),
            warm = Color(0xFFD4622F),
            cool = Color(0xFF7C8CE8),
            ember = Color(0xFF2E2260),
            glow = Color(0xFF8B6FE8),
            hot = Color(0xFFFFF6D9),
            edge = Color(0xFFCFC0FF),
        )
    ),
    SAMSKARA(
        "Saṃskāra",
        Motif.TREE,
        ThemeColors(
            ground = Color(0xFF100D0A),
            surface = Color(0xFF1F1A15),
            surfaceHigh = Color(0xFF2B2419),
            hairline = Color(0xFF3A3128),
            muted = Color(0xFF8B8072),
            primary = Color(0xFFF0E6D2),
            warm = Color(0xFFB5502F),
            cool = Color(0xFF7B9B84),
            ember = Color(0xFF5A3218),
            glow = Color(0xFFC98A4B),
            hot = Color(0xFFFFF3DE),
            edge = Color(0xFFE6D9BE),
        )
    ),
}

val LocalThemeColors = staticCompositionLocalOf { AppTheme.NIE.colors }
val LocalMotif = staticCompositionLocalOf { Motif.BLADE }

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
