package io.github.professorquantumuniverse.climit.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Parses the undocumented `/api/organizations/{uuid}/usage` response.
 *
 * The endpoint is not a stable API: fields come and go, many are null, and types may change.
 * So this reads the JSON tree by hand and treats every field as optional. Only a response that
 * is not a JSON object at all is an error; anything else yields a (possibly empty) snapshot.
 */
object UsageParser {

    class MalformedResponse(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun parse(body: String): UsageSnapshot {
        val root = try {
            Json.parseToJsonElement(body) as? JsonObject
        } catch (e: Exception) {
            throw MalformedResponse("Usage response is not valid JSON", e)
        } ?: throw MalformedResponse("Usage response is not a JSON object")

        val limits = root.array("limits").mapNotNull { parseLimit(it) }
            .ifEmpty { legacyLimits(root) }

        val breakdown = root.obj("seven_day_breakdown")
        val surfaces = breakdown?.array("rows").orEmpty().mapNotNull { parseSurface(it) }

        return UsageSnapshot(
            limits = limits,
            surfaces = surfaces,
            weekStartedAt = breakdown?.instant("window_started_at"),
        )
    }

    /** One entry of the generic `limits` list. */
    private fun parseLimit(element: JsonElement): UsageLimit? {
        val o = element as? JsonObject ?: return null
        val percent = o.double("percent") ?: return null
        val rawKind = o.string("kind") ?: o.string("group") ?: return null
        return UsageLimit(
            kind = when (rawKind) {
                "session" -> LimitKind.SESSION
                "weekly_all" -> LimitKind.WEEKLY_ALL
                "weekly_scoped" -> LimitKind.WEEKLY_SCOPED
                else -> LimitKind.OTHER
            },
            rawKind = rawKind,
            percent = percent,
            resetsAt = o.instant("resets_at"),
            modelName = o.obj("scope")?.obj("model")?.string("display_name"),
            severity = severity(o.string("severity")),
        )
    }

    /** Fallback for responses without `limits`: the older fixed top-level windows. */
    private fun legacyLimits(root: JsonObject): List<UsageLimit> {
        fun window(key: String, kind: LimitKind, rawKind: String, model: String? = null): UsageLimit? {
            val o = root.obj(key) ?: return null
            val percent = o.double("utilization") ?: return null
            return UsageLimit(kind, rawKind, percent, o.instant("resets_at"), model, Severity.UNKNOWN)
        }
        return listOfNotNull(
            window("five_hour", LimitKind.SESSION, "session"),
            window("seven_day", LimitKind.WEEKLY_ALL, "weekly_all"),
            window("seven_day_opus", LimitKind.WEEKLY_SCOPED, "weekly_scoped", "Opus"),
            window("seven_day_sonnet", LimitKind.WEEKLY_SCOPED, "weekly_scoped", "Sonnet"),
        )
    }

    private fun parseSurface(element: JsonElement): SurfaceShare? {
        val o = element as? JsonObject ?: return null
        val key = o.string("key") ?: return null
        val percent = o.double("percent") ?: return null
        return SurfaceShare(key, o.string("display_name") ?: key, percent)
    }

    private fun severity(raw: String?): Severity = when (raw) {
        "normal" -> Severity.NORMAL
        "warning" -> Severity.WARNING
        "critical", "danger", "error" -> Severity.CRITICAL
        else -> Severity.UNKNOWN
    }

    // Lenient accessors: a missing key, JSON null or wrong type all read as "absent".

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()

    private fun JsonObject.primitive(key: String): JsonPrimitive? =
        (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }

    private fun JsonObject.string(key: String): String? = primitive(key)?.takeIf { it.isString }?.content

    private fun JsonObject.double(key: String): Double? = primitive(key)?.doubleOrNull

    private fun JsonObject.instant(key: String): Instant? {
        val text = string(key) ?: return null
        return try {
            OffsetDateTime.parse(text).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
