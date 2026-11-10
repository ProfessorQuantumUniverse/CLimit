@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)

package io.github.professorquantumuniverse.climit.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BorderOuter
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LineWeight
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.graphics.shapes.RoundedPolygon
import io.github.professorquantumuniverse.climit.R
import io.github.professorquantumuniverse.climit.data.SessionRepository
import io.github.professorquantumuniverse.climit.ui.ChoiceRow
import io.github.professorquantumuniverse.climit.ui.ClimitTheme
import io.github.professorquantumuniverse.climit.ui.ConnectedChoice
import io.github.professorquantumuniverse.climit.ui.RowIcon
import io.github.professorquantumuniverse.climit.ui.RowText
import io.github.professorquantumuniverse.climit.ui.SettingsGroup
import io.github.professorquantumuniverse.climit.ui.SliderRow
import io.github.professorquantumuniverse.climit.ui.SwitchRow
import io.github.professorquantumuniverse.climit.ui.flexFamily
import io.github.professorquantumuniverse.climit.ui.hueBrush
import io.github.professorquantumuniverse.climit.ui.hueColor
import io.github.professorquantumuniverse.climit.ui.rememberHaptics
import io.github.professorquantumuniverse.climit.ui.rememberMorphShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Settings for one widget instance, opened from the launcher's "edit widget" action. */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // Leaving without saving must not discard a widget that is being added.
        setResult(RESULT_CANCELED, result)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val glanceId = try {
            GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        } catch (_: IllegalArgumentException) {
            finish()
            return
        }
        val defaults = UsageWidget.defaultsFor(this, appWidgetId)

        setContent {
            ClimitTheme {
                val context = LocalContext.current
                var loaded by remember { mutableStateOf<WidgetConfig?>(null) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    if (SessionRepository.state.value == null) SessionRepository(context).load()
                    loaded = WidgetConfig.from(getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId), defaults)
                }
                loaded?.let { initial ->
                    ConfigScreen(
                        appWidgetId = appWidgetId,
                        glanceId = glanceId,
                        initial = initial,
                        defaults = defaults,
                        onCancel = { finish() },
                        onSave = { config ->
                            scope.launch {
                                updateAppWidgetState(context, glanceId) { it[WidgetConfig.KEY] = config.toJson() }
                                UsageWidget().update(context, glanceId)
                                setResult(RESULT_OK, result)
                                finish()
                            }
                        },
                    )
                }
            }
        }
    }
}

private val ConfigSaver = Saver<WidgetConfig, String>(save = { it.toJson() }, restore = { WidgetConfig.fromJson(it) })

private enum class ConfigTab(val title: Int, val icon: ImageVector) {
    STYLE(R.string.tab_style, Icons.Rounded.Style),
    CONTENT(R.string.tab_content, Icons.Rounded.TextFields),
    COLOR(R.string.tab_color, Icons.Rounded.Palette),
    CARD(R.string.tab_card, Icons.Rounded.DashboardCustomize),
    BEHAVIOR(R.string.tab_behavior, Icons.Rounded.TouchApp),
}

private class ConfigActions(
    val export: () -> Unit,
    val import: () -> Unit,
    val reset: () -> Unit,
    val copyFrom: () -> Unit,
    val savePreset: () -> Unit,
)

