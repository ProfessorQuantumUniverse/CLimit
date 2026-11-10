@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.professorquantumuniverse.climit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.professorquantumuniverse.climit.R
import io.github.professorquantumuniverse.climit.data.LimitKind
import io.github.professorquantumuniverse.climit.data.UsageLimit
import io.github.professorquantumuniverse.climit.data.UsageSnapshot
import io.github.professorquantumuniverse.climit.widget.resetClock
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun WelcomeScreen(onSignIn: () -> Unit) {
    val fill = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) { fill.animateTo(0.66f, spec) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(168.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Gauge(
                    fraction = fill.value,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
                    modifier = Modifier.size(88.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayLargeEmphasized)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onSignIn, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(stringResource(R.string.action_sign_in), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun UsageScreen(
    snapshot: UsageSnapshot?,
    fetchedAt: Instant?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onAddWidget: (compact: Boolean) -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // The list scrolls under a translucent app bar that blurs what passes beneath it.
    val haze = rememberHazeState()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                subtitle = {
                    Text(fetchedAt?.let { stringResource(R.string.home_updated, resetClock(context, it)) } ?: stringResource(R.string.home_subtitle))
                },
                actions = {
                    if (loading && snapshot != null) LoadingIndicator(modifier = Modifier.size(40.dp))
                    IconButton(onClick = onRefresh, enabled = !loading) { Icon(Icons.Rounded.Refresh, stringResource(R.string.action_refresh)) }
                    IconButton(onClick = onSignOut, enabled = !loading) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.action_sign_out))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                scrollBehavior = scrollBehavior,
                modifier = Modifier.hazeBlur(input = HazeInput.Sources(haze), style = HazeMaterials.thin()),
            )
        },
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 720.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize().hazeSource(haze),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = insets.calculateTopPadding() + 8.dp,
                    bottom = insets.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (error != null) item { ErrorCard(error) }

                if (snapshot == null) {
                    if (loading) {
                        item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { ContainedLoadingIndicator() } }
                    }
                } else {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            snapshot.session?.let {
                                HeroCard(it, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f))
                            }
                            snapshot.weekly?.let {
                                HeroCard(it, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f))
                            }
                        }
                    }
                    item {
                        val others = snapshot.limits.filter { it.kind != LimitKind.SESSION && it.kind != LimitKind.WEEKLY_ALL }
                        val moreTitle = stringResource(R.string.home_more_limits)
                        val surfacesTitle = stringResource(R.string.surfaces_title)
                        val groups = buildList<@Composable (Modifier) -> Unit> {
                            if (others.isNotEmpty()) add { m -> SettingsGroup(moreTitle, others.map { limit -> { LimitRow(limit) } }, m) }
                            if (snapshot.surfaces.isNotEmpty()) {
                                add { m -> SettingsGroup(surfacesTitle, snapshot.surfaces.map { s -> { SurfaceRow(s.displayName, s.percent) } }, m) }
                            }
                        }
                        // On a tablet the two lists sit side by side.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            maxItemsInEachRow = if (wide) 2 else 1,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            groups.forEach { it(Modifier.weight(1f)) }
                        }
                    }
                }

                item { WidgetsCard(onAddWidget) }
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(20.dp))
    }
}

/** One of the two main limits: a tonal card with a wavy gauge that fills in when the screen opens. */
@Composable
private fun HeroCard(limit: UsageLimit, container: Color, content: Color, modifier: Modifier = Modifier) {
    val target = (limit.percent / 100.0).toFloat().coerceIn(0f, 1f)
    val fill = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(target) { fill.animateTo(target, spec) }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(32.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(116.dp)) {
                Gauge(fill.value, content, content.copy(alpha = 0.14f), Modifier.fillMaxSize())
                Text("${(fill.value * 100).roundToInt()}%", style = MaterialTheme.typography.headlineMediumEmphasized)
            }
            Column {
                Text(limitLabel(limit), style = MaterialTheme.typography.titleLargeEmphasized)
                limit.resetsAt?.let { Text(resetText(it), style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.76f)) }
            }
        }
    }
}

@Composable
private fun Gauge(fraction: Float, color: Color, track: Color, modifier: Modifier = Modifier) {
    val width = with(LocalDensity.current) { 10.dp.toPx() }
    val stroke = Stroke(width = width, cap = StrokeCap.Round)
    CircularWavyProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier,
        color = color,
        trackColor = track,
        stroke = stroke,
        trackStroke = stroke,
        wavelength = 36.dp,
    )
}

@Composable
private fun LimitRow(limit: UsageLimit) {
    val fill = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(limit.percent) { fill.animateTo((limit.percent / 100.0).toFloat().coerceIn(0f, 1f), spec) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(limitLabel(limit), style = MaterialTheme.typography.bodyLarge)
                limit.resetsAt?.let { Text(resetText(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text("${limit.percent.roundToInt()} %", style = MaterialTheme.typography.titleMediumEmphasized)
        }
        LinearWavyProgressIndicator(progress = { fill.value }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SurfaceRow(name: String, percent: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("${percent.roundToInt()} %", style = MaterialTheme.typography.titleMediumEmphasized)
    }
}

/** Adding widgets from the app, with a hint where their settings live. */
@Composable
private fun WidgetsCard(onAddWidget: (compact: Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(32.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIcon(Icons.Rounded.Widgets)
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.home_widgets_title), style = MaterialTheme.typography.titleLargeEmphasized)
            }
            Text(stringResource(R.string.home_widgets_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAddWidget(false) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.widget_rings_name))
                }
                FilledTonalButton(onClick = { onAddWidget(true) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.widget_compact_name))
                }
            }
        }
    }
}

@Composable
private fun limitLabel(limit: UsageLimit): String = when (limit.kind) {
    LimitKind.SESSION -> stringResource(R.string.limit_session)
    LimitKind.WEEKLY_ALL -> stringResource(R.string.limit_weekly)
    LimitKind.WEEKLY_SCOPED -> limit.modelName?.let { stringResource(R.string.limit_weekly_model, it) } ?: stringResource(R.string.limit_weekly)
    LimitKind.OTHER -> limit.rawKind
}

@Composable
private fun resetText(resetsAt: Instant): String {
    val remaining = Duration.between(Instant.now(), resetsAt)
    if (remaining.isNegative || remaining.isZero) return stringResource(R.string.reset_due)
    val days = remaining.toDays()
    val hours = remaining.toHours() % 24
    val minutes = remaining.toMinutes() % 60
    return when {
        days > 0 -> stringResource(R.string.reset_in_days, days, hours)
        hours > 0 -> stringResource(R.string.reset_in_hours, hours, minutes)
        else -> stringResource(R.string.reset_in_minutes, minutes.coerceAtLeast(1))
    }
}
