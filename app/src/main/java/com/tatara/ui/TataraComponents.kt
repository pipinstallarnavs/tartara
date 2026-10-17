package com.tatara.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatara.ui.theme.LocalThemeColors

private enum class ActionStyle { PRIMARY, SECONDARY, GHOST, DESTRUCTIVE }

@Composable
private fun TataraAction(
    label: String,
    onClick: () -> Unit,
    style: ActionStyle,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
) {
    val c = LocalThemeColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val interactive = enabled && !loading
    val container = when {
        !interactive -> c.surface
        pressed && style == ActionStyle.PRIMARY -> c.cool.copy(alpha = 0.78f)
        pressed -> c.surfaceHigh
        style == ActionStyle.PRIMARY -> c.cool
        style == ActionStyle.DESTRUCTIVE -> c.surface
        else -> Color.Transparent
    }
    val border = when (style) {
        ActionStyle.PRIMARY -> if (interactive) c.cool else c.hairline
        ActionStyle.SECONDARY, ActionStyle.GHOST -> c.muted.copy(alpha = if (interactive) 0.72f else 0.35f)
        ActionStyle.DESTRUCTIVE -> if (interactive) c.warm else c.hairline
    }
    val content = when {
        !interactive -> c.muted.copy(alpha = 0.55f)
        style == ActionStyle.PRIMARY -> c.ground
        style == ActionStyle.DESTRUCTIVE -> c.warm
        else -> c.primary
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = TataraSizing.MinimumTouch, minHeight = TataraSizing.ControlHeight)
            .clip(TataraShapes.Control)
            .background(container)
            .border(1.dp, border, TataraShapes.Control)
            .clickable(
                enabled = interactive,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = TataraSpacing.Standard),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (loading) "Working…" else label,
            color = content,
            fontSize = TataraType.Action,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun TataraPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) = TataraAction(label, onClick, ActionStyle.PRIMARY, modifier, enabled, loading)

@Composable
fun TataraSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) = TataraAction(label, onClick, ActionStyle.SECONDARY, modifier, enabled, loading)

@Composable
fun TataraGhostAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = TataraAction(label, onClick, ActionStyle.GHOST, modifier, enabled, loading = false)

@Composable
fun TataraDestructiveButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = TataraAction(label, onClick, ActionStyle.DESTRUCTIVE, modifier, enabled, loading = false)

@Composable
fun TataraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    suffix: String? = null,
    enabled: Boolean = true,
    error: String? = null,
    helper: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    monospace: Boolean = false,
) {
    val c = LocalThemeColors.current
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        !enabled -> c.hairline
        error != null -> c.warm
        focused -> c.cool
        else -> c.muted.copy(alpha = 0.72f)
    }
    val textColor = if (enabled) c.primary else c.muted.copy(alpha = 0.55f)

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = if (enabled) c.primary.copy(alpha = 0.68f) else c.muted.copy(alpha = 0.5f),
            fontSize = TataraType.Label,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
        Spacer(modifier = Modifier.height(TataraSpacing.Micro))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            textStyle = TextStyle(
                color = textColor,
                fontSize = TataraType.Body,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            ),
            cursorBrush = SolidColor(c.cool),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = TataraSizing.ControlHeight)
                        .background(c.surface, TataraShapes.Control)
                        .border(1.dp, borderColor, TataraShapes.Control)
                        .padding(start = TataraSpacing.Control),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                placeholder,
                                color = c.muted.copy(alpha = if (enabled) 0.72f else 0.42f),
                                fontSize = TataraType.Body,
                                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                            )
                        }
                        inner()
                    }
                    suffix?.let {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(if (error != null) c.warm.copy(alpha = 0.4f) else c.hairline)
                        )
                        Text(
                            it,
                            color = c.muted,
                            fontSize = TataraType.Label,
                            modifier = Modifier.padding(horizontal = TataraSpacing.Control),
                        )
                    } ?: Spacer(modifier = Modifier.width(TataraSpacing.Control))
                }
            },
        )
        val supporting = error ?: helper
        if (supporting != null) {
            Text(
                supporting,
                color = if (error != null) c.warm else c.muted,
                fontSize = TataraType.Label,
                modifier = Modifier.padding(top = TataraSpacing.Micro),
            )
        }
    }
}