@Composable
private fun ConfigScreen(
    appWidgetId: Int,
    glanceId: GlanceId,
    initial: WidgetConfig,
    defaults: WidgetConfig,
    onCancel: () -> Unit,
    onSave: (WidgetConfig) -> Unit,
) {
    val context = LocalContext.current
    var config by rememberSaveable(stateSaver = ConfigSaver) { mutableStateOf(initial) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun toast(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    var showCopyDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<SavedPreset?>(null) }
    val presetStore = remember { PresetStore(context) }
    var savedPresets by remember { mutableStateOf(presetStore.list()) }
    val widgetSize = remember(appWidgetId) { widgetSize(context, appWidgetId, defaults) }

    val exportedText = stringResource(R.string.exported)
    val importedText = stringResource(R.string.imported)
    val importFailedText = stringResource(R.string.import_failed)
    val presetSavedText = stringResource(R.string.preset_saved)
    val copiedText = stringResource(R.string.copied_from)

    val haptics = rememberHaptics()
    val actions = ConfigActions(
        export = {
            haptics.confirm()
            copyToClipboard(context, config.toJson())
            toast(exportedText)
        },
        import = {
            val parsed = parseImported(readClipboard(context))
            if (parsed != null) {
                haptics.confirm()
                config = parsed
                toast(importedText)
            } else {
                haptics.reject()
                toast(importFailedText)
            }
        },
        reset = {
            haptics.longPress()
            config = defaults
        },
        copyFrom = {
            haptics.click()
            showCopyDialog = true
        },
        savePreset = {
            haptics.click()
            showSaveDialog = true
        },
    )
    val set: (WidgetConfig) -> Unit = { config = it.sanitized() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.config_title), style = MaterialTheme.typography.titleLargeEmphasized) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        BoxWithConstraints(modifier = Modifier.padding(insets).fillMaxSize()) {
            val wide = maxWidth >= 720.dp

            val settings: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier = modifier) {
                    PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp, containerColor = MaterialTheme.colorScheme.surface) {
                        ConfigTab.entries.forEachIndexed { index, t ->
                            Tab(
                                selected = tab == index,
                                onClick = {
                                    if (tab != index) haptics.tick()
                                    tab = index
                                },
                                text = { Text(stringResource(t.title)) },
                                icon = { Icon(t.icon, null) },
                            )
                        }
                    }
                    val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                    val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                    AnimatedContent(
                        targetState = ConfigTab.entries[tab],
                        transitionSpec = { fadeIn(enter) togetherWith fadeOut(exit) },
                        label = "tab",
                        modifier = Modifier.weight(1f),
                    ) { current ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                                .padding(top = 4.dp, bottom = 112.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            when (current) {
                                ConfigTab.STYLE -> StyleTab(
                                    config, set, savedPresets,
                                    onSavePreset = actions.savePreset,
                                    onDeletePreset = {
                                        haptics.longPress()
                                        presetToDelete = it
                                    },
                                )
                                ConfigTab.CONTENT -> ContentTab(config, set)
                                ConfigTab.COLOR -> ColorTab(config, set)
                                ConfigTab.CARD -> CardTab(config, set)
                                ConfigTab.BEHAVIOR -> BehaviorTab(config, set, actions)
                            }
                        }
                    }
                }
            }

            if (wide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    PreviewPane(config, widgetSize, Modifier.weight(0.45f).fillMaxHeight().padding(16.dp), fill = true)
                    settings(Modifier.weight(0.55f).fillMaxHeight())
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    PreviewPane(config, widgetSize, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), fill = false)
                    settings(Modifier.weight(1f))
                }
            }

            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = {
                        haptics.confirm()
                        onSave(config)
                    }) {
                        Icon(Icons.Rounded.Check, stringResource(R.string.action_save))
                    }
                },
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                modifier = Modifier
                    .align(if (wide) Alignment.BottomEnd else Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .offset(x = if (wide) (-24).dp else 0.dp, y = (-16).dp),
            ) {
                IconButton(onClick = actions.reset) { Icon(Icons.Rounded.RestartAlt, stringResource(R.string.action_reset)) }
                IconButton(onClick = actions.export) { Icon(Icons.Rounded.ContentCopy, stringResource(R.string.action_export)) }
                IconButton(onClick = actions.import) { Icon(Icons.Rounded.ContentPaste, stringResource(R.string.action_import)) }
                IconButton(onClick = actions.copyFrom) { Icon(Icons.Rounded.Widgets, stringResource(R.string.action_copy_from)) }
            }
        }
    }

    if (showCopyDialog) {
        CopyFromDialog(
            currentId = glanceId,
            onDismiss = { showCopyDialog = false },
            onPick = {
                haptics.confirm()
                config = it
                showCopyDialog = false
                toast(copiedText)
            },
        )
    }
    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                haptics.confirm()
                presetStore.save(name, config)
                savedPresets = presetStore.list()
                showSaveDialog = false
                toast(presetSavedText)
            },
        )
    }
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text(stringResource(R.string.preset_delete_title)) },
            text = { Text(preset.name) },
            confirmButton = {
                TextButton(onClick = {
                    presetStore.delete(preset.name)
                    savedPresets = presetStore.list()
                    presetToDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { presetToDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** The widget's real size on the home screen (portrait), so the preview matches it. */
private fun widgetSize(context: Context, appWidgetId: Int, defaults: WidgetConfig): DpSize {
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return when {
        width > 0 && height > 0 -> DpSize(width.dp, height.dp)
        defaults == WidgetConfig.COMPACT -> DpSize(150.dp, 150.dp)
        else -> DpSize(320.dp, 160.dp)
    }
}

// ---------------------------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------------------------

@Composable
private fun PreviewPane(config: WidgetConfig, widgetSize: DpSize, modifier: Modifier, fill: Boolean) {
    val context = LocalContext.current
    val store = remember { PreviewBackgroundStore(context) }
    var background by remember { mutableStateOf(store.background) }
    var custom by remember { mutableStateOf(if (background == PreviewBackground.CUSTOM) store.customImage()?.asImageBitmap() else null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            store.importImage(uri)?.let {
                custom = it.asImageBitmap()
                background = PreviewBackground.CUSTOM
            }
        }
    }

    Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier.height(232.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PreviewBackdrop(background, custom)
            // Fit the real widget size into the pane, leaving room for the background picker.
            val scale = minOf(1f, (maxWidth - 40.dp) / widgetSize.width, (maxHeight - 72.dp) / widgetSize.height)
            Box(
                modifier = Modifier.offset(y = (-20).dp).size(widgetSize.width * scale, widgetSize.height * scale),
                contentAlignment = Alignment.Center,
            ) {
                LivePreview(config, widgetSize, Modifier.requiredSize(widgetSize).scale(scale))
            }
            BackgroundPicker(
                selected = background,
                onSelect = {
                    background = it
                    store.background = it
                },
                onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun PreviewBackdrop(background: PreviewBackground, custom: ImageBitmap?) {
    val scheme = MaterialTheme.colorScheme
    if (background == PreviewBackground.CUSTOM && custom != null) {
        Image(custom, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.fillMaxSize().background(backdropBrush(background, scheme.primaryContainer, scheme.tertiaryContainer, scheme.secondaryContainer)))
    }
}

private fun backdropBrush(background: PreviewBackground, a: Color, b: Color, c: Color): Brush = when (background) {
    PreviewBackground.LIGHT -> Brush.linearGradient(listOf(Color(0xFFE9EEFB), Color(0xFFF8E7EE), Color(0xFFFFF4DE)))
    PreviewBackground.DARK -> Brush.linearGradient(listOf(Color(0xFF0D1220), Color(0xFF232A48), Color(0xFF101418)))
    PreviewBackground.SUNSET -> Brush.linearGradient(listOf(Color(0xFF3B1E6E), Color(0xFFD9467A), Color(0xFFFFA65C)))
    else -> Brush.linearGradient(listOf(a, b, c))
}

@Composable
private fun BackgroundPicker(selected: PreviewBackground, onSelect: (PreviewBackground) -> Unit, onPick: () -> Unit, modifier: Modifier) {
    val haptics = rememberHaptics()
    val scheme = MaterialTheme.colorScheme
    Surface(shape = CircleShape, color = scheme.surfaceContainerHigh.copy(alpha = 0.92f), modifier = modifier) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(PreviewBackground.DYNAMIC, PreviewBackground.LIGHT, PreviewBackground.DARK, PreviewBackground.SUNSET).forEach { bg ->
                val isSelected = bg == selected
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(backdropBrush(bg, scheme.primaryContainer, scheme.tertiaryContainer, scheme.secondaryContainer))
                        .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) scheme.primary else scheme.outlineVariant, CircleShape)
                        .clickable(onClickLabel = stringResource(R.string.preview_background)) {
                            haptics.tick()
                            onSelect(bg)
                        },
                )
            }
            IconButton(onClick = onPick, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Rounded.Wallpaper,
                    stringResource(R.string.bg_pick),
                    tint = if (selected == PreviewBackground.CUSTOM) scheme.primary else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The real widget, composed with the settings being edited and hosted here, so preview and home
 * screen cannot drift apart. Renders are conflated: while one runs, further changes collapse into
 * the latest, so the preview follows a slider live without queueing up stale frames.
 */
@Composable
private fun LivePreview(config: WidgetConfig, size: DpSize, modifier: Modifier) {
    val context = LocalContext.current
    var views by remember { mutableStateOf<RemoteViews?>(null) }
    val requests = remember { MutableStateFlow<Pair<WidgetConfig, DpSize>?>(null) }
    LaunchedEffect(config, size) { requests.value = config to size }
    LaunchedEffect(Unit) {
        requests.filterNotNull().conflate().collect { (c, s) ->
            runCatching {
                UsageWidget().compose(context = context, size = s, state = mutablePreferencesOf(WidgetConfig.KEY to c.toJson()))
            }.getOrNull()?.let { views = it }
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { FrameLayout(it) },
        update = { host ->
            val current = views ?: return@AndroidView
            host.removeAllViews()
            runCatching { host.addView(current.apply(context.applicationContext, host)) }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Tabs
// ---------------------------------------------------------------------------------------------

@Composable
private fun StyleTab(
    config: WidgetConfig,
    set: (WidgetConfig) -> Unit,
    saved: List<SavedPreset>,
    onSavePreset: () -> Unit,
    onDeletePreset: (SavedPreset) -> Unit,
) {
    val style = config.style
    SettingsGroup(
        title = stringResource(R.string.config_presets),
        rows = listOf {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Preset.entries.forEach { preset ->
                    PresetTile(stringResource(presetName(preset)), preset.config, onClick = { set(config.withLookOf(preset.config)) })
                }
                saved.forEach { preset ->
                    PresetTile(preset.name, preset.config, onClick = { set(config.withLookOf(preset.config)) }, onLongClick = { onDeletePreset(preset) })
                }
                AddPresetTile(onSavePreset)
            }
        },
    )

    SettingsGroup(
        title = stringResource(R.string.config_style),
        rows = listOf {
            FlowRow(
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                WidgetStyle.entries.forEach { s ->
                    StyleTile(s, selected = s == style, modifier = Modifier.weight(1f)) { set(config.copy(style = s)) }
                }
            }
        },
    )

    val rows = buildList<@Composable () -> Unit> {
        if (style.usesGaugeShape) add { GaugeShapePicker(config.gaugeShape) { set(config.copy(gaugeShape = it)) } }
        if (style.hasStroke) {
            add { SliderRow(Icons.Rounded.LineWeight, stringResource(R.string.config_thickness), config.thickness, { set(config.copy(thickness = it)) }) }
        }
        if (style.isWavy) {
            add { SliderRow(Icons.Rounded.Waves, stringResource(R.string.config_wave_height), config.waveAmplitude, { set(config.copy(waveAmplitude = it)) }) }
            add { SliderRow(null, stringResource(R.string.config_wave_length), config.waveLength, { set(config.copy(waveLength = it)) }) }
        }
        if (style.usesCount) {
            add {
                SliderRow(
                    Icons.Rounded.Numbers, stringResource(R.string.config_count), config.count.toFloat(),
                    { set(config.copy(count = it.roundToInt())) },
                    range = MIN_COUNT.toFloat()..MAX_COUNT.toFloat(), steps = MAX_COUNT - MIN_COUNT - 1, valueText = config.count.toString(),
                )
            }
        }
        if (style.isRing) {
            add {
                SwitchRow(
                    null, stringResource(R.string.config_ring_open), config.ringOpen,
                    { set(config.copy(ringOpen = it)) }, stringResource(R.string.config_ring_open_sub),
                )
            }
            if (!config.ringOpen) {
                add {
                    SliderRow(
                        Icons.Rounded.RotateRight, stringResource(R.string.config_ring_start), config.ringStart,
                        { set(config.copy(ringStart = it)) }, range = 0f..315f, steps = 6, valueText = "${config.ringStart.roundToInt()}°",
                    )
                }
            }
            add { SwitchRow(null, stringResource(R.string.config_clockwise), config.clockwise, { set(config.copy(clockwise = it)) }) }
        }
        if (style.supportsTimeProgress) {
            add {
                SwitchRow(
                    Icons.Rounded.Timelapse, stringResource(R.string.config_time), config.showTimeProgress,
                    { set(config.copy(showTimeProgress = it)) }, stringResource(R.string.config_time_sub),
                )
            }
        }
    }
    SettingsGroup(title = stringResource(R.string.config_gauge), rows = rows)
}

@Composable
private fun ContentTab(config: WidgetConfig, set: (WidgetConfig) -> Unit) {
    SettingsGroup(
        title = stringResource(R.string.config_limits),
        rows = config.order.mapIndexed { index, slot ->
            {
                LimitOrderRow(
                    slot = slot,
                    shown = slot in config.shown,
                    canMoveUp = index > 0,
                    canMoveDown = index < config.order.lastIndex,
                    onToggle = { on -> set(config.copy(shown = if (on) config.shown + slot else config.shown - slot)) },
                    onMove = { delta ->
                        val list = config.order.toMutableList()
                        val target = (index + delta).coerceIn(0, list.lastIndex)
                        list.add(target, list.removeAt(index))
                        set(config.copy(order = list))
                    },
                )
            }
        },
    )

    if (config.style.supportsArrangement) {
        SettingsGroup(
            title = null,
            rows = listOf {
                ChoiceRow(
                    Icons.Rounded.ViewColumn, stringResource(R.string.config_arrangement),
                    listOf(
                        LimitArrangement.AUTO to stringResource(R.string.arrangement_auto),
                        LimitArrangement.ROW to stringResource(R.string.arrangement_row),
                        LimitArrangement.COLUMN to stringResource(R.string.arrangement_column),
                    ),
                    config.arrangement, { set(config.copy(arrangement = it)) },
                )
            },
        )
    }

    SettingsGroup(
        title = stringResource(R.string.config_texts),
        rows = buildList<@Composable () -> Unit> {
            add { SwitchRow(Icons.Rounded.TextFields, stringResource(R.string.config_show_labels), config.showLabels, { set(config.copy(showLabels = it)) }) }
            if (config.showLabels) {
                add {
                    LabelField(stringResource(R.string.config_label_session), config.labelSession, stringResource(R.string.widget_label_session)) {
                        set(config.copy(labelSession = it))
                    }
                }
                add {
                    LabelField(stringResource(R.string.config_label_weekly), config.labelWeekly, stringResource(R.string.widget_label_week)) {
                        set(config.copy(labelWeekly = it))
                    }
                }
            }
            add { SwitchRow(Icons.Rounded.Schedule, stringResource(R.string.config_show_reset), config.showReset, { set(config.copy(showReset = it)) }) }
            if (config.showReset) {
                add {
                    Column {
                        ConnectedChoice(
                            listOf(
                                ResetFormat.CLOCK to stringResource(R.string.reset_clock),
                                ResetFormat.COUNTDOWN to stringResource(R.string.reset_countdown),
                            ),
                            config.resetFormat, { set(config.copy(resetFormat = it)) },
                        )
                        AnimatedVisibility(config.resetFormat == ResetFormat.COUNTDOWN) {
                            Text(
                                stringResource(R.string.config_countdown_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
            add { SwitchRow(Icons.Rounded.Percent, stringResource(R.string.config_percent_sign), config.showPercentSign, { set(config.copy(showPercentSign = it)) }) }
            add { SwitchRow(Icons.Rounded.SwapVert, stringResource(R.string.config_show_remaining), config.showRemaining, { set(config.copy(showRemaining = it)) }) }
        },
    )

    SettingsGroup(
        title = stringResource(R.string.config_sizes),
        rows = listOf(
            { SliderRow(Icons.Rounded.FormatSize, stringResource(R.string.config_number_size), config.numberSize, { set(config.copy(numberSize = it)) }) },
            { SliderRow(null, stringResource(R.string.config_label_size), config.labelSize, { set(config.copy(labelSize = it)) }) },
            { SliderRow(null, stringResource(R.string.config_reset_size), config.resetSize, { set(config.copy(resetSize = it)) }) },
        ),
    )

    SettingsGroup(
        title = stringResource(R.string.config_number_font),
        rows = buildList<@Composable () -> Unit> {
            add {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowIcon(Icons.Rounded.FontDownload)
                    Spacer(Modifier.width(16.dp))
                    ConnectedChoice(
                        listOf(NumberFont.FLEX to stringResource(R.string.font_flex), NumberFont.SYSTEM to stringResource(R.string.font_system)),
                        config.numberFont, { set(config.copy(numberFont = it)) }, modifier = Modifier.weight(1f),
                    )
                }
            }
            if (config.numberFont == NumberFont.FLEX) {
                add {
                    Text(
                        "73%",
                        fontFamily = flexFamily((300 + 700 * config.numberWeight).roundToInt(), 60f + 91f * config.numberWidth),
                        fontSize = 52.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                add { SliderRow(null, stringResource(R.string.config_number_weight), config.numberWeight, { set(config.copy(numberWeight = it)) }) }
                add { SliderRow(null, stringResource(R.string.config_number_width), config.numberWidth, { set(config.copy(numberWidth = it)) }) }
            }
        },
    )
}

@Composable
private fun ColorTab(config: WidgetConfig, set: (WidgetConfig) -> Unit) {
    SettingsGroup(
        title = stringResource(R.string.config_color_source),
        rows = buildList<@Composable () -> Unit> {
            add { ColorSourcePicker(config) { set(config.copy(source = it)) } }
            if (config.source == ColorSource.CUSTOM) {
                add {
                    SliderRow(
                        Icons.Rounded.ColorLens, stringResource(R.string.config_hue), config.customHue,
                        { set(config.copy(customHue = it)) }, range = 0f..359f, valueText = "${config.customHue.roundToInt()}°", track = hueBrush(),
                    )
                }
            }
        },
    )
    SettingsGroup(
        title = null,
        rows = listOf {
            ChoiceRow(
                Icons.Rounded.SettingsBrightness, stringResource(R.string.config_theme),
                listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.config_theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.config_theme_light),
                    ThemeMode.DARK to stringResource(R.string.config_theme_dark),
                ),
                config.theme, { set(config.copy(theme = it)) },
                icons = listOf(Icons.Rounded.SettingsBrightness, Icons.Rounded.LightMode, Icons.Rounded.DarkMode),
            )
        },
    )
    SettingsGroup(
        title = stringResource(R.string.config_indicator_colors),
        rows = buildList<@Composable () -> Unit> {
            add {
                ConnectedChoice(
                    listOf(
                        IndicatorColors.ROLES to stringResource(R.string.config_indicator_roles),
                        IndicatorColors.SINGLE to stringResource(R.string.config_indicator_single),
                        IndicatorColors.LEVEL to stringResource(R.string.config_indicator_level),
                    ),
                    config.indicatorColors, { set(config.copy(indicatorColors = it)) },
                )
            }
            if (config.indicatorColors == IndicatorColors.LEVEL) {
                add {
                    SliderRow(
                        Icons.Rounded.WarningAmber, stringResource(R.string.config_warn_at), config.warnAt,
                        { set(config.copy(warnAt = it.roundToInt().toFloat())) }, range = 30f..95f, valueText = "${config.warnAt.roundToInt()} %",
                    )
                }
                add {
                    SliderRow(
                        null, stringResource(R.string.config_critical_at), config.criticalAt,
                        { set(config.copy(criticalAt = it.roundToInt().toFloat())) }, range = 31f..100f, valueText = "${config.criticalAt.roundToInt()} %",
                    )
                }
            }
        },
    )
}

@Composable
private fun CardTab(config: WidgetConfig, set: (WidgetConfig) -> Unit) {
    val context = LocalContext.current
    SettingsGroup(
        title = stringResource(R.string.config_container),
        rows = buildList<@Composable () -> Unit> {
            add { ContainerPicker(config) { set(config.copy(container = it)) } }
            if (config.container != ContainerRole.NONE) {
                add {
                    SliderRow(
                        Icons.Rounded.Opacity, stringResource(R.string.config_opacity), config.containerOpacity,
                        { set(config.copy(containerOpacity = it)) }, valueText = "${(config.containerOpacity * 100).roundToInt()} %",
                    )
                }
                add {
                    val oneUi = remember { isOneUiHome(context) }
                    Column {
                        SwitchRow(Icons.Rounded.BlurOn, stringResource(R.string.config_blur), config.blur, { set(config.copy(blur = it)) })
                        AnimatedVisibility(config.blur) {
                            val note = stringResource(if (oneUi) R.string.blur_active else R.string.blur_unsupported)
                            val shapeNote = stringResource(R.string.blur_shape_hint)
                            InfoNote(if (config.cardShape != CardShape.ROUNDED) "$note\n\n$shapeNote" else note)
                        }
                    }
                }
            }
        },
    )
    SettingsGroup(
        title = stringResource(R.string.config_card_shape),
        rows = buildList<@Composable () -> Unit> {
            add { CardShapePicker(config.cardShape) { set(config.copy(cardShape = it)) } }
            if (config.cardShape == CardShape.ROUNDED || config.blur) {
                add {
                    SliderRow(
                        Icons.Rounded.Category, stringResource(R.string.config_corners), config.cornerDp,
                        { set(config.copy(cornerDp = it)) }, range = 0f..48f, valueText = "${config.cornerDp.roundToInt()} dp",
                    )
                }
            }
            add { SwitchRow(Icons.Rounded.BorderOuter, stringResource(R.string.config_outline), config.outline, { set(config.copy(outline = it)) }) }
        },
    )
    SettingsGroup(
        title = stringResource(R.string.config_spacing_group),
        rows = listOf(
            { SliderRow(Icons.Rounded.SpaceBar, stringResource(R.string.config_padding), config.padding, { set(config.copy(padding = it)) }) },
            { SliderRow(null, stringResource(R.string.config_spacing), config.spacing, { set(config.copy(spacing = it)) }) },
        ),
    )
}

@Composable
private fun BehaviorTab(config: WidgetConfig, set: (WidgetConfig) -> Unit, actions: ConfigActions) {
    val haptics = rememberHaptics()
    val taps = listOf(
        Triple(TapAction.REFRESH, R.string.tap_refresh, Icons.Rounded.Refresh),
        Triple(TapAction.OPEN_APP, R.string.tap_open_app, Icons.Rounded.Apps),
        Triple(TapAction.OPEN_CLAUDE, R.string.tap_open_claude, Icons.AutoMirrored.Rounded.OpenInNew),
        Triple(TapAction.OPEN_SETTINGS, R.string.tap_open_settings, Icons.Rounded.Settings),
        Triple(TapAction.NONE, R.string.tap_none, Icons.Rounded.TouchApp),
    )
    SettingsGroup(
        title = stringResource(R.string.config_tap),
        rows = taps.map { (action, label, icon) ->
            {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (config.tapAction != action) haptics.tick()
                        set(config.copy(tapAction = action))
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RowIcon(icon)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    RadioButton(selected = config.tapAction == action, onClick = {
                        if (config.tapAction != action) haptics.tick()
                        set(config.copy(tapAction = action))
                    })
                }
            }
        },
    )
    SettingsGroup(
        title = stringResource(R.string.config_freshness),
        rows = listOf(
            { SwitchRow(Icons.Rounded.Update, stringResource(R.string.config_show_updated), config.showUpdated, { set(config.copy(showUpdated = it)) }) },
            {
                SwitchRow(
                    Icons.Rounded.WarningAmber, stringResource(R.string.config_stale_hint), config.staleHint,
                    { set(config.copy(staleHint = it)) }, stringResource(R.string.config_stale_hint_sub),
                )
            },
        ),
    )
    SettingsGroup(
        title = stringResource(R.string.config_transfer),
        rows = listOf(
            { ActionRow(Icons.Rounded.Widgets, stringResource(R.string.action_copy_from), null, actions.copyFrom) },
            { ActionRow(Icons.Rounded.Add, stringResource(R.string.preset_save), null, actions.savePreset) },
            { ActionRow(Icons.Rounded.ContentCopy, stringResource(R.string.action_export), stringResource(R.string.action_export_sub), actions.export) },
            { ActionRow(Icons.Rounded.ContentPaste, stringResource(R.string.action_import), stringResource(R.string.action_import_sub), actions.import) },
            { ActionRow(Icons.Rounded.RestartAlt, stringResource(R.string.action_reset), null, actions.reset) },
        ),
    )
}

// ---------------------------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RowIcon(icon)
        Spacer(Modifier.width(16.dp))
        RowText(title, subtitle, Modifier.weight(1f))
    }
}

@Composable
private fun InfoNote(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun LabelField(title: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(MAX_LABEL)) },
        label = { Text(title) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LimitOrderRow(
    slot: LimitSlot,
    shown: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
) {
    val haptics = rememberHaptics()
    val title = stringResource(
        when (slot) {
            LimitSlot.SESSION -> R.string.limit_session
            LimitSlot.WEEKLY -> R.string.limit_weekly
            LimitSlot.MODELS -> R.string.config_models
        }
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = shown, onCheckedChange = {
            haptics.toggle(it)
            onToggle(it)
        })
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = {
            haptics.tick()
            onMove(-1)
        }, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.action_move_up), Modifier.size(18.dp))
        }
        Spacer(Modifier.width(6.dp))
        FilledTonalIconButton(onClick = {
            haptics.tick()
            onMove(1)
        }, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.action_move_down), Modifier.size(18.dp))
        }
    }
}

/** A swatch whose shape morphs between two Material shapes when it is selected. */
@Composable
private fun MorphSwatch(
    selected: Boolean,
    idle: RoundedPolygon,
    active: RoundedPolygon,
    color: Color,
    contentDescription: String,
    onClick: () -> Unit,
    brush: Brush? = null,
    label: String? = null,
    /** Width to height of the inner shape, for shapes that are not square, like the pill. */
    aspect: Float = 1f,
) {
    val haptics = rememberHaptics()
    val shape = rememberMorphShape(idle, active, selected)
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) scheme.primary else Color.Transparent, CircleShape)
                .clickable(onClickLabel = contentDescription) {
                    haptics.tick()
                    onClick()
                }
                .padding(7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .then(if (aspect == 1f) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(aspect))
                    .clip(shape)
                    .then(if (brush != null) Modifier.background(brush) else Modifier.background(color))
                    // Keeps swatches visible whose colour matches the row behind them.
                    .border(1.dp, scheme.outlineVariant, shape),
            )
        }
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ColorSourcePicker(config: WidgetConfig, onSelect: (ColorSource) -> Unit) {
    val context = LocalContext.current
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ColorSource.entries.forEach { source ->
            val primary = remember(source, config.theme, config.customHue) { widgetColorScheme(context, config.copy(source = source)).primary }
            val name = colorSourceName(source)
            MorphSwatch(
                selected = source == config.source,
                idle = MaterialShapes.Circle,
                active = MaterialShapes.Cookie9Sided,
                color = primary,
                brush = if (source == ColorSource.CUSTOM) Brush.sweepGradient((0..12).map { hueColor(it * 30f) }) else null,
                contentDescription = name,
                label = name,
                onClick = { onSelect(source) },
            )
        }
    }
}

@Composable
private fun colorSourceName(source: ColorSource): String = stringResource(
    when (source) {
        ColorSource.DYNAMIC -> R.string.source_dynamic
        ColorSource.CUSTOM -> R.string.source_custom
        ColorSource.ORANGE -> R.string.source_orange
        ColorSource.RED -> R.string.source_red
        ColorSource.PINK -> R.string.source_pink
        ColorSource.PURPLE -> R.string.source_purple
        ColorSource.BLUE -> R.string.source_blue
        ColorSource.CYAN -> R.string.source_cyan
        ColorSource.GREEN -> R.string.source_green
        ColorSource.YELLOW -> R.string.source_yellow
    }
)

@Composable
private fun ContainerPicker(config: WidgetConfig, onSelect: (ContainerRole) -> Unit) {
    val context = LocalContext.current
    val scheme = remember(config.source, config.theme, config.customHue) { widgetColorScheme(context, config) }
    val roles = listOf(
        Triple(ContainerRole.SURFACE, scheme.surfaceContainer, R.string.config_container_surface),
        Triple(ContainerRole.SURFACE_HIGH, scheme.surfaceContainerHighest, R.string.config_container_surface_high),
        Triple(ContainerRole.PRIMARY, scheme.primaryContainer, R.string.config_container_primary),
        Triple(ContainerRole.SECONDARY, scheme.secondaryContainer, R.string.config_container_secondary),
        Triple(ContainerRole.TERTIARY, scheme.tertiaryContainer, R.string.config_container_tertiary),
        Triple(ContainerRole.INVERSE, scheme.inverseSurface, R.string.config_container_inverse),
        Triple(ContainerRole.NONE, Color.Transparent, R.string.config_container_none),
    )
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        roles.forEach { (role, color, nameRes) ->
            val name = stringResource(nameRes)
            MorphSwatch(
                selected = role == config.container,
                idle = MaterialShapes.Square,
                active = MaterialShapes.Cookie4Sided,
                color = if (role == ContainerRole.NONE) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f) else color,
                contentDescription = name,
                label = name,
                onClick = { onSelect(role) },
            )
        }
    }
}

@Composable
private fun CardShapePicker(selected: CardShape, onSelect: (CardShape) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CardShape.entries.forEach { shape ->
            val polygon = WidgetPainter.cardPolygon(shape)
            MorphSwatch(
                selected = shape == selected,
                idle = polygon,
                active = polygon,
                color = if (shape == selected) scheme.primary else scheme.outline,
                contentDescription = shape.name,
                aspect = if (shape == CardShape.PILL) 2f else 1f,
                onClick = { onSelect(shape) },
            )
        }
    }
}

