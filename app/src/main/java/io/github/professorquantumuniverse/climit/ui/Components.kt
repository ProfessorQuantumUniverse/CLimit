@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.professorquantumuniverse.climit.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.graphics.ColorUtils
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.roundToInt

private val OUTER = 24.dp
private val INNER = 6.dp

/**
 * A titled group of rows in the segmented style of current Material settings screens: every row
 * is its own tonal surface, with large corners on the outside of the group and small ones between.
 */
@Composable
fun SettingsGroup(title: String?, rows: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
            )
        }
        rows.forEachIndexed { index, row ->
            val top = if (index == 0) OUTER else INNER
            val bottom = if (index == rows.lastIndex) OUTER else INNER
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) { row() }
            }
        }
    }
}

/** Leading icon in a tonal circle, as in the Android 16 settings app. */
@Composable
fun RowIcon(icon: ImageVector?) {
    if (icon == null) return
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun RowText(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwitchRow(icon: ImageVector?, title: String, checked: Boolean, onChange: (Boolean) -> Unit, subtitle: String? = null) {
    val haptics = rememberHaptics()
    val change: (Boolean) -> Unit = {
        haptics.toggle(it)
        onChange(it)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { change(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        if (icon != null) Spacer(Modifier.width(16.dp))
        RowText(title, subtitle, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = change,
            thumbContent = if (checked) {
                { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
            } else {
                null
            },
        )
    }
}

@Composable
fun SliderRow(
    icon: ImageVector?,
    title: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    valueText: String? = null,
    track: Brush? = null,
) {
    // A tick on every step of a stepped slider, and every 5 % of a continuous one.
    val haptics = rememberHaptics()
    val notches = if (steps > 0) steps + 1 else 20
    var lastNotch by remember { mutableIntStateOf(-1) }
    val change: (Float) -> Unit = { v ->
        val notch = (((v - range.start) / (range.endInclusive - range.start)) * notches).roundToInt()
        if (notch != lastNotch) {
            if (lastNotch >= 0) {
                if (steps > 0) haptics.tick() else haptics.step()
            }
            lastNotch = notch
        }
        onChange(v)
    }
    val finished: () -> Unit = { lastNotch = -1 }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            if (icon != null) Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (valueText != null) {
                Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (track == null) {
            Slider(value = value, onValueChange = change, valueRange = range, steps = steps, onValueChangeFinished = finished)
        } else {
            // A custom track, e.g. the hue rainbow, under a transparent standard slider.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(track),
                )
                Slider(
                    value = value,
                    onValueChange = change,
                    onValueChangeFinished = finished,
                    valueRange = range,
                    colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
                )
            }
        }
    }
}

/**
 * A connected button group for a single choice: Material 3 Expressive's segmented control, where
 * the selected button turns round and its neighbours keep square inner corners.
 */
@Composable
fun <T> ConnectedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector>? = null,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, (value, label) ->
            ToggleButton(
                checked = value == selected,
                onCheckedChange = {
                    if (it) {
                        haptics.tick()
                        onSelect(value)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                // Unchecked buttons sit on surfaceContainer rows, so they need a higher tone to show.
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                if (icons != null) {
                    Icon(icons[index], contentDescription = null, modifier = Modifier.size(18.dp))
                    if (label.isNotEmpty()) Spacer(Modifier.width(6.dp))
                }
                if (label.isNotEmpty()) Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Title above a connected choice, with an optional icon. */
@Composable
fun <T> ChoiceRow(
    icon: ImageVector?,
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    icons: List<ImageVector>? = null,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            if (icon != null) Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(10.dp))
        ConnectedChoice(options, selected, onSelect, icons = icons)
    }
}

/** A shape that morphs from [start] to [end] as [progress] goes from 0 to 1. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    private val matrix = Matrix()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress, Path())
        // Material shapes live in a unit square; move them to the origin and stretch to the size.
        val bounds = path.getBounds()
        path.translate(androidx.compose.ui.geometry.Offset(-bounds.left, -bounds.top))
        matrix.reset()
        matrix.scale(size.width / bounds.width, size.height / bounds.height)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

/** Animates between two Material shapes, e.g. circle when idle and cookie when selected. */
@Composable
fun rememberMorphShape(start: RoundedPolygon, end: RoundedPolygon, selected: Boolean): Shape {
    val morph = remember(start, end) { Morph(start, end) }
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "morph",
    )
    return remember(morph, progress) { MorphShape(morph, progress) }
}

/** The colour of [hue] at a medium tone, for the hue slider and swatches. */
fun hueColor(hue: Float, tone: Float = 60f, chroma: Float = 60f): Color =
    Color(ColorUtils.M3HCTToColor(hue.mod(360f), chroma, tone))

/** A rainbow brush across all hues, for the custom colour slider. */
fun hueBrush(): Brush = Brush.horizontalGradient((0..12).map { hueColor(it * 30f) })

/** Small solid dot for legends. */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(12.dp)) { drawCircle(color) }
}

/**
 * Haptic feedback with the semantic types Android offers, so the system can pick the right
 * vibration for the device and respect the user's haptics setting.
 */
class Haptics(private val feedback: HapticFeedback) {
    /** Picking one of several options. */
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** Fine steps while dragging. */
    fun step() = feedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)

    fun toggle(on: Boolean) = feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)

    fun longPress() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)

    /** Plain buttons and toolbar actions. */
    fun click() = feedback.performHapticFeedback(HapticFeedbackType.ContextClick)
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
