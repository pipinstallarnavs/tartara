package com.tatara.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.ActivityLevel
import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.Sex
import com.tatara.ui.hero.thumbnailFor
import com.tatara.ui.theme.AppTheme
import com.tatara.ui.theme.LocalThemeColors
import java.time.LocalDate
import kotlinx.coroutines.launch

private val SEX_OPTIONS = listOf<Sex?>(null, Sex.MALE, Sex.FEMALE)


@Composable
fun SettingsScreen(
    db: TataraDatabase,
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalThemeColors.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(Settings()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settings = db.bodyDao().getSettings() ?: Settings()
        loaded = true
    }

    fun save(updated: Settings) {
        settings = updated
        scope.launch { db.bodyDao().upsertSettings(updated) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TataraSpacing.ScreenHorizontal, vertical = TataraSpacing.Standard),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Settings", color = c.primary, fontSize = 20.sp)
            TataraGhostAction(
                label = "Back",
                onClick = onBack,
            )
        }

        TataraSectionHeader("Theme")
        AppTheme.entries.forEach { theme ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(theme) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(thumbnailFor(theme.motif)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 48.dp, height = 64.dp),
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    theme.label,
                    color = if (theme == selected) c.primary else c.muted,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                if (theme == selected) {
                    Text("Selected", color = c.cool, fontSize = 12.sp)
                }
            }
            TataraDivider()
        }

        if (loaded) {
            TataraSectionHeader("Targets")
            NumberRow("Protein (g/kg)", settings.proteinPerKg.toString()) { v ->
                v.toFloatOrNull()?.let { save(settings.copy(proteinPerKg = it)) }
            }
            NumberRow("Goal rate (% bw/week)", settings.goalRatePercent.toString()) { v ->
                v.toFloatOrNull()?.let { save(settings.copy(goalRatePercent = it)) }
            }

            TataraSectionHeader("Profile")
            Text(
                "Feeds the calorie sanity bounds and the Dashboard's formula estimate. Blank disables both.",
                color = c.muted, fontSize = 11.sp,
            )
            NumberRow("Height (cm)", settings.heightCm?.toString() ?: "") { v ->
                save(settings.copy(heightCm = v.toFloatOrNull()))
            }
            NumberRow("Birth year", settings.birthYear?.toString() ?: "") { v ->
                save(settings.copy(birthYear = v.toIntOrNull()))
            }
            TataraSegmentedControl(
                items = SEX_OPTIONS,
                selected = settings.sex,
                onSelected = { save(settings.copy(sex = it)) },
                label = "Sex",
                itemLabel = {
                    when (it) {
                        Sex.MALE -> "Male"
                        Sex.FEMALE -> "Female"
                        null -> "Not set"
                    }
                },
            )
            Spacer(modifier = Modifier.height(TataraSpacing.Control))
            Text(
                "ACTIVITY LEVEL",
                color = c.primary.copy(alpha = 0.68f),
                fontSize = TataraType.Label,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(TataraSpacing.Micro))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)
            ) {
                item {
                    TataraChoiceChip(
                        label = "Not set",
                        selected = settings.activityLevel == null,
                        onClick = { save(settings.copy(activityLevel = null)) },
                    )
                }
                items(ActivityLevel.entries.size) { index ->
                    val level = ActivityLevel.entries[index]
                    TataraChoiceChip(
                        label = level.label,
                        selected = settings.activityLevel == level,
                        onClick = { save(settings.copy(activityLevel = level)) },
                    )
                }
            }

            TataraSectionHeader("Block")
            // §3.4.1 — clears the protein ratchet to current weight.
            TataraSecondaryButton(
                label = "Start new block",
                onClick = {
                    save(settings.copy(blockStartDate = LocalDate.now(), ratchetWeightKg = null))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            settings.blockStartDate?.let {
                Text("Current block since $it.", color = c.muted, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
private fun NumberRow(label: String, initial: String, onCommit: (String) -> Unit) {
    var text by remember(label, initial) { mutableStateOf(initial) }
    TataraNumberField(
        value = text,
        onValueChange = {
            text = it
            onCommit(it)
        },
        label = label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = TataraSpacing.Control),
    )
}
