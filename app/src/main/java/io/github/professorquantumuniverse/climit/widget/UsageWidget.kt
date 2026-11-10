package io.github.professorquantumuniverse.climit.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.professorquantumuniverse.climit.MainActivity
import io.github.professorquantumuniverse.climit.R
import io.github.professorquantumuniverse.climit.data.SessionRepository
import io.github.professorquantumuniverse.climit.data.UsageState
import java.time.Duration
import java.time.Instant
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** The usage widget. Its look is set per instance by a [WidgetConfig] in the widget's own state. */
class UsageWidget : GlanceAppWidget() {

    // Exact sizes, because the gauges are bitmaps drawn for the actual widget size.
    override val sizeMode = SizeMode.Exact

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) {
            // The settings preview composes with a fake id.
            AppWidgetManager.INVALID_APPWIDGET_ID
        }
        val defaults = defaultsFor(context, appWidgetId)
        val initial = SessionRepository.state.value ?: SessionRepository(context).load()
        provideContent {
            // Collected here so a background refresh redraws a widget that is already composed.
            val state by SessionRepository.state.collectAsState()
            val config = WidgetConfig.from(currentState<Preferences>(), defaults)
            Content(state ?: initial, config, appWidgetId)
        }
    }

    companion object {
        /** Settings a widget starts with, which depend on the entry it was added from. */
        fun defaultsFor(context: Context, appWidgetId: Int): WidgetConfig {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return WidgetConfig()
            val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider?.className
            return if (provider == UsageWidgetCompactReceiver::class.java.name) WidgetConfig.COMPACT else WidgetConfig()
        }
    }
}

open class UsageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshWorker.schedule(context)
        RefreshWorker.runOnce(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Each entry gets its own onDisabled; keep refreshing while any CLimit widget is left.
        val manager = AppWidgetManager.getInstance(context)
        val left = listOf(UsageWidgetReceiver::class.java, UsageWidgetCompactReceiver::class.java).sumOf {
            manager.getAppWidgetIds(android.content.ComponentName(context, it)).size
        }
        if (left == 0) RefreshWorker.cancel(context)
    }
}

/** Second entry in the widget picker: small, one limit, a Material shape. */
class UsageWidgetCompactReceiver : UsageWidgetReceiver()

/** Fetches fresh numbers. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        RefreshWorker.runOnce(context)
    }
}

private const val CLAUDE_PACKAGE = "com.anthropic.claude"

private fun tapAction(context: Context, config: WidgetConfig, appWidgetId: Int): Action? = when (config.tapAction) {
    TapAction.REFRESH -> actionRunCallback<RefreshAction>()
    TapAction.OPEN_APP -> actionStartActivity<MainActivity>()
    TapAction.OPEN_CLAUDE -> actionStartActivity(
        (context.packageManager.getLaunchIntentForPackage(CLAUDE_PACKAGE) ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://claude.ai")))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    TapAction.OPEN_SETTINGS -> if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        null
    } else {
        actionStartActivity(
            Intent(context, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                // Makes the intent unique per widget, so each opens its own settings.
                .setData(Uri.parse("climit://widget/$appWidgetId"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
    TapAction.NONE -> null
}

// ---------------------------------------------------------------------------------------------
// Layout
// ---------------------------------------------------------------------------------------------

/** Everything the style layouts need, resolved once per composition. */
private class Ctx(
    val context: Context,
    val config: WidgetConfig,
    val tones: WidgetTones,
    val density: Float,
    val fontScale: Float,
) {
    val content = ColorProvider(Color(tones.content))
    val muted = ColorProvider(Color(tones.muted))
    val spacing: Dp = (4f + 16f * config.spacing).dp
    val labelSp = 10f + 6f * config.labelSize
    val resetSp = 9f + 5f * config.resetSize

    fun line(sp: Float): Dp = (sp * 1.32f * fontScale).dp
    fun px(dp: Dp): Int = (dp.value * density).roundToInt().coerceAtLeast(1)
    fun dp(px: Int): Dp = (px / density).dp

    /** Second, thin gauge colour for the time window. */
    val timeColor: Int get() = if (config.showTimeProgress) tones.content else 0
}

