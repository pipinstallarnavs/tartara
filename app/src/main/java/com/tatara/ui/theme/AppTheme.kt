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
 * glows living green — so the three read as different worlds.
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
enum class Motif { BLADE, ASTRA, TREE, ALCHEMY, AXIOM, SIGNAL, EDIFICE, VOYAGER }

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
            ground = Color(0xFF06120F),
            surface = Color(0xFF0B1D18),
            surfaceHigh = Color(0xFF122A23),
            hairline = Color(0xFF214438),
            muted = Color(0xFF789487),
            primary = Color(0xFFE4F3E9),
            warm = Color(0xFFC38658),
            cool = Color(0xFF62D497),
            ember = Color(0xFF0B5137),
            glow = Color(0xFF50E09A),
            hot = Color(0xFFF0FFF5),
            edge = Color(0xFFA7F0C8),
        )
    ),
    ALCHEMY(
        "Alchemy",
        Motif.ALCHEMY,
        ThemeColors(
            ground = Color(0xFF100E0D),
            surface = Color(0xFF211B17),
            surfaceHigh = Color(0xFF302821),
            hairline = Color(0xFF514235),
            muted = Color(0xFFAC9780),
            primary = Color(0xFFF3E9D6),
            warm = Color(0xFFC97840),
            cool = Color(0xFF8CC8B0),
            ember = Color(0xFF76502B),
            glow = Color(0xFFE8B560),
            hot = Color(0xFFFFF3CF),
            edge = Color(0xFFFFDEAA),
        )
    ),
    AXIOM(
        "Axiom",
        Motif.AXIOM,
        ThemeColors(
            ground = Color(0xFF090F17),
            surface = Color(0xFF14212C),
            surfaceHigh = Color(0xFF1B3040),
            hairline = Color(0xFF365062),
            muted = Color(0xFF91A9AD),
            primary = Color(0xFFE3F3EC),
            warm = Color(0xFFC49F64),
            cool = Color(0xFF7BC7C4),
            ember = Color(0xFF285C68),
            glow = Color(0xFF84D9D4),
            hot = Color(0xFFF0FFF7),
            edge = Color(0xFFC1EEEA),
        )
    ),
    SIGNAL(
        "Signal",
        Motif.SIGNAL,
        ThemeColors(
            ground = Color(0xFF0C1215),
            surface = Color(0xFF17262B),
            surfaceHigh = Color(0xFF21353B),
            hairline = Color(0xFF39545B),
            muted = Color(0xFF8EA9A7),
            primary = Color(0xFFE0EEE5),
            warm = Color(0xFFD18C4B),
            cool = Color(0xFF8CC5AC),
            ember = Color(0xFF335D53),
            glow = Color(0xFFA2D8B3),
            hot = Color(0xFFF3FFE8),
            edge = Color(0xFFCBEFD3),
        )
    ),
    EDIFICE(
        "Edifice",
        Motif.EDIFICE,
        ThemeColors(
            ground = Color(0xFF141210),
            surface = Color(0xFF28231D),
            surfaceHigh = Color(0xFF352E26),
            hairline = Color(0xFF5B5042),
            muted = Color(0xFFADA18D),
            primary = Color(0xFFF1E8D7),
            warm = Color(0xFFC58250),
            cool = Color(0xFFA5B8AC),
            ember = Color(0xFF755238),
            glow = Color(0xFFD8B482),
            hot = Color(0xFFFFF0D3),
            edge = Color(0xFFE9D8AF),
        )
    ),
    VOYAGER(
        "Voyager",
        Motif.VOYAGER,
        ThemeColors(
            ground = Color(0xFF0B1220),
            surface = Color(0xFF19283A),
            surfaceHigh = Color(0xFF24374C),
            hairline = Color(0xFF3C5670),
            muted = Color(0xFF93A9B8),
            primary = Color(0xFFF1E8CE),
            warm = Color(0xFFD29A5D),
            cool = Color(0xFF85BED1),
            ember = Color(0xFF365471),
            glow = Color(0xFFD8B581),
            hot = Color(0xFFFFF1D6),
            edge = Color(0xFFE9D4A2),
        )
    )
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
    AppTheme.ALCHEMY to listOf(
        "Prima Materia", "Calcination", "Dissolution", "Separation", "Conjunction",
        "Fermentation", "Distillation", "Coagulation", "Magnum Opus",
    ),
    AppTheme.AXIOM to listOf(
        "Point", "Line", "Angle", "Plane", "Proof",
        "Lattice", "Manifold", "Symmetry", "Axiom",
    ),
    AppTheme.SIGNAL to listOf(
        "Noise", "Pulse", "Carrier", "Alignment", "Lock",
        "Resonance", "Coherence", "Clarity", "Signal",
    ),
    AppTheme.EDIFICE to listOf(
        "Survey", "Footing", "Foundation", "Column", "Arch",
        "Hall", "Tower", "Observatory", "Monument",
    ),
    AppTheme.VOYAGER to listOf(
        "Bearing", "Compass", "Chart", "Meridian", "Sextant",
        "Astrolabe", "Celestial Sphere", "Horizon", "Beyond",
    ),
)

fun AppTheme.tierName(bandIndex: Int): String = tierNamesByTheme.getValue(this)[bandIndex]

/** Bands: 1–11, 12–22, …, 78–88, 89–100 (§7.3). */
fun bandIndexForLevel(level: Int): Int = com.tatara.data.dashboard.Levels.bandFor(level)
