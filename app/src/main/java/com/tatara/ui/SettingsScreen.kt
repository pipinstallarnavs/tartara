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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.ui.theme.AppTheme
import com.tatara.ui.theme.LocalThemeColors

@Composable
fun SettingsScreen(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalThemeColors.current
    Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 16.dp, top = 16.dp)) {
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

        Spacer(modifier = Modifier.height(24.dp))
        Text("Theme", color = c.muted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        AppTheme.entries.forEach { theme ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(theme) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Swatch previews the candidate theme's own colours.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        theme.colors.warm,
                        theme.colors.cool,
                        theme.colors.primary,
                    ).forEach { swatch ->
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
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        }
    }
}
