package io.github.professorquantumuniverse.climit.widget

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import io.github.professorquantumuniverse.climit.R
import io.github.professorquantumuniverse.climit.data.UsageLimit
import io.github.professorquantumuniverse.climit.data.UsageSnapshot
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Colours
// ---------------------------------------------------------------------------------------------

/** Resolved colours as ARGB ints, ready for Canvas drawing and Glance text. */
class WidgetTones(
    /** Card colour including its opacity, or 0 for no card. */
    val container: Int,
    /** Card colour without opacity, used for chips and tiles on the card. */
    val containerSolid: Int,
    val content: Int,
    val muted: Int,
    val track: Int,
    val outline: Int,
    val warning: Int,
    private val accents: IntArray,
    private val error: Int,
    private val mode: IndicatorColors,
    private val warnAt: Float,
    private val criticalAt: Float,
) {
    fun accent(index: Int, usedPercent: Double?): Int = when (mode) {
        IndicatorColors.ROLES -> accents[index % accents.size]
        IndicatorColors.SINGLE -> accents[0]
        IndicatorColors.LEVEL -> when {
            usedPercent == null -> accents[0]
            usedPercent >= criticalAt -> error
            usedPercent >= warnAt -> accents[1]
            else -> accents[0]
        }
    }
}

fun isDark(context: Context, theme: ThemeMode): Boolean = when (theme) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM ->
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

/** The Material colour scheme a widget draws from. */
fun widgetColorScheme(context: Context, config: WidgetConfig): ColorScheme {
    val dark = isDark(context, config.theme)
    return when (config.source) {
        ColorSource.DYNAMIC -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        ColorSource.CUSTOM -> seedScheme(config.customHue, dark)
        else -> seedScheme(config.source.hue, dark)
    }
}

fun widgetTones(context: Context, config: WidgetConfig): WidgetTones {
    val s = widgetColorScheme(context, config)
    data class Roles(val container: Color, val content: Color, val muted: Color, val accents: List<Color>)

    val roles = when (config.container) {
        ContainerRole.SURFACE, ContainerRole.NONE ->
            Roles(s.surfaceContainer, s.onSurface, s.onSurfaceVariant, listOf(s.primary, s.tertiary, s.secondary))
        ContainerRole.SURFACE_HIGH ->
            Roles(s.surfaceContainerHighest, s.onSurface, s.onSurfaceVariant, listOf(s.primary, s.tertiary, s.secondary))
        ContainerRole.PRIMARY -> Roles(
            s.primaryContainer, s.onPrimaryContainer, s.onPrimaryContainer.copy(alpha = 0.72f),
            listOf(s.onPrimaryContainer, s.tertiary, s.secondary),
        )
        ContainerRole.SECONDARY -> Roles(
            s.secondaryContainer, s.onSecondaryContainer, s.onSecondaryContainer.copy(alpha = 0.72f),
            listOf(s.onSecondaryContainer, s.primary, s.tertiary),
        )
        ContainerRole.TERTIARY -> Roles(
            s.tertiaryContainer, s.onTertiaryContainer, s.onTertiaryContainer.copy(alpha = 0.72f),
            listOf(s.onTertiaryContainer, s.primary, s.secondary),
        )
        ContainerRole.INVERSE -> Roles(
            s.inverseSurface, s.inverseOnSurface, s.inverseOnSurface.copy(alpha = 0.72f),
            listOf(s.inversePrimary, s.inverseOnSurface, s.inversePrimary.copy(alpha = 0.7f)),
        )
    }
    // Without a card the text sits on the wallpaper, where the surface colours read best.
    val opacity = if (config.blur) config.containerOpacity.coerceIn(0.01f, 0.99f) else config.containerOpacity
    return WidgetTones(
        container = if (config.container == ContainerRole.NONE) 0 else roles.container.copy(alpha = opacity).toArgb(),
        containerSolid = roles.container.toArgb(),
        content = roles.content.toArgb(),
        muted = roles.muted.toArgb(),
        track = roles.content.copy(alpha = 0.14f).toArgb(),
        outline = roles.content.copy(alpha = 0.32f).toArgb(),
        warning = s.error.toArgb(),
        accents = roles.accents.map { it.toArgb() }.toIntArray(),
        error = s.error.toArgb(),
        mode = config.indicatorColors,
        warnAt = config.warnAt,
        criticalAt = config.criticalAt,
    )
}

/**
 * A Material colour scheme from one hue, built from HCT tones the way dynamic colour does it:
 * accents at tone 80 (dark) or 40 (light), containers at 30 or 90, near-neutral surfaces.
 */
