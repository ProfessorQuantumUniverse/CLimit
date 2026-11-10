package io.github.professorquantumuniverse.climit.data

import java.time.Instant

/** One fetch of the usage endpoint, reduced to what the widgets need. */
data class UsageSnapshot(
    val limits: List<UsageLimit>,
    val surfaces: List<SurfaceShare>,
    /** Start of the current weekly window, if the server reported it. */
    val weekStartedAt: Instant?,
) {
    val session: UsageLimit? get() = limits.firstOrNull { it.kind == LimitKind.SESSION }
    val weekly: UsageLimit? get() = limits.firstOrNull { it.kind == LimitKind.WEEKLY_ALL }
    val weeklyScoped: List<UsageLimit> get() = limits.filter { it.kind == LimitKind.WEEKLY_SCOPED }
}

enum class LimitKind { SESSION, WEEKLY_ALL, WEEKLY_SCOPED, OTHER }

enum class Severity { NORMAL, WARNING, CRITICAL, UNKNOWN }

data class UsageLimit(
    val kind: LimitKind,
    /** The server's own kind string, kept so unknown kinds can still be labelled. */
    val rawKind: String,
    /** 0..100, but the server may exceed 100, so it is not clamped here. */
    val percent: Double,
    val resetsAt: Instant?,
    /** Model a scoped limit applies to, e.g. "Fable"; null for unscoped limits. */
    val modelName: String?,
    val severity: Severity,
)

/** Share of the weekly usage that came from one surface (Claude Code, Chats, Cowork ...). */
data class SurfaceShare(
    val key: String,
    val displayName: String,
    val percent: Double,
)
