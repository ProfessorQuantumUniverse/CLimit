package io.github.professorquantumuniverse.climit.widget

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonObject

/** How the limits are drawn. */
enum class WidgetStyle {
    RINGS, WAVY_RINGS, CONCENTRIC, GAUGE, BARS, WAVY_BARS, SEGMENTS, DOTS, SHAPES, FILL, NUMBERS, COMBO;

    val isRing get() = this == RINGS || this == WAVY_RINGS || this == CONCENTRIC || this == COMBO
    val isWavy get() = this == WAVY_RINGS || this == WAVY_BARS
    val isLinear get() = this == BARS || this == WAVY_BARS || this == SEGMENTS || this == DOTS
    val hasStroke get() = isRing || isLinear || this == GAUGE
    val usesGaugeShape get() = this == SHAPES || this == DOTS
    val usesCount get() = this == SEGMENTS || this == DOTS
    val supportsTimeProgress get() = this != SHAPES && this != NUMBERS && this != CONCENTRIC && this != FILL && this != DOTS
    val supportsArrangement get() = !isLinear && this != CONCENTRIC && this != COMBO
}

/** The limits a widget can show. Their order in [WidgetConfig.order] is the display order. */
enum class LimitSlot { SESSION, WEEKLY, MODELS }

/** Where the colours come from: the wallpaper, a hue picked by the user, or a fixed seed hue. */
enum class ColorSource(val hue: Float) {
    DYNAMIC(0f), CUSTOM(0f), ORANGE(50f), RED(25f), PINK(350f), PURPLE(300f), BLUE(265f), CYAN(200f), GREEN(140f), YELLOW(100f),
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Material colour role of the card. NONE draws no card at all. */
enum class ContainerRole { SURFACE, SURFACE_HIGH, PRIMARY, SECONDARY, TERTIARY, INVERSE, NONE }

enum class IndicatorColors {
    /** One accent role per limit: primary, tertiary, secondary. */
    ROLES,

    /** Every limit in the primary colour. */
    SINGLE,

