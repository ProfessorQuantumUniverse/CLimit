package io.github.professorquantumuniverse.climit.widget

import io.github.professorquantumuniverse.climit.data.LimitKind
import io.github.professorquantumuniverse.climit.data.Severity
import io.github.professorquantumuniverse.climit.data.UsageLimit
import io.github.professorquantumuniverse.climit.data.UsageSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class WidgetConfigTest {

    @Test
    fun `round trips through JSON`() {
        val config = WidgetConfig(
            style = WidgetStyle.WAVY_BARS,
            order = listOf(LimitSlot.MODELS, LimitSlot.SESSION, LimitSlot.WEEKLY),
            shown = setOf(LimitSlot.MODELS),
            labelSession = "Jetzt",
            source = ColorSource.CUSTOM,
            customHue = 123f,
            cardShape = CardShape.CLOVER4,
            tapAction = TapAction.OPEN_CLAUDE,
        )
        assertEquals(config, WidgetConfig.fromJson(config.toJson()))
    }

    @Test
    fun `migrates version 1 settings`() {
        val v1 = """
            {"style":"SHAPES","showSession":false,"showWeekly":true,"showModels":true,
             "shape":"CLOVER","source":"GREEN","containerOpacity":0.5,"opacity":0.3,"showRim":true}
        """.trimIndent()
        val config = WidgetConfig.fromJson(v1)
        assertEquals(WidgetStyle.SHAPES, config.style)
        assertEquals(setOf(LimitSlot.WEEKLY, LimitSlot.MODELS), config.shown)
        assertEquals(GaugeShape.CLOVER, config.gaugeShape)
        assertEquals(ColorSource.GREEN, config.source)
        assertEquals(0.5f, config.containerOpacity, 0f)
        assertEquals(WidgetConfig.CURRENT_VERSION, config.version)
    }

    @Test
    fun `unreadable or unknown input falls back to the defaults`() {
        val fallback = WidgetConfig(style = WidgetStyle.DOTS)
        assertSame(fallback, WidgetConfig.fromJson("not json", fallback))
        assertSame(fallback, WidgetConfig.fromJson(null, fallback))
        // Unknown enum values and fields are tolerated.
        val config = WidgetConfig.fromJson("""{"version":2,"style":"HOLOGRAM","future":1,"count":12}""")
        assertEquals(WidgetStyle.RINGS, config.style)
        assertEquals(12, config.count)
    }

    @Test
    fun `sanitizing clamps hand-edited values`() {
        val config = WidgetConfig(
            count = 500,
            cornerDp = -4f,
            warnAt = 99f,
            criticalAt = 10f,
            order = listOf(LimitSlot.WEEKLY, LimitSlot.WEEKLY),
            labelSession = "x".repeat(100),
        ).sanitized()
        assertEquals(MAX_COUNT, config.count)
        assertEquals(0f, config.cornerDp, 0f)
        assertTrue(config.criticalAt > config.warnAt)
        assertEquals(listOf(LimitSlot.WEEKLY, LimitSlot.SESSION, LimitSlot.MODELS), config.order)
        assertEquals(MAX_LABEL, config.labelSession.length)
    }

    @Test
    fun `presets keep content and behaviour`() {
        val mine = WidgetConfig(shown = setOf(LimitSlot.MODELS), labelWeekly = "W", tapAction = TapAction.NONE)
        val applied = mine.withLookOf(Preset.CONTRAST.config)
        assertEquals(WidgetStyle.FILL, applied.style)
        assertEquals(ContainerRole.INVERSE, applied.container)
        assertEquals(setOf(LimitSlot.MODELS), applied.shown)
        assertEquals("W", applied.labelWeekly)
        assertEquals(TapAction.NONE, applied.tapAction)
    }

    @Test
    fun `import accepts only CLimit settings`() {
        assertNull(parseImported(null))
        assertNull(parseImported("hello"))
        assertNull(parseImported("""{"name":"something else"}"""))
        assertNull(parseImported("""{"style": broken"""))
        assertEquals(WidgetStyle.GAUGE, parseImported(WidgetConfig(style = WidgetStyle.GAUGE).toJson())?.style)
    }

    private val now: Instant = Instant.parse("2026-11-10T20:00:00Z")

    private fun limit(kind: LimitKind, percent: Double, resetsIn: Duration?, model: String? = null) =
        UsageLimit(kind, kind.name.lowercase(), percent, resetsIn?.let { now + it }, model, Severity.NORMAL)

    private val snapshot = UsageSnapshot(
        limits = listOf(
            limit(LimitKind.SESSION, 80.0, Duration.ofHours(1)),
            limit(LimitKind.WEEKLY_ALL, 50.0, Duration.ofDays(2)),
            limit(LimitKind.WEEKLY_SCOPED, 95.0, Duration.ofDays(2), "Opus"),
        ),
        surfaces = emptyList(),
        weekStartedAt = null,
    )

    private fun items(config: WidgetConfig) =
        buildItems(config, snapshot, { index, _ -> index }, now) { it.name.lowercase() }

    @Test
    fun `items follow order, visibility and custom labels`() {
        val config = WidgetConfig(
            order = listOf(LimitSlot.MODELS, LimitSlot.WEEKLY, LimitSlot.SESSION),
            shown = setOf(LimitSlot.MODELS, LimitSlot.SESSION),
            labelSession = "Jetzt",
        )
        val result = items(config)
        assertEquals(listOf("Opus", "Jetzt"), result.map { it.label })
        assertEquals(listOf(0, 1), result.map { it.color })
    }

    @Test
    fun `remaining mode flips the number but not the colour input`() {
        var seen = -1.0
        val result = buildItems(WidgetConfig(showRemaining = true), snapshot, { _, used -> seen = used; 0 }, now) { "" }
        assertEquals(20.0, result.first().shownPercent, 0.0)
        assertEquals(0.2f, result.first().fraction, 0.0001f)
        assertEquals(50.0, seen, 0.0)
    }

    @Test
    fun `time fraction measures the elapsed window`() {
        val session = items(WidgetConfig()).first()
        // One hour left of five: four fifths have passed.
        assertEquals(0.8f, session.timeFraction!!, 0.001f)
        assertNull(timeFraction(now, null, SESSION_WINDOW))
        assertEquals(1f, timeFraction(now, now - Duration.ofMinutes(5), SESSION_WINDOW)!!, 0f)
    }

    @Test
    fun `level colours switch at the thresholds`() {
        val tones = WidgetTones(
            container = 0, containerSolid = 0, content = 0, muted = 0, track = 0, outline = 0, warning = 0,
            accents = intArrayOf(1, 2, 3), error = 9, mode = IndicatorColors.LEVEL, warnAt = 70f, criticalAt = 90f,
        )
        assertEquals(1, tones.accent(0, 69.9))
        assertEquals(2, tones.accent(0, 70.0))
        assertEquals(9, tones.accent(0, 90.0))
    }

    @Test
    fun `countdown splits the remaining time`() {
        assertEquals(Countdown(1, 2, 3), countdown(now, now + Duration.ofDays(1).plusHours(2).plusMinutes(3)))
        assertNull(countdown(now, now))
    }
}
