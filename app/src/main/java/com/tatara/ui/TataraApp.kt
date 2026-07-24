package com.tatara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.data.db.TataraDatabase
import com.tatara.data.food.FoodRepository
import com.tatara.data.habit.HabitRepository
import com.tatara.data.sleep.SleepRepository
import com.tatara.ui.theme.AppTheme
import com.tatara.ui.theme.LocalThemeColors
import com.tatara.ui.theme.ThemePreferences

/** §1 — five destinations, no sixth. Settings is an overlay, not a tab. */
private enum class Tab(val label: String) {
    DASHBOARD("Dashboard"), FOOD("Food"), TRAIN("Train"), HABITS("Habits"), SLEEP("Sleep"),
}

@Composable
fun TataraApp(db: TataraDatabase) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences(context) }
    var theme by remember { mutableStateOf(themePrefs.load()) }
    var tab by remember { mutableStateOf(Tab.DASHBOARD) }
    var showSettings by remember { mutableStateOf(false) }
    val foodRepository = remember { FoodRepository(db) }
    val habitRepository = remember { HabitRepository(db) }
    val sleepRepository = remember { SleepRepository(db) }

    CompositionLocalProvider(LocalThemeColors provides theme.colors) {
        val c = LocalThemeColors.current
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
                        Tab.DASHBOARD -> DashboardStub(onOpenSettings = { showSettings = true })
                        Tab.FOOD -> FoodScreen(foodRepository)
                        Tab.TRAIN -> EmptyTab()
                        Tab.HABITS -> HabitsScreen(habitRepository)
                        Tab.SLEEP -> SleepScreen(sleepRepository)
                    }
                }
            }
            BottomNav(current = tab, onSelect = {
                tab = it
                showSettings = false
            })
        }
    }
}

@Composable
private fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    val c = LocalThemeColors.current
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.hairline)
        )
        Row(modifier = Modifier.fillMaxWidth().background(c.ground)) {
            Tab.entries.forEach { tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable { onSelect(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.label,
                        color = if (tab == current) c.primary else c.muted,
                        fontSize = 12.sp,
                        fontWeight = if (tab == current) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStub(onOpenSettings: () -> Unit) {
    val c = LocalThemeColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "Settings",
                color = c.muted,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(onClick = onOpenSettings)
                    .padding(16.dp),
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Nothing here yet.", color = c.muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyTab() {
    val c = LocalThemeColors.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Nothing here yet.", color = c.muted, fontSize = 14.sp)
    }
}