@Composable
private fun GaugeShapePicker(selected: GaugeShape, onSelect: (GaugeShape) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GaugeShape.entries.forEach { shape ->
            val polygon = WidgetPainter.gaugePolygon(shape)
            MorphSwatch(
                selected = shape == selected,
                idle = polygon,
                active = polygon,
                color = if (shape == selected) scheme.primary else scheme.outline,
                contentDescription = shape.name,
                onClick = { onSelect(shape) },
            )
        }
    }
}

/** One of the twelve styles: a small drawing of it, whose backdrop morphs when selected. */
@Composable
private fun StyleTile(style: WidgetStyle, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val color = if (selected) scheme.onPrimaryContainer else scheme.primary
    val track = (if (selected) scheme.onPrimaryContainer else scheme.onSurface).copy(alpha = 0.16f)
    val thumb = remember(style, color, track) { styleThumbnail(context, style, color.toArgb(), track.toArgb(), density) }
    val shape = rememberMorphShape(MaterialShapes.Square, MaterialShapes.Cookie9Sided, selected)
    val bg by animateColorAsState(
        if (selected) scheme.primaryContainer else scheme.surfaceContainerHighest,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tile",
    )
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable {
            haptics.tick()
            onClick()
        }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(60.dp).clip(shape).background(bg), contentAlignment = Alignment.Center) {
            Image(thumb, null, Modifier.size(36.dp), contentScale = ContentScale.Fit)
        }
        Text(
            stringResource(styleName(style)),
            style = if (selected) MaterialTheme.typography.labelMediumEmphasized else MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun styleThumbnail(context: Context, style: WidgetStyle, color: Int, track: Int, density: Float): ImageBitmap {
    val size = (48 * density).roundToInt()
    val bitmap: Bitmap = when (style) {
        WidgetStyle.RINGS -> WidgetPainter.ring(size, 0.66f, color, track, WidgetPainter.RingSpec(0.55f))
        WidgetStyle.WAVY_RINGS -> WidgetPainter.ring(size, 0.66f, color, track, WidgetPainter.RingSpec(0.55f, WidgetPainter.Wave(0.7f, 0.4f)))
        WidgetStyle.CONCENTRIC -> WidgetPainter.concentric(size, listOf(0.75f, 0.5f, 0.3f), listOf(color, color, color), track, WidgetPainter.RingSpec(0.6f))
        WidgetStyle.GAUGE -> WidgetPainter.gauge(size, 0.6f, color, track, 0.6f, null, 0)
        WidgetStyle.BARS -> WidgetPainter.bar(size, density, 0.6f, color, track, 0.6f, WidgetPainter.Wave.NONE, null, 0)
        WidgetStyle.WAVY_BARS -> WidgetPainter.bar(size, density, 0.7f, color, track, 0.5f, WidgetPainter.Wave(0.6f, 0.2f), null, 0)
        WidgetStyle.SEGMENTS -> WidgetPainter.segments(size, density, 0.6f, color, track, 0.6f, 5, null, 0)
        WidgetStyle.DOTS -> WidgetPainter.dots(size, 0.6f, color, track, 4, GaugeShape.SUNNY)
        WidgetStyle.SHAPES -> WidgetPainter.shape(size, 0.6f, GaugeShape.COOKIE, color, track)
        WidgetStyle.FILL -> WidgetPainter.fillTile((size * 0.72f).roundToInt(), size, 10 * density, 0.6f, color, density)
        WidgetStyle.NUMBERS -> WidgetPainter.number(context, "42", size * 0.62f, color, 0.85f, 0.7f)
            ?: WidgetPainter.shape(size, 0.42f, GaugeShape.CIRCLE, color, track)
        WidgetStyle.COMBO -> WidgetPainter.ring(size, 0.7f, color, track, WidgetPainter.RingSpec(0.6f, total = 270f, canvasStart = 135f))
    }
    return bitmap.asImageBitmap()
}

private fun styleName(style: WidgetStyle): Int = when (style) {
    WidgetStyle.RINGS -> R.string.style_rings
    WidgetStyle.WAVY_RINGS -> R.string.style_wavy_rings
    WidgetStyle.CONCENTRIC -> R.string.style_concentric
    WidgetStyle.GAUGE -> R.string.style_gauge
    WidgetStyle.BARS -> R.string.style_bars
    WidgetStyle.WAVY_BARS -> R.string.style_wavy_bars
    WidgetStyle.SEGMENTS -> R.string.style_segments
    WidgetStyle.DOTS -> R.string.style_dots
    WidgetStyle.SHAPES -> R.string.style_shapes
    WidgetStyle.FILL -> R.string.style_fill
    WidgetStyle.NUMBERS -> R.string.style_numbers
    WidgetStyle.COMBO -> R.string.style_combo
}

private fun presetName(preset: Preset): Int = when (preset) {
    Preset.PIXEL -> R.string.preset_pixel
    Preset.EXPRESSIVE -> R.string.preset_expressive
    Preset.SHAPES -> R.string.preset_shapes
    Preset.MINIMAL -> R.string.preset_minimal
    Preset.BARS -> R.string.preset_bars
    Preset.GAUGE -> R.string.preset_gauge
    Preset.CONTRAST -> R.string.preset_contrast
    Preset.GLASS -> R.string.preset_glass
    Preset.MONO -> R.string.preset_mono
    Preset.COMBO -> R.string.preset_combo
    Preset.SEGMENTS -> R.string.preset_segments
    Preset.DOTS -> R.string.preset_dots
}

/** A preset as a tile in its own card colour and shape, with its gauge drawn on it. */
@Composable
private fun PresetTile(name: String, look: WidgetConfig, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val tones = remember(look) { widgetTones(context, look.copy(containerOpacity = 1f, blur = false)) }
    val thumb = remember(look) { styleThumbnail(context, look.style, tones.accent(0, 40.0), tones.track, density) }
    val card = if (look.container == ContainerRole.NONE) MaterialTheme.colorScheme.surfaceContainerHighest else Color(tones.containerSolid)
    val shape = if (look.cardShape == CardShape.ROUNDED) RoundedCornerShape(22.dp) else WidgetPainter.cardPolygon(look.cardShape).toShape()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    haptics.tick()
                    onClick()
                },
                onLongClick = onLongClick,
            )
            .padding(vertical = 4.dp),
    ) {
        Box(Modifier.size(68.dp).clip(shape).background(card), contentAlignment = Alignment.Center) {
            Image(thumb, null, Modifier.size(38.dp), contentScale = ContentScale.Fit)
        }
        Text(name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun AddPresetTile(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
    ) {
        Box(
            Modifier.size(68.dp).clip(RoundedCornerShape(22.dp)).border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.preset_new), style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SavePresetDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Add, null) },
        title = { Text(stringResource(R.string.preset_save)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_LABEL) },
                label = { Text(stringResource(R.string.preset_name)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Lists the other CLimit widgets and copies all settings of the one picked. */
@Composable
private fun CopyFromDialog(currentId: GlanceId, onDismiss: () -> Unit, onPick: (WidgetConfig) -> Unit) {
    val context = LocalContext.current
    var others by remember { mutableStateOf<List<WidgetConfig>?>(null) }
    LaunchedEffect(Unit) {
        val manager = GlanceAppWidgetManager(context)
        others = manager.getGlanceIds(UsageWidget::class.java).filter { it != currentId }.mapNotNull { id ->
            runCatching {
                WidgetConfig.from(
                    getAppWidgetState(context, PreferencesGlanceStateDefinition, id),
                    UsageWidget.defaultsFor(context, manager.getAppWidgetId(id)),
                )
            }.getOrNull()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Widgets, null) },
        title = { Text(stringResource(R.string.action_copy_from)) },
        text = {
            val list = others
            when {
                list == null -> Unit
                list.isEmpty() -> Text(stringResource(R.string.no_other_widgets))
                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    list.forEachIndexed { index, cfg ->
                        Surface(onClick = { onPick(cfg) }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                PresetThumb(cfg)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.widget_number, index + 1), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        stringResource(styleName(cfg.style)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

@Composable
private fun PresetThumb(look: WidgetConfig) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val tones = remember(look) { widgetTones(context, look.copy(containerOpacity = 1f, blur = false)) }
    val thumb = remember(look) { styleThumbnail(context, look.style, tones.accent(0, 40.0), tones.track, density) }
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(tones.containerSolid)), contentAlignment = Alignment.Center) {
        Image(thumb, null, Modifier.size(26.dp))
    }
}