@Composable
fun TataraNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    suffix: String? = null,
    enabled: Boolean = true,
    error: String? = null,
) = TataraTextField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    placeholder = placeholder,
    suffix = suffix,
    enabled = enabled,
    error = error,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    monospace = true,
)

@Composable
fun TataraTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "23:00",
    error: String? = null,
) = TataraTextField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    placeholder = placeholder,
    suffix = "24H",
    error = error,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    monospace = true,
)

@Composable
fun TataraStepper(
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decreaseEnabled: Boolean = true,
    increaseEnabled: Boolean = true,
) {
    val c = LocalThemeColors.current
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            color = c.primary.copy(alpha = 0.68f),
            fontSize = TataraType.Label,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
        Spacer(modifier = Modifier.height(TataraSpacing.Micro))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TataraSizing.ControlHeight)
                .background(c.surface, TataraShapes.Control)
                .border(1.dp, c.muted.copy(alpha = 0.72f), TataraShapes.Control),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton("−", decreaseEnabled, onDecrease)
            Text(
                value.toString(),
                color = c.primary,
                fontSize = TataraType.Metric,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            StepperButton("+", increaseEnabled, onIncrease)
        }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalThemeColors.current
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(TataraSizing.ControlHeight)
            .background(if (enabled) c.surfaceHigh else c.surface, TataraShapes.Control)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) c.primary else c.muted.copy(alpha = 0.45f), fontSize = 20.sp)
    }
}

@Composable
fun <T> TataraSegmentedControl(
    items: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    label: String,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    selectedColor: Color? = null,
    enabled: Boolean = true,
) {
    require(items.isNotEmpty())
    val c = LocalThemeColors.current
    val accent = selectedColor ?: c.cool
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            color = c.primary.copy(alpha = 0.68f),
            fontSize = TataraType.Label,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
        Spacer(modifier = Modifier.height(TataraSpacing.Micro))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TataraSizing.ControlHeight)
                .clip(TataraShapes.Control)
                .background(c.surface)
                .border(1.dp, c.muted.copy(alpha = 0.72f), TataraShapes.Control),
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = item == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TataraSizing.ControlHeight)
                        .background(if (isSelected) accent else Color.Transparent)
                        .selectable(
                            selected = isSelected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onSelected(item) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(1.dp)
                                .height(28.dp)
                                .background(c.hairline)
                        )
                    }
                    Text(
                        itemLabel(item),
                        color = when {
                            !enabled -> c.muted.copy(alpha = 0.45f)
                            isSelected -> c.ground
                            else -> c.primary
                        },
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
fun TataraChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalThemeColors.current
    Box(
        modifier = modifier
            .heightIn(min = TataraSizing.MinimumTouch)
            .background(if (selected) c.surfaceHigh else c.surface, TataraShapes.Control)
            .border(1.dp, if (selected) c.cool else c.muted.copy(alpha = 0.72f), TataraShapes.Control)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = TataraSpacing.Control),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) c.primary else c.muted, fontSize = 12.sp)
    }
}

@Composable
fun TataraCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalThemeColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TataraSizing.MinimumTouch)
            .clickable(enabled = enabled, role = Role.Checkbox) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MotifMark(
            checked = checked,
            size = 24.dp,
        )
        Spacer(modifier = Modifier.width(TataraSpacing.Control))
        Text(label, color = if (enabled) c.primary else c.muted.copy(alpha = 0.45f), fontSize = TataraType.Body)
    }
}

@Composable
fun TataraDivider(modifier: Modifier = Modifier) {
    val c = LocalThemeColors.current
    Box(modifier = modifier.fillMaxWidth().height(TataraSizing.Divider).background(c.hairline))
}