    /** Primary, then tertiary from [WidgetConfig.warnAt], then error from [WidgetConfig.criticalAt]. */
    LEVEL,
}

/** Material shapes for the "shapes" and "dots" styles. */
enum class GaugeShape { COOKIE, CLOVER, SUNNY, FLOWER, BURST, PUFFY, GEM, PILL, HEART, CIRCLE }

/** Outline of the whole widget: a rounded rectangle or a Material shape stretched to the widget. */
enum class CardShape { ROUNDED, COOKIE4, COOKIE6, COOKIE9, CLOVER4, CLOVER8, SUNNY, PUFFY, PILL, ARCH, SLANTED, GHOSTISH }

enum class TapAction { REFRESH, OPEN_APP, OPEN_CLAUDE, OPEN_SETTINGS, NONE }

enum class ResetFormat { CLOCK, COUNTDOWN }

enum class LimitArrangement { AUTO, ROW, COLUMN }

enum class NumberFont { SYSTEM, FLEX }

/** Everything the user can change about one widget instance. Stored per widget as JSON. */
@Serializable
data class WidgetConfig(
    val version: Int = CURRENT_VERSION,
    val style: WidgetStyle = WidgetStyle.RINGS,

    // Content
    val order: List<LimitSlot> = listOf(LimitSlot.SESSION, LimitSlot.WEEKLY, LimitSlot.MODELS),
    val shown: Set<LimitSlot> = setOf(LimitSlot.SESSION, LimitSlot.WEEKLY),
    val arrangement: LimitArrangement = LimitArrangement.AUTO,
    val labelSession: String = "",
    val labelWeekly: String = "",
    val showLabels: Boolean = true,
    val showReset: Boolean = true,
    val resetFormat: ResetFormat = ResetFormat.CLOCK,
    val showPercentSign: Boolean = true,
    /** Show what is left instead of what is used. */
    val showRemaining: Boolean = false,
    val labelSize: Float = 0.5f,
    val resetSize: Float = 0.5f,
    val numberSize: Float = 0.5f,
    val numberFont: NumberFont = NumberFont.FLEX,
    /** 0..1, mapped to the font weight axis. */
    val numberWeight: Float = 0.7f,
    /** 0..1, mapped to the font width axis. */
    val numberWidth: Float = 0.5f,

    // Colour
    val source: ColorSource = ColorSource.DYNAMIC,
    val customHue: Float = 265f,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val indicatorColors: IndicatorColors = IndicatorColors.ROLES,
    val warnAt: Float = 70f,
    val criticalAt: Float = 90f,

    // Card
    val container: ContainerRole = ContainerRole.SURFACE,
    /** 0 = card fully transparent, 1 = solid. */
    val containerOpacity: Float = 1f,
    val cardShape: CardShape = CardShape.ROUNDED,
    val cornerDp: Float = 28f,
    val outline: Boolean = false,
    /** Ask One UI Home to blur the wallpaper behind the card. Has no effect on other launchers. */
    val blur: Boolean = false,
    /** 0..1: inner padding of the card. */
    val padding: Float = 0.5f,
    /** 0..1: space between the limits. */
    val spacing: Float = 0.5f,

    // Gauges
    /** 0..1: stroke thickness of rings and bars. */
    val thickness: Float = 0.5f,
    val waveAmplitude: Float = 0.5f,
    val waveLength: Float = 0.4f,
    val gaugeShape: GaugeShape = GaugeShape.COOKIE,
    /** Number of segments or dots in the segmented and dotted styles. */
    val count: Int = 10,
    /** Ring with a gap at the bottom (270°) instead of a full circle. */
    val ringOpen: Boolean = false,
    /** Where a full ring starts, in degrees clockwise from the top. */
    val ringStart: Float = 0f,
    val clockwise: Boolean = true,
    /** Thin second gauge for how much of the time window has passed. */
    val showTimeProgress: Boolean = false,

    // Behaviour
    val tapAction: TapAction = TapAction.REFRESH,
    val showUpdated: Boolean = false,
    /** Point out numbers that are more than an hour old. */
    val staleHint: Boolean = true,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    /** Takes over the look of [preset] but keeps what this widget shows and how it behaves. */
    fun withLookOf(preset: WidgetConfig): WidgetConfig = preset.copy(
        order = order,
        shown = shown,
        labelSession = labelSession,
        labelWeekly = labelWeekly,
        tapAction = tapAction,
        showUpdated = showUpdated,
        staleHint = staleHint,
    )

    /** Limits in display order, only the ones switched on. */
    val visibleSlots: List<LimitSlot> get() = order.filter { it in shown }

    companion object {
        const val CURRENT_VERSION = 2

        val KEY = stringPreferencesKey("config")

        /** Default for the compact widget: one Material shape, nothing else. */
        val COMPACT = WidgetConfig(
            style = WidgetStyle.SHAPES,
            shown = setOf(LimitSlot.SESSION),
            gaugeShape = GaugeShape.COOKIE,
            container = ContainerRole.PRIMARY,
            cardShape = CardShape.COOKIE9,
            showReset = false,
        )

        // Old or damaged settings must never break a widget: unknown fields are ignored and
        // anything unreadable falls back to the defaults.
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }

        fun from(preferences: Preferences, defaults: WidgetConfig = WidgetConfig()): WidgetConfig =
            fromJson(preferences[KEY], defaults)

        fun fromJson(text: String?, defaults: WidgetConfig = WidgetConfig()): WidgetConfig {
            if (text.isNullOrBlank()) return defaults
            return try {
                val element = json.parseToJsonElement(text).jsonObject
                json.decodeFromJsonElement(serializer(), migrate(element)).sanitized()
            } catch (_: Exception) {
                defaults
            }
        }

        /** Version 1 had one switch per limit and called the gauge shape "shape". */
        private fun migrate(o: JsonObject): JsonElement {
            val version = (o["version"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 1
            if (version >= 2) return o
            val map = o.toMutableMap()
            fun flag(key: String, default: Boolean) = (o[key] as? JsonPrimitive)?.booleanOrNull ?: default
            map["shown"] = buildJsonArray {
                if (flag("showSession", true)) add(JsonPrimitive(LimitSlot.SESSION.name))
                if (flag("showWeekly", true)) add(JsonPrimitive(LimitSlot.WEEKLY.name))
                if (flag("showModels", false)) add(JsonPrimitive(LimitSlot.MODELS.name))
            }
            o["shape"]?.let { map["gaugeShape"] = it }
            map["version"] = JsonPrimitive(CURRENT_VERSION)
            return JsonObject(map)
        }
    }

    /** Clamps every number into its range, so a hand-edited import cannot produce nonsense. */
    fun sanitized(): WidgetConfig = copy(
        version = CURRENT_VERSION,
        order = (order.distinct() + LimitSlot.entries).distinct(),
        labelSession = labelSession.take(MAX_LABEL),
        labelWeekly = labelWeekly.take(MAX_LABEL),
        labelSize = labelSize.coerceIn(0f, 1f),
        resetSize = resetSize.coerceIn(0f, 1f),
        numberSize = numberSize.coerceIn(0f, 1f),
        numberWeight = numberWeight.coerceIn(0f, 1f),
        numberWidth = numberWidth.coerceIn(0f, 1f),
        customHue = customHue.mod(360f),
        warnAt = warnAt.coerceIn(1f, 99f),
        criticalAt = criticalAt.coerceIn(warnAt.coerceIn(1f, 99f) + 1f, 100f),
        containerOpacity = containerOpacity.coerceIn(0f, 1f),
        cornerDp = cornerDp.coerceIn(0f, 48f),
        padding = padding.coerceIn(0f, 1f),
        spacing = spacing.coerceIn(0f, 1f),
        thickness = thickness.coerceIn(0f, 1f),
        waveAmplitude = waveAmplitude.coerceIn(0f, 1f),
        waveLength = waveLength.coerceIn(0f, 1f),
        count = count.coerceIn(MIN_COUNT, MAX_COUNT),
        ringStart = ringStart.mod(360f),
    )
}

const val MAX_LABEL = 24
const val MIN_COUNT = 4
const val MAX_COUNT = 24

/** Ready-made looks. Applying one keeps the widget's content and behaviour settings. */
enum class Preset(val config: WidgetConfig) {
    PIXEL(WidgetConfig()),
    EXPRESSIVE(
        WidgetConfig(
            style = WidgetStyle.WAVY_RINGS,
            container = ContainerRole.PRIMARY,
            thickness = 0.7f,
            numberWeight = 0.95f,
            numberWidth = 0.85f,
            numberSize = 0.65f,
            cornerDp = 40f,
        )
    ),
    SHAPES(
        WidgetConfig(
            style = WidgetStyle.SHAPES,
            gaugeShape = GaugeShape.CLOVER,
            container = ContainerRole.TERTIARY,
            cornerDp = 36f,
        )
    ),
    MINIMAL(
        WidgetConfig(
            style = WidgetStyle.NUMBERS,
            container = ContainerRole.NONE,
            numberWeight = 0.9f,
            numberWidth = 1f,
            numberSize = 0.8f,
            showReset = false,
        )
    ),
    BARS(
        WidgetConfig(
            style = WidgetStyle.WAVY_BARS,
            shown = setOf(LimitSlot.SESSION, LimitSlot.WEEKLY, LimitSlot.MODELS),
            showTimeProgress = true,
            container = ContainerRole.SURFACE_HIGH,
        )
    ),
    GAUGE(
        WidgetConfig(
            style = WidgetStyle.GAUGE,
            container = ContainerRole.SECONDARY,
            thickness = 0.65f,
            showTimeProgress = true,
        )
    ),
    CONTRAST(
        WidgetConfig(
            style = WidgetStyle.FILL,
            container = ContainerRole.INVERSE,
            indicatorColors = IndicatorColors.LEVEL,
            cornerDp = 24f,
        )
    ),
    GLASS(
        WidgetConfig(
            style = WidgetStyle.RINGS,
            containerOpacity = 0.45f,
            blur = true,
            outline = true,
        )
    ),
    MONO(
        WidgetConfig(
            style = WidgetStyle.CONCENTRIC,
            shown = setOf(LimitSlot.SESSION, LimitSlot.WEEKLY, LimitSlot.MODELS),
            indicatorColors = IndicatorColors.SINGLE,
            thickness = 0.35f,
        )
    ),
    COMBO(
        WidgetConfig(
            style = WidgetStyle.COMBO,
            shown = setOf(LimitSlot.SESSION, LimitSlot.WEEKLY, LimitSlot.MODELS),
            container = ContainerRole.PRIMARY,
            ringOpen = true,
        )
    ),
    SEGMENTS(
        WidgetConfig(
            style = WidgetStyle.SEGMENTS,
            source = ColorSource.ORANGE,
            count = 12,
            thickness = 0.7f,
        )
    ),
    DOTS(
        WidgetConfig(
            style = WidgetStyle.DOTS,
            gaugeShape = GaugeShape.SUNNY,
            source = ColorSource.GREEN,
            count = 10,
        )
    ),
}