@Composable
private fun Content(state: UsageState, config: WidgetConfig, appWidgetId: Int) {
    val context = LocalContext.current
    val size = LocalSize.current
    val metrics = context.resources.displayMetrics
    val c = Ctx(context, config, widgetTones(context, config), metrics.density, context.resources.configuration.fontScale)

    // One UI Home blurs the wallpaper behind a translucent android:id/background; it handles the
    // corners itself, so a blurred card is always a plain rounded rectangle.
    val shape = if (config.blur) CardShape.ROUNDED else config.cardShape
    val hasCard = c.tones.container != 0
    val bitmapCard = hasCard && !config.blur && (shape != CardShape.ROUNDED || config.outline)

    var root = GlanceModifier.fillMaxSize().appWidgetBackground()
    if (hasCard && !bitmapCard) {
        root = root.background(ColorProvider(Color(c.tones.container))).cornerRadius(config.cornerDp.dp)
    }
    tapAction(context, config, appWidgetId)?.let { root = root.clickable(it) }

    val items = state.snapshot?.let { widgetItems(context, config, c.tones, it) }.orEmpty()
    root = root.semantics {
        contentDescription = items.joinToString(", ") {
            context.getString(R.string.widget_a11y_item, it.label, it.shownPercent.roundToInt())
        }
    }

    Box(modifier = root, contentAlignment = Alignment.Center) {
        if (bitmapCard || (config.blur && config.outline && hasCard)) {
            val card = WidgetPainter.card(
                widthPx = c.px(size.width),
                heightPx = c.px(size.height),
                shape = shape,
                cornerPx = config.cornerDp * c.density,
                fill = if (config.blur) 0 else c.tones.container,
                outline = if (config.outline) c.tones.outline else 0,
                outlineWidth = 1.5f * c.density,
            )
            Image(ImageProvider(card), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }

        val shortSide = minOf(size.width.value, size.height.value)
        // Small widgets get tighter insets, otherwise the shape would swallow the content.
        val shrink = if (shortSide < 130f) 0.5f else 1f
        val insets = WidgetPainter.cardInsets(shape, size.width.value, size.height.value)
        val base = 4f + 16f * config.padding
        // Never let padding eat more than a fifth of a side.
        fun side(inset: Float, length: Float) = minOf(base + inset * shrink, length * 0.2f).dp
        val padStart = side(insets[0], size.width.value)
        val padTop = side(insets[1], size.height.value)
        val padEnd = side(insets[2], size.width.value)
        val padBottom = side(insets[3], size.height.value)

        val fetchedAt = state.fetchedAt
        val stale = fetchedAt != null && Duration.between(fetchedAt, Instant.now()) > STALE_AFTER
        val footer = when {
            items.isEmpty() || fetchedAt == null -> null
            stale && config.staleHint -> context.getString(R.string.widget_stale, resetClock(context, fetchedAt))
            config.showUpdated -> context.getString(R.string.widget_updated, resetClock(context, fetchedAt))
            else -> null
        }
        val footerHeight = if (footer != null) c.line(10f) + 2.dp else 0.dp
        val area = DpSize(
            (size.width - padStart - padEnd).coerceAtLeast(16.dp),
            (size.height - padTop - padBottom - footerHeight).coerceAtLeast(16.dp),
        )

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(start = padStart, top = padTop, end = padEnd, bottom = padBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    !state.signedIn || state.sessionExpired -> Message(context.getString(R.string.widget_sign_in), c.content)
                    items.isEmpty() -> Message(context.getString(R.string.widget_loading), c.muted)
                    else -> Body(c, items, area)
                }
            }
            if (footer != null) {
                Text(
                    text = footer,
                    style = TextStyle(
                        color = if (stale && config.staleHint) ColorProvider(Color(c.tones.warning)) else c.muted,
                        fontSize = 10.sp,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun Body(c: Ctx, all: List<WidgetItem>, area: DpSize) {
    val style = c.config.style
    when {
        minOf(area.width.value, area.height.value) < 64f -> Tiny(c, all.first(), area)
        style.isLinear -> Linear(c, all, area)
        style == WidgetStyle.CONCENTRIC -> Concentric(c, all, area)
        style == WidgetStyle.COMBO -> Combo(c, all, area)
        else -> {
            val horizontal = when (c.config.arrangement) {
                LimitArrangement.ROW -> true
                LimitArrangement.COLUMN -> false
                LimitArrangement.AUTO -> area.width.value / all.size.coerceAtMost(3) >= area.height.value * 0.75f
            }
            if (horizontal) GaugeRow(c, all, area) else GaugeColumn(c, all, area)
        }
    }
}

/** How many items of [minSize] fit into [available] with the spacing in between. */
private fun fitCount(available: Dp, minSize: Dp, spacing: Dp): Int =
    floor((available + spacing).value / (minSize + spacing).value).toInt().coerceAtLeast(1)

/** Gauges side by side, captions underneath. */
@Composable
private fun GaugeRow(c: Ctx, all: List<WidgetItem>, area: DpSize) {
    val style = c.config.style
    val items = all.take(fitCount(area.width, 44.dp, c.spacing))
    val labelOn = c.config.showLabels && area.height >= 64.dp
    val resetOn = c.config.showReset && area.height >= 92.dp
    val captionHeight = (if (labelOn) c.line(c.labelSp) else 0.dp) + (if (resetOn) c.line(c.resetSp) else 0.dp)
    val itemWidth = (area.width - c.spacing * (items.size - 1)) / items.size

    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { i, item ->
            if (i > 0) Spacer(GlanceModifier.width(c.spacing))
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (style) {
                    WidgetStyle.FILL -> FillTile(c, item, DpSize(itemWidth, area.height), labelOn, resetOn)
                    WidgetStyle.NUMBERS -> {
                        val sp = min(itemWidth / 2.4f, (area.height - captionHeight) / 1.2f).value * (0.75f + 0.5f * c.config.numberSize)
                        NumberView(c, percentText(item, c.config), sp.coerceIn(14f, 80f), item.color, maxWidth = itemWidth * 0.92f)
                        Caption(c, item, labelOn, resetOn)
                    }
                    WidgetStyle.SHAPES -> {
                        val numberSp = 13f + 9f * c.config.numberSize
                        val gauge = min(itemWidth, area.height - captionHeight - c.line(numberSp)).coerceAtLeast(20.dp)
                        Image(
                            ImageProvider(WidgetPainter.shape(c.px(gauge), item.fraction, c.config.gaugeShape, item.color, c.tones.track)),
                            null,
                            GlanceModifier.size(gauge),
                        )
                        NumberView(c, percentText(item, c.config), numberSp, c.tones.content)
                        Caption(c, item, labelOn, resetOn)
                    }
                    WidgetStyle.GAUGE -> {
                        val maxHeight = area.height - captionHeight
                        val width = min(itemWidth, maxHeight * 1.75f).coerceAtLeast(24.dp)
                        Gauge(c, item, width)
                        Caption(c, item, labelOn, resetOn)
                    }
                    else -> {
                        val gauge = min(itemWidth, area.height - captionHeight).coerceAtLeast(24.dp)
                        Ring(c, item, gauge, withNumber = true)
                        Caption(c, item, labelOn, resetOn)
                    }
                }
            }
        }
    }
}

/** Gauges stacked, each with number and captions to its right. */
@Composable
private fun GaugeColumn(c: Ctx, all: List<WidgetItem>, area: DpSize) {
    val style = c.config.style
    val items = all.take(fitCount(area.height, 36.dp, c.spacing))
    val rowHeight = (area.height - c.spacing * (items.size - 1)) / items.size
    val labelOn = c.config.showLabels && rowHeight >= 30.dp
    val resetOn = c.config.showReset && rowHeight >= 50.dp

    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { i, item ->
            if (i > 0) Spacer(GlanceModifier.height(c.spacing))
            if (style == WidgetStyle.FILL) {
                FillTile(c, item, DpSize(area.width, rowHeight), labelOn, resetOn)
                return@forEachIndexed
            }
            Row(modifier = GlanceModifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                val gauge = min(rowHeight, area.width * 0.42f)
                when (style) {
                    WidgetStyle.SHAPES -> Image(
                        ImageProvider(WidgetPainter.shape(c.px(gauge), item.fraction, c.config.gaugeShape, item.color, c.tones.track)),
                        null,
                        GlanceModifier.size(gauge),
                    )
                    WidgetStyle.GAUGE -> Gauge(c, item, gauge * 1.6f, withNumber = false)
                    WidgetStyle.NUMBERS -> Unit
                    else -> Ring(c, item, gauge, withNumber = false)
                }
                if (style != WidgetStyle.NUMBERS) Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    val sp = (rowHeight.value * (0.3f + 0.22f * c.config.numberSize)).coerceIn(14f, 44f)
                    NumberView(
                        c, percentText(item, c.config), sp, if (style == WidgetStyle.NUMBERS) item.color else c.tones.content,
                        maxWidth = if (style == WidgetStyle.NUMBERS) area.width else area.width - gauge - 12.dp,
                    )
                    Caption(c, item, labelOn, resetOn, center = false)
                }
            }
        }
    }
}

/** Bars, segments and dots: one row per limit with a header line above the gauge. */
@Composable
private fun Linear(c: Ctx, all: List<WidgetItem>, area: DpSize) {
    val style = c.config.style
    val wave = if (style.isWavy) WidgetPainter.Wave(c.config.waveAmplitude, c.config.waveLength) else WidgetPainter.Wave.NONE
    val time = c.config.showTimeProgress && style.supportsTimeProgress
    val numberSp = 12f + 10f * c.config.numberSize
    val headerHeight = max(c.line(numberSp).value, c.line(c.labelSp).value).dp
    val widthPx = c.px(area.width)
    val gaugePx = when (style) {
        WidgetStyle.DOTS -> WidgetPainter.dotSize(widthPx, c.config.count)
        WidgetStyle.SEGMENTS -> (WidgetPainter.barStroke(c.density, c.config.thickness) * 1.15f).roundToInt()
        else -> WidgetPainter.barHeight(c.density, c.config.thickness, wave, time)
    }
    val rowHeight = headerHeight + 3.dp + c.dp(gaugePx)
    val items = all.take(fitCount(area.height, rowHeight, c.spacing))
    val showHeader = area.height >= rowHeight

    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { item ->
            // Each row takes an equal share of the height, so a tall widget spreads its rows out.
            Column(modifier = GlanceModifier.defaultWeight().fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (showHeader) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (c.config.showLabels) {
                        Text(item.label, style = TextStyle(color = c.content, fontSize = c.labelSp.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                        Spacer(GlanceModifier.width(8.dp))
                    }
                    if (c.config.showReset && area.width >= 160.dp) {
                        Text(resetText(c.context, item, c.config.resetFormat), style = TextStyle(color = c.muted, fontSize = c.resetSp.sp), maxLines = 1)
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    NumberView(c, percentText(item, c.config), numberSp, c.tones.content)
                }
                Spacer(GlanceModifier.height(3.dp))
            }
            val bitmap = when (style) {
                WidgetStyle.DOTS -> WidgetPainter.dots(widthPx, item.fraction, item.color, c.tones.track, c.config.count, c.config.gaugeShape)
                WidgetStyle.SEGMENTS -> WidgetPainter.segments(
                    widthPx, c.density, item.fraction, item.color, c.tones.track, c.config.thickness, c.config.count,
                    item.timeFraction.takeIf { time }, c.timeColor,
                )
                else -> WidgetPainter.bar(
                    widthPx, c.density, item.fraction, item.color, c.tones.track, c.config.thickness, wave,
                    item.timeFraction.takeIf { time }, c.timeColor,
                )
            }
            Image(ImageProvider(bitmap), null, GlanceModifier.width(area.width).height(c.dp(bitmap.height)))
            }
        }
    }
}

/** All limits as nested rings; a legend beside them when there is room. */
@Composable
private fun Concentric(c: Ctx, all: List<WidgetItem>, area: DpSize) {
    val items = all.take(5)
    val legend = area.width - area.height >= 96.dp
    val size = if (legend) min(area.height, area.width * 0.5f) else min(area.width, area.height)
    val spec = WidgetPainter.RingSpec.of(c.config)
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = GlanceModifier.size(size), contentAlignment = Alignment.Center) {
            val bitmap = WidgetPainter.concentric(c.px(size), items.map { it.fraction }, items.map { it.color }, c.tones.track, spec)
            Image(ImageProvider(bitmap), null, GlanceModifier.size(size))
            val hole = c.dp(WidgetPainter.concentricHole(c.px(size), items.size, spec).roundToInt())
            if (hole >= 14.dp) {
                NumberView(c, percentText(items.first(), c.config), (hole.value * (0.5f + 0.3f * c.config.numberSize)).coerceIn(9f, 40f), c.tones.content)
            }
        }
        if (legend) {
            Spacer(GlanceModifier.width(c.spacing + 8.dp))
            Column(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Box(GlanceModifier.size(10.dp).background(ColorProvider(Color(item.color))).cornerRadius(5.dp)) {}
                        Spacer(GlanceModifier.width(8.dp))
                        Text(item.label, style = TextStyle(color = c.content, fontSize = c.labelSp.sp), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        NumberView(c, percentText(item, c.config), 12f + 6f * c.config.numberSize, c.tones.content)
                    }
                }
            }
        }
    }
}