@Composable
fun TataraSectionHeader(title: String, modifier: Modifier = Modifier) {
    val c = LocalThemeColors.current
    Column(modifier = modifier.fillMaxWidth().padding(top = TataraSpacing.Group)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MotifGlyph(size = 9.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                title.uppercase(),
                color = c.primary.copy(alpha = 0.68f),
                fontSize = TataraType.SectionLabel,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            )
        }
        Spacer(modifier = Modifier.height(TataraSpacing.Tight))
        TataraDivider()
        Spacer(modifier = Modifier.height(TataraSpacing.Control))
    }
}

@Composable
fun TataraEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val c = LocalThemeColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = TataraSpacing.Section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TataraSpacing.Tight),
    ) {
        MotifGlyph(size = 24.dp)
        Text(title, color = c.primary, fontSize = 18.sp, fontFamily = FontFamily.Serif)
        Text(message, color = c.muted, fontSize = 12.sp)
        if (
            (primaryActionLabel != null && onPrimaryAction != null) ||
            (secondaryActionLabel != null && onSecondaryAction != null)
        ) {
            Spacer(modifier = Modifier.height(TataraSpacing.Micro))
            Row(horizontalArrangement = Arrangement.spacedBy(TataraSpacing.Tight)) {
                if (primaryActionLabel != null && onPrimaryAction != null) {
                    TataraPrimaryButton(primaryActionLabel, onPrimaryAction)
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    TataraSecondaryButton(secondaryActionLabel, onSecondaryAction)
                }
            }
        }
    }
}

enum class TataraNavIcon { DASHBOARD, FOOD, TRAIN, HABITS, SLEEP }

@Composable
fun <T> TataraBottomNavigation(
    items: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
    icon: (T) -> TataraNavIcon,
    modifier: Modifier = Modifier,
) {
    val c = LocalThemeColors.current
    Column(modifier = modifier.fillMaxWidth().background(c.surface)) {
        TataraDivider()
        Row(modifier = Modifier.fillMaxWidth().height(TataraSizing.BottomNavigationHeight)) {
            items.forEach { item ->
                val isSelected = item == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TataraSizing.BottomNavigationHeight)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelected(item) },
                        ),
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        TataraNavigationIcon(icon(item), if (isSelected) c.cool else c.muted)
                        Text(
                            label(item),
                            color = if (isSelected) c.primary else c.muted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .width(32.dp)
                                .height(TataraSizing.SelectionMarker)
                                .background(c.cool, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TataraNavigationIcon(icon: TataraNavIcon, color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        when (icon) {
            TataraNavIcon.DASHBOARD -> {
                drawLine(color, Offset(w * 0.16f, h * 0.48f), Offset(w * 0.5f, h * 0.18f), stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.84f, h * 0.48f), stroke)
                drawRect(color, Offset(w * 0.25f, h * 0.44f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.4f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            }
            TataraNavIcon.FOOD -> {
                drawLine(color, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.88f), stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.42f), Offset(w * 0.78f, h * 0.16f), stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.22f, h * 0.36f), stroke)
            }
            TataraNavIcon.TRAIN -> {
                drawRect(color, Offset(w * 0.2f, h * 0.32f), androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.36f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawLine(color, Offset(w * 0.08f, h * 0.24f), Offset(w * 0.08f, h * 0.76f), stroke)
                drawLine(color, Offset(w * 0.92f, h * 0.24f), Offset(w * 0.92f, h * 0.76f), stroke)
            }
            TataraNavIcon.HABITS -> {
                drawLine(color, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.88f), stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.48f), Offset(w * 0.2f, h * 0.22f), stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.8f, h * 0.32f), stroke)
            }
            TataraNavIcon.SLEEP -> {
                drawCircle(color, w * 0.36f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawLine(color, center, Offset(w * 0.5f, h * 0.26f), stroke)
                drawLine(color, center, Offset(w * 0.7f, h * 0.58f), stroke)
            }
        }
    }
}