fun seedScheme(hue: Float, dark: Boolean): ColorScheme {
    fun t(h: Float, chroma: Float, tone: Float) = Color(ColorUtils.M3HCTToColor(h.mod(360f), chroma, tone))
    val p = hue
    val tert = hue + 60f
    val err = 25f
    return if (dark) {
        darkColorScheme(
            primary = t(p, 52f, 80f), onPrimary = t(p, 52f, 20f),
            primaryContainer = t(p, 48f, 30f), onPrimaryContainer = t(p, 40f, 90f),
            inversePrimary = t(p, 52f, 40f),
            secondary = t(p, 18f, 80f), onSecondary = t(p, 18f, 20f),
            secondaryContainer = t(p, 18f, 30f), onSecondaryContainer = t(p, 18f, 90f),
            tertiary = t(tert, 36f, 80f), onTertiary = t(tert, 36f, 20f),
            tertiaryContainer = t(tert, 32f, 30f), onTertiaryContainer = t(tert, 30f, 90f),
            background = t(p, 6f, 6f), onBackground = t(p, 6f, 90f),
            surface = t(p, 6f, 6f), onSurface = t(p, 6f, 90f),
            surfaceVariant = t(p, 10f, 30f), onSurfaceVariant = t(p, 10f, 80f),
            surfaceContainerLowest = t(p, 6f, 4f), surfaceContainerLow = t(p, 6f, 10f),
            surfaceContainer = t(p, 8f, 12f), surfaceContainerHigh = t(p, 8f, 17f),
            surfaceContainerHighest = t(p, 8f, 22f),
            inverseSurface = t(p, 6f, 90f), inverseOnSurface = t(p, 6f, 20f),
            outline = t(p, 10f, 60f), outlineVariant = t(p, 10f, 30f),
            error = t(err, 84f, 80f), onError = t(err, 84f, 20f),
            errorContainer = t(err, 84f, 30f), onErrorContainer = t(err, 84f, 90f),
        )
    } else {
        lightColorScheme(
            primary = t(p, 52f, 40f), onPrimary = t(p, 52f, 100f),
            primaryContainer = t(p, 40f, 90f), onPrimaryContainer = t(p, 48f, 30f),
            inversePrimary = t(p, 52f, 80f),
            secondary = t(p, 18f, 40f), onSecondary = t(p, 18f, 100f),
            secondaryContainer = t(p, 18f, 90f), onSecondaryContainer = t(p, 18f, 30f),
            tertiary = t(tert, 36f, 40f), onTertiary = t(tert, 36f, 100f),
            tertiaryContainer = t(tert, 32f, 90f), onTertiaryContainer = t(tert, 36f, 30f),
            background = t(p, 6f, 98f), onBackground = t(p, 6f, 10f),
            surface = t(p, 6f, 98f), onSurface = t(p, 6f, 10f),
            surfaceVariant = t(p, 10f, 90f), onSurfaceVariant = t(p, 10f, 30f),
            surfaceContainerLowest = t(p, 6f, 100f), surfaceContainerLow = t(p, 6f, 96f),
            surfaceContainer = t(p, 8f, 94f), surfaceContainerHigh = t(p, 8f, 92f),
            surfaceContainerHighest = t(p, 8f, 90f),
            inverseSurface = t(p, 6f, 20f), inverseOnSurface = t(p, 6f, 95f),
            outline = t(p, 10f, 50f), outlineVariant = t(p, 10f, 80f),
            error = t(err, 84f, 40f), onError = t(err, 84f, 100f),
            errorContainer = t(err, 84f, 90f), onErrorContainer = t(err, 84f, 30f),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Items
// ---------------------------------------------------------------------------------------------

/** One limit as a widget draws it. */
data class WidgetItem(
    val slot: LimitSlot,
    val label: String,
    /** How full the limit is, 0..100 (may exceed 100). */
    val usedPercent: Double,
    /** The number to show: used or remaining, depending on the settings. */
    val shownPercent: Double,
    val resetsAt: Instant?,
    val color: Int,
    /** How much of the limit's time window has passed, 0..1, or null if unknown. */
    val timeFraction: Float?,
) {
    /** Fill of the gauge, 0..1. Follows the shown number. */
    val fraction: Float get() = (shownPercent / 100.0).toFloat().coerceIn(0f, 1f)
}

val SESSION_WINDOW: Duration = Duration.ofHours(5)
val WEEKLY_WINDOW: Duration = Duration.ofDays(7)

/** Share of [window] that has passed when the window ends at [resetsAt]. */
fun timeFraction(now: Instant, resetsAt: Instant?, window: Duration): Float? {
    if (resetsAt == null) return null
    val remaining = Duration.between(now, resetsAt).toMillis().toFloat()
    return (1f - remaining / window.toMillis()).coerceIn(0f, 1f)
}

/**
 * The limits this widget shows, in display order, with labels and colours resolved.
 * Pure apart from [defaultLabel], so it can be unit-tested.
 */
fun buildItems(
    config: WidgetConfig,
    snapshot: UsageSnapshot,
    accent: (index: Int, usedPercent: Double) -> Int,
    now: Instant,
    defaultLabel: (LimitSlot) -> String,
): List<WidgetItem> {
    val limits = buildList<Triple<LimitSlot, String, UsageLimit>> {
        for (slot in config.visibleSlots) {
            when (slot) {
                LimitSlot.SESSION -> snapshot.session?.let {
                    add(Triple(slot, config.labelSession.ifBlank { defaultLabel(slot) }, it))
                }
                LimitSlot.WEEKLY -> snapshot.weekly?.let {
                    add(Triple(slot, config.labelWeekly.ifBlank { defaultLabel(slot) }, it))
                }
                LimitSlot.MODELS -> snapshot.weeklyScoped.forEach {
                    add(Triple(slot, it.modelName ?: defaultLabel(slot), it))
                }
            }
        }
    }
    return limits.mapIndexed { index, (slot, label, limit) ->
        val used = limit.percent
        val window = if (slot == LimitSlot.SESSION) SESSION_WINDOW else WEEKLY_WINDOW
        WidgetItem(
            slot = slot,
            label = label,
            usedPercent = used,
            shownPercent = if (config.showRemaining) (100.0 - used).coerceAtLeast(0.0) else used,
            resetsAt = limit.resetsAt,
            // The colour always follows how full the limit is, even when "remaining" is shown.
            color = accent(index, used),
            timeFraction = timeFraction(now, limit.resetsAt, window),
        )
    }
}

fun widgetItems(context: Context, config: WidgetConfig, tones: WidgetTones, snapshot: UsageSnapshot): List<WidgetItem> =
    buildItems(config, snapshot, tones::accent, Instant.now()) { slot ->
        when (slot) {
            LimitSlot.SESSION -> context.getString(R.string.widget_label_session)
            LimitSlot.WEEKLY -> context.getString(R.string.widget_label_week)
            LimitSlot.MODELS -> context.getString(R.string.config_models)
        }
    }

// ---------------------------------------------------------------------------------------------
// Text
// ---------------------------------------------------------------------------------------------

fun percentText(item: WidgetItem, config: WidgetConfig): String {
    val number = item.shownPercent.roundToInt().toString()
    return if (config.showPercentSign) "$number%" else number
}

data class Countdown(val days: Long, val hours: Long, val minutes: Long)

/** Time left until [resetsAt], or null when it has passed. */
fun countdown(now: Instant, resetsAt: Instant): Countdown? {
    val left = Duration.between(now, resetsAt)
    if (left.isNegative || left.isZero) return null
    return Countdown(left.toDays(), left.toHours() % 24, left.toMinutes() % 60)
}

fun resetText(context: Context, item: WidgetItem, format: ResetFormat): String {
    val resetsAt = item.resetsAt ?: return ""
    return when (format) {
        ResetFormat.CLOCK -> context.getString(R.string.widget_resets, resetClock(context, resetsAt))
        ResetFormat.COUNTDOWN -> {
            val c = countdown(Instant.now(), resetsAt) ?: return context.getString(R.string.reset_due)
            val text = when {
                c.days > 0 -> context.getString(R.string.countdown_days, c.days, c.hours)
                c.hours > 0 -> context.getString(R.string.countdown_hours, c.hours, c.minutes)
                else -> context.getString(R.string.countdown_minutes, c.minutes.coerceAtLeast(1))
            }
            context.getString(R.string.widget_resets_in, text)
        }
    }
}

/**
 * Clock time of [instant], with the weekday when it is not today. An absolute time stays correct
 * between refreshes; a countdown goes stale on the home screen.
 */
fun resetClock(context: Context, instant: Instant): String {
    val zone = ZoneId.systemDefault()
    val local = instant.atZone(zone)
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val time = local.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale))
    val today = Instant.now().atZone(zone).toLocalDate()
    return if (local.toLocalDate() == today) time else local.format(DateTimeFormatter.ofPattern("EEE", locale)) + " " + time
}

/** Data older than this is marked as stale. */
val STALE_AFTER: Duration = Duration.ofMinutes(60)
