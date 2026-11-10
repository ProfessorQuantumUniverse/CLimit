package io.github.professorquantumuniverse.climit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class UsageParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)) { "Missing fixture $name" }.readText()

    @Test
    fun `parses a real Pro response`() {
        val snapshot = UsageParser.parse(fixture("usage_pro.json"))

        assertEquals(3, snapshot.limits.size)

        val session = checkNotNull(snapshot.session)
        assertEquals(10.0, session.percent, 0.0)
        assertEquals(Instant.parse("2026-11-10T22:00:00.511263Z"), session.resetsAt)
        assertEquals(Severity.NORMAL, session.severity)
        assertNull(session.modelName)

        val weekly = checkNotNull(snapshot.weekly)
        assertEquals(55.0, weekly.percent, 0.0)
        assertEquals(Instant.parse("2026-11-12T08:00:00.511282Z"), weekly.resetsAt)

        val scoped = snapshot.weeklyScoped.single()
        assertEquals("Fable", scoped.modelName)
        assertEquals(3.0, scoped.percent, 0.0)

        assertEquals(listOf("claude_code", "chat", "cowork", "other"), snapshot.surfaces.map { it.key })
        assertEquals(84.0, snapshot.surfaces.first { it.key == "cowork" }.percent, 0.0)
        assertEquals(Instant.parse("2026-11-05T08:00:00.511282Z"), snapshot.weekStartedAt)
    }

    @Test
    fun `falls back to the fixed windows when limits is missing`() {
        val snapshot = UsageParser.parse(
            """
            {
              "five_hour": {"utilization": 42.5, "resets_at": "2026-01-01T05:00:00Z"},
              "seven_day": {"utilization": 7, "resets_at": null},
              "seven_day_opus": {"utilization": 1, "resets_at": "2026-01-03T00:00:00+01:00"},
              "seven_day_sonnet": null
            }
            """.trimIndent()
        )

        assertEquals(42.5, checkNotNull(snapshot.session).percent, 0.0)
        assertNull(checkNotNull(snapshot.weekly).resetsAt)
        assertEquals("Opus", snapshot.weeklyScoped.single().modelName)
        assertEquals(Instant.parse("2026-01-02T23:00:00Z"), snapshot.weeklyScoped.single().resetsAt)
        assertTrue(snapshot.surfaces.isEmpty())
    }

    @Test
    fun `skips broken entries and keeps unknown kinds`() {
        val snapshot = UsageParser.parse(
            """
            {
              "limits": [
                {"kind": "session", "percent": "not a number"},
                "garbage",
                {"kind": "monthly_all", "percent": 120, "resets_at": "yesterday", "severity": 5},
                {"kind": "session", "percent": 12, "scope": "odd"}
              ],
              "seven_day_breakdown": {"rows": [{"key": "chat"}, {"key": "x", "percent": 2}]}
            }
            """.trimIndent()
        )

        assertEquals(2, snapshot.limits.size)
        val other = snapshot.limits.first()
        assertEquals(LimitKind.OTHER, other.kind)
        assertEquals("monthly_all", other.rawKind)
        assertEquals(120.0, other.percent, 0.0)
        assertNull(other.resetsAt)
        assertEquals(Severity.UNKNOWN, other.severity)
        assertEquals(12.0, checkNotNull(snapshot.session).percent, 0.0)
        assertEquals("x", snapshot.surfaces.single().displayName)
    }

    @Test
    fun `an empty object gives an empty snapshot`() {
        val snapshot = UsageParser.parse("{}")
        assertTrue(snapshot.limits.isEmpty())
        assertNull(snapshot.session)
        assertNull(snapshot.weekStartedAt)
    }

    @Test
    fun `rejects bodies that are not a JSON object`() {
        assertThrows(UsageParser.MalformedResponse::class.java) { UsageParser.parse("<html>blocked</html>") }
        assertThrows(UsageParser.MalformedResponse::class.java) { UsageParser.parse("[]") }
    }
}
