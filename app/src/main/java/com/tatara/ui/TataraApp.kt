package com.tatara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tatara.data.db.TataraDatabase
import com.tatara.data.food.FoodRepository
import com.tatara.data.habit.HabitRepository
import com.tatara.data.sleep.SleepRepository
import com.tatara.data.train.TrainRepository
import com.tatara.ui.theme.AppTheme
import com.tatara.ui.theme.LocalMotif
import com.tatara.ui.theme.LocalThemeColors
import com.tatara.ui.theme.ThemePreferences

/** §1 — five destinations, no sixth. Settings is an overlay, not a tab. */
private enum class Tab(val label: String, val icon: TataraNavIcon) {
    DASHBOARD("Dashboard", TataraNavIcon.DASHBOARD),
    FOOD("Food", TataraNavIcon.FOOD),
    TRAIN("Train", TataraNavIcon.TRAIN),
    HABITS("Habits", TataraNavIcon.HABITS),
    SLEEP("Sleep", TataraNavIcon.SLEEP),
}

@Composable
fun TataraApp(db: TataraDatabase) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences(context) }
    var theme by remember { mutableStateOf(themePrefs.load()) }
    var tab by remember { mutableStateOf(Tab.DASHBOARD) }
    var showSettings by remember { mutableStateOf(false) }
    /** Non-null while the §7.3 fold is playing; holds the newly reached tier name. */
    var foldTier by remember { mutableStateOf<String?>(null) }
    val foodRepository = remember { FoodRepository(db) }
    val habitRepository = remember { HabitRepository(db) }
    val sleepRepository = remember { SleepRepository(db) }
    val trainRepository = remember { TrainRepository(db) }

    val c = theme.colors
    // Every framework default is overridden at the root: Material's own purple/blue
    // can never leak into a component we did not colour ourselves, and the text
    // selection highlight follows the active theme instead of the stock accent.
    val scheme = darkColorScheme(
        primary = c.cool,
        onPrimary = c.ground,
        secondary = c.warm,
        onSecondary = c.ground,
        background = c.ground,
        onBackground = c.primary,
        surface = c.surface,
        onSurface = c.primary,
        surfaceVariant = c.surfaceHigh,
        onSurfaceVariant = c.muted,
        outline = c.hairline,
        error = c.warm,
        onError = c.ground,
    )
    val selection = TextSelectionColors(handleColor = c.cool, backgroundColor = c.cool.copy(alpha = 0.28f))

    CompositionLocalProvider(
        LocalThemeColors provides c,
        LocalMotif provides theme.motif,
        LocalTextSelectionColors provides selection,
        LocalContentColor provides c.primary,
    ) {
        MaterialTheme(colorScheme = scheme) {
        // §7.3 — the fold is drawn over the whole app, nav included: crossing a
        // tier is the one moment allowed to take the entire screen.
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.ground)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (showSettings) {
                    SettingsScreen(
                        db = db,
                        selected = theme,
                        onSelect = {
                            theme = it
                            themePrefs.save(it)
                        },
                        onBack = { showSettings = false },
                    )
                } else {
                    when (tab) {
                        Tab.DASHBOARD -> DashboardScreen(
                            db, theme,
                            onOpenSettings = { showSettings = true },
                            onTierCrossed = { foldTier = it },
                        )
                        Tab.FOOD -> FoodScreen(foodRepository, db)
                        Tab.TRAIN -> TrainScreen(trainRepository)
                        Tab.HABITS -> HabitsScreen(habitRepository)
                        Tab.SLEEP -> SleepScreen(sleepRepository)
                    }
                }
            }
            TataraBottomNavigation(
                items = Tab.entries,
                selected = tab,
                onSelected = {
                    tab = it
                    showSettings = false
                },
                label = { it.label },
                icon = { it.icon },
            )
        }
        foldTier?.let { tier ->
            FoldOverlay(tierName = tier, onDone = { foldTier = null })
        }
        }
        }
    }
}
