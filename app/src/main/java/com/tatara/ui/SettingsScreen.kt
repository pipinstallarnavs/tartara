package com.tatara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.data.db.TataraDatabase
import com.tatara.data.db.entity.Settings
import com.tatara.data.db.entity.Sex
import com.tatara.ui.theme.AppTheme
import com.tatara.ui.theme.LocalThemeColors
import java.time.LocalDate
import kotlinx.coroutines.launch

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
            .padding(start = 24.dp, end = 16.dp, top = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Settings", color = c.primary, fontSize = 20.sp)
            Text(
                "Back",
                color = c.muted,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(4.dp),
            )
        }

        SectionLabel("Theme")
        AppTheme.entries.forEach { theme ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(theme) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(theme.colors.warm, theme.colors.cool, theme.colors.primary).forEach { swatch ->
                        Box(modifier = Modifier.size(14.dp).background(swatch, CircleShape))
                    }
                }
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
            Hairline()
        }

        if (loaded) {
            SectionLabel("Targets")
            NumberRow("Protein (g/kg)", settings.proteinPerKg.toString()) { v ->
                v.toFloatOrNull()?.let { save(settings.copy(proteinPerKg = it)) }
            }
            NumberRow("Goal rate (% bw/week)", settings.goalRatePercent.toString()) { v ->
                v.toFloatOrNull()?.let { save(settings.copy(goalRatePercent = it)) }
            }

            SectionLabel("Profile")
            Text(
                "Used only for the calorie sanity bounds. Blank disables them.",
                color = c.muted, fontSize = 11.sp,
            )
            NumberRow("Height (cm)", settings.heightCm?.toString() ?: "") { v ->
                save(settings.copy(heightCm = v.toFloatOrNull()))
            }
            NumberRow("Birth year", settings.birthYear?.toString() ?: "") { v ->
                save(settings.copy(birthYear = v.toIntOrNull()))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = when (settings.sex) {
                            null -> Sex.MALE
                            Sex.MALE -> Sex.FEMALE
                            Sex.FEMALE -> null
                        }
                        save(settings.copy(sex = next))
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Sex", color = c.muted, fontSize = 14.sp)
                Text(
                    when (settings.sex) {
                        Sex.MALE -> "Male"
                        Sex.FEMALE -> "Female"
                        null -> "—"
                    },
                    color = c.primary, fontSize = 14.sp,
                )
            }
            Hairline()

            SectionLabel("Block")
            // §3.4.1 — clears the protein ratchet to current weight.
            Text(
                "Start new block",
                color = c.cool,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable {
                        save(settings.copy(blockStartDate = LocalDate.now(), ratchetWeightKg = null))
                    }
                    .padding(vertical = 12.dp),
            )
            settings.blockStartDate?.let {
                Text("Current block since $it.", color = c.muted, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = LocalThemeColors.current
    Spacer(modifier = Modifier.height(24.dp))
    Text(text, color = c.muted, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun Hairline() {
    val c = LocalThemeColors.current
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
}

@Composable
private fun NumberRow(label: String, initial: String, onCommit: (String) -> Unit) {
    val c = LocalThemeColors.current
    var text by remember(label) { mutableStateOf(initial) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.muted, fontSize = 14.sp)
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onCommit(it)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(
                color = c.primary, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            ),
            cursorBrush = SolidColor(c.cool),
            modifier = Modifier.width(96.dp).background(c.surface).padding(6.dp),
        )
    }
    Hairline()
}
