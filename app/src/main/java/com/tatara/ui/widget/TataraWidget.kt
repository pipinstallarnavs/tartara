package com.tatara.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tatara.data.dashboard.Levels
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.HabitLogStatus
import com.tatara.data.food.FatPace
import com.tatara.data.food.FatPaceState
import com.tatara.data.food.FoodRepository
import com.tatara.data.habit.HabitRepository
import com.tatara.ui.theme.ThemePreferences
import com.tatara.ui.theme.tierName
import java.time.LocalDate

class TataraWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TataraWidget()
}

/**
 * §8 — 4×2 Glance widget: four stack rows with n/m progress, tap a row to expand
 * its items, tap an item to tick it straight into Room. Bottom strip: calories
 * remaining, fat pace dot, current tier. One-tap ticking from the home screen is
 * the highest-leverage feature in the application.
 */
class TataraWidget : GlanceAppWidget() {

    companion object {
        val EXPANDED_STACK = longPreferencesKey("expandedStack")
        val STACK_ID = ActionParameters.Key<Long>("stackId")
        val HABIT_ID = ActionParameters.Key<Long>("habitId")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = TataraDatabase.build(context)
        val theme = ThemePreferences(context).load()
        val colors = theme.colors
        val today = LocalDate.now()

        val stacks = db.habitDao().getAllStacks().sortedBy { it.sortOrder }
        val habits = db.habitDao().getAllHabits()
        val logs = db.habitDao().logsOn(today).associateBy { it.habitId }

        val foodRepo = FoodRepository(db)
        val totals = foodRepo.totalsOn(today)
        val target = foodRepo.latestTargets()
        val remainingKcal = target?.let { it.kcalTarget - totals.kcal }
        val paceState = target?.let { FatPace.state(it.fatG - totals.fat, it.kcalTarget - totals.kcal) }

        val xp = db.dashboardDao().totalXp()
        val maxBand = db.dashboardDao().getAllTierCrossings()
            .maxOfOrNull { Levels.bandFor(it.level) } ?: 0
        val tier = theme.tierName(Levels.bandFor(Levels.effectiveLevel(xp, maxBand)))

        provideContent {
            val expanded = currentState<Preferences>()[EXPANDED_STACK] ?: -1L
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(colors.ground))
                    .cornerRadius(2.dp)
                    .padding(10.dp),
            ) {
                stacks.take(4).forEach { stack ->
                    val members = habits.filter { it.stackId == stack.id }
                    val done = members.count { logs[it.id]?.status == HabitLogStatus.COMPLETED }
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(
                                actionRunCallback<ToggleStackAction>(
                                    actionParametersOf(STACK_ID to stack.id)
                                )
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stack.name,
                            style = TextStyle(
                                color = ColorProvider(if (done == members.size && members.isNotEmpty()) colors.cool else colors.primary),
                                fontSize = 12.sp,
                            ),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        Text(
                            "$done/${members.size}",
                            style = TextStyle(color = ColorProvider(colors.muted), fontSize = 11.sp),
                        )
                    }
                    if (expanded == stack.id) {
                        Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp)) {
                            members.forEach { habit ->
                                val ticked = logs[habit.id]?.status == HabitLogStatus.COMPLETED
                                Box(
                                    modifier = GlanceModifier
                                        .padding(end = 4.dp)
                                        .background(ColorProvider(if (ticked) colors.cool else colors.surface))
                                        .cornerRadius(2.dp)
                                        .clickable(
                                            actionRunCallback<ToggleHabitAction>(
                                                actionParametersOf(HABIT_ID to habit.id)
                                            )
                                        ),
                                ) {
                                    Text(
                                        habit.name.take(10),
                                        style = TextStyle(
                                            color = ColorProvider(if (ticked) colors.ground else colors.muted),
                                            fontSize = 10.sp,
                                        ),
                                        modifier = GlanceModifier.padding(horizontal = 5.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        remainingKcal?.let { "${it.toInt()} kcal left" } ?: "No target.",
                        style = TextStyle(color = ColorProvider(colors.muted), fontSize = 11.sp),
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    paceState?.let { state ->
                        val dot = when (state) {
                            FatPaceState.FAT_LIGHT -> colors.cool
                            FatPaceState.ON_PACE -> colors.primary
                            FatPaceState.FAT_LOADED -> colors.warm.copy(alpha = 0.6f)
                            FatPaceState.SPENT -> colors.warm
                            FatPaceState.OVER -> colors.muted
                        }
                        Box(
                            modifier = GlanceModifier.size(8.dp).background(ColorProvider(dot)).cornerRadius(4.dp),
                        ) { Text("") }
                    }
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        tier,
                        style = TextStyle(color = ColorProvider(colors.primary), fontSize = 11.sp),
                    )
                }
            }
        }
    }
}

class ToggleStackAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val stackId = parameters[TataraWidget.STACK_ID] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[TataraWidget.EXPANDED_STACK] ?: -1L
            prefs[TataraWidget.EXPANDED_STACK] = if (current == stackId) -1L else stackId
        }
        TataraWidget().update(context, glanceId)
    }
}

/** §8 — writes straight to Room, no app launch. */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[TataraWidget.HABIT_ID] ?: return
        HabitRepository(TataraDatabase.build(context)).toggle(habitId)
        TataraWidget().update(context, glanceId)
    }
}