/** One big main limit plus the others as chips. */
@Composable
private fun Combo(c: Ctx, all: List<WidgetItem>, area: DpSize) {
    val main = all.first()
    val others = all.drop(1).take(4)
    val side = others.isNotEmpty() && area.width - area.height >= 80.dp
    val labelOn = c.config.showLabels && area.height >= 90.dp
    val captionHeight = if (labelOn) c.line(c.labelSp) else 0.dp
    val ringSize = (if (side) min(area.height - captionHeight, area.width * 0.48f) else min(area.width, area.height - captionHeight)).coerceAtLeast(24.dp)
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Ring(c, main, ringSize, withNumber = true)
            if (labelOn) Text(main.label, style = TextStyle(color = c.content, fontSize = c.labelSp.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
        if (side) {
            Spacer(GlanceModifier.width(c.spacing + 6.dp))
            Column(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                others.forEachIndexed { i, item ->
                    if (i > 0) Spacer(GlanceModifier.height(6.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth()
                            .background(ColorProvider(Color(WidgetPainter.withAlpha(item.color, 0.2f))))
                            .cornerRadius(20.dp)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(item.label, style = TextStyle(color = c.content, fontSize = c.labelSp.sp, fontWeight = FontWeight.Medium), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        NumberView(c, percentText(item, c.config), 13f + 5f * c.config.numberSize, c.tones.content)
                    }
                }
            }
        }
    }
}

@Composable
private fun Ring(c: Ctx, item: WidgetItem, size: Dp, withNumber: Boolean) {
    val time = c.config.showTimeProgress && c.config.style.supportsTimeProgress
    Box(modifier = GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        val bitmap = WidgetPainter.ring(
            c.px(size), item.fraction, item.color, c.tones.track, WidgetPainter.RingSpec.of(c.config),
            item.timeFraction.takeIf { time }, WidgetPainter.withAlpha(c.tones.content, 0.55f).takeIf { time } ?: 0,
        )
        Image(ImageProvider(bitmap), null, GlanceModifier.size(size))
        if (withNumber) {
            val factor = (0.17f + 0.13f * c.config.numberSize) * (if (time) 0.82f else 1f)
            // Keep the number inside the ring, whatever the font width and weight.
            val inner = c.dp(WidgetPainter.ringInner(c.px(size), WidgetPainter.RingSpec.of(c.config)).roundToInt())
            NumberView(c, percentText(item, c.config), (size.value * factor).coerceIn(9f, 44f), c.tones.content, maxWidth = inner * (if (time) 0.62f else 0.8f))
        }
    }
}

@Composable
private fun Gauge(c: Ctx, item: WidgetItem, width: Dp, withNumber: Boolean = true) {
    val time = c.config.showTimeProgress
    val bitmap = WidgetPainter.gauge(
        c.px(width), item.fraction, item.color, c.tones.track, c.config.thickness,
        item.timeFraction.takeIf { time }, WidgetPainter.withAlpha(c.tones.content, 0.7f),
    )
    val height = c.dp(bitmap.height)
    Box(modifier = GlanceModifier.width(width).height(height), contentAlignment = Alignment.BottomCenter) {
        Image(ImageProvider(bitmap), null, GlanceModifier.width(width).height(height))
        if (withNumber) {
            NumberView(c, percentText(item, c.config), (width.value * (0.13f + 0.1f * c.config.numberSize)).coerceIn(9f, 44f), c.tones.content, maxWidth = width * 0.62f)
        }
    }
}

@Composable
private fun FillTile(c: Ctx, item: WidgetItem, size: DpSize, labelOn: Boolean, resetOn: Boolean) {
    val corner = (c.config.cornerDp * 0.7f).coerceAtLeast(8f)
    val bitmap = WidgetPainter.fillTile(c.px(size.width), c.px(size.height), corner * c.density, item.fraction, item.color, c.density)
    Box(modifier = GlanceModifier.width(size.width).height(size.height)) {
        Image(ImageProvider(bitmap), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Column(modifier = GlanceModifier.fillMaxSize().padding(10.dp)) {
            val sp = (minOf(size.width.value, size.height.value) * (0.2f + 0.14f * c.config.numberSize)).coerceIn(14f, 52f)
            NumberView(c, percentText(item, c.config), sp, c.tones.content, maxWidth = size.width - 20.dp)
            Spacer(GlanceModifier.defaultWeight())
            Caption(c, item, labelOn, resetOn, center = false)
        }
    }
}

@Composable
private fun Caption(c: Ctx, item: WidgetItem, labelOn: Boolean, resetOn: Boolean, center: Boolean = true) {
    val align = if (center) TextAlign.Center else TextAlign.Start
    if (labelOn) {
        Text(item.label, style = TextStyle(color = c.content, fontSize = c.labelSp.sp, fontWeight = FontWeight.Medium, textAlign = align), maxLines = 1)
    }
    if (resetOn && item.resetsAt != null) {
        Text(resetText(c.context, item, c.config.resetFormat), style = TextStyle(color = c.muted, fontSize = c.resetSp.sp, textAlign = align), maxLines = 1)
    }
}

/** A number in Roboto Flex (as a bitmap) or, if that is off or fails, in the system font. */
@Composable
private fun NumberView(c: Ctx, text: String, sizeSp: Float, color: Int, maxWidth: Dp? = null) {
    if (c.config.numberFont == NumberFont.FLEX) {
        val bitmap: Bitmap? = WidgetPainter.number(
            c.context, text, sizeSp * c.fontScale * c.density, color, c.config.numberWeight, c.config.numberWidth,
            maxWidth?.let { c.px(it).toFloat() } ?: Float.MAX_VALUE,
        )
        if (bitmap != null) {
            Image(ImageProvider(bitmap), text, GlanceModifier.width(c.dp(bitmap.width)).height(c.dp(bitmap.height)))
            return
        }
    }
    Text(text, style = TextStyle(color = ColorProvider(Color(color)), fontSize = sizeSp.sp, fontWeight = FontWeight.Bold), maxLines = 1)
}

@Composable
private fun Message(text: String, color: ColorProvider) {
    Text(
        text = text,
        style = TextStyle(color = color, fontSize = 13.sp, textAlign = TextAlign.Center),
        modifier = GlanceModifier.padding(8.dp),
    )
}

/**
 * For widgets of about one cell: a single limit, the gauge filling the space, the number on top
 * and no captions.
 */
@Composable
private fun Tiny(c: Ctx, item: WidgetItem, area: DpSize) {
    val style = c.config.style
    val size = min(area.width, area.height).coerceAtLeast(16.dp)
    val px = c.px(size)
    // Filled shapes sit under the number, so the number gets a small chip to stay readable.
    val filled = style == WidgetStyle.SHAPES || style == WidgetStyle.DOTS || style == WidgetStyle.FILL
    Box(modifier = GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        val bitmap = when {
            style == WidgetStyle.NUMBERS -> null
            filled -> WidgetPainter.shape(
                px, item.fraction, if (style == WidgetStyle.FILL) GaugeShape.CIRCLE else c.config.gaugeShape, item.color, c.tones.track,
            )
            else -> WidgetPainter.ring(
                px, item.fraction, item.color, c.tones.track,
                if (style == WidgetStyle.GAUGE || style.isLinear) {
                    WidgetPainter.RingSpec(c.config.thickness, total = 270f, canvasStart = 135f)
                } else {
                    WidgetPainter.RingSpec.of(c.config)
                },
            )
        }
        if (bitmap != null) Image(ImageProvider(bitmap), null, GlanceModifier.size(size))
        val text = percentText(item, c.config)
        when {
            style == WidgetStyle.NUMBERS ->
                NumberView(c, text, (size.value * (0.4f + 0.12f * c.config.numberSize)).coerceIn(8f, 64f), item.color, maxWidth = size)
            filled -> Box(
                modifier = GlanceModifier
                    .background(ColorProvider(Color(c.tones.containerSolid)))
                    .cornerRadius(50.dp)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                NumberView(c, text, (size.value * (0.16f + 0.06f * c.config.numberSize)).coerceIn(8f, 40f), c.tones.content)
            }
            else -> NumberView(c, text, (size.value * (0.22f + 0.08f * c.config.numberSize)).coerceIn(8f, 64f), c.tones.content)
        }
    }
}
