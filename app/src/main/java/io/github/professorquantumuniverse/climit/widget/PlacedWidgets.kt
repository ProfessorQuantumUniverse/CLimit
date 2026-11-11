package io.github.professorquantumuniverse.climit.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition

/** A CLimit widget on the home screen, listed in the app so its settings open on any launcher. */
data class PlacedWidget(val appWidgetId: Int, val config: WidgetConfig)

/** The CLimit widgets on the home screen, oldest first. */
suspend fun placedWidgets(context: Context): List<PlacedWidget> {
    val manager = GlanceAppWidgetManager(context)
    return manager.getGlanceIds(UsageWidget::class.java).mapNotNull { id ->
        runCatching {
            val appWidgetId = manager.getAppWidgetId(id)
            PlacedWidget(
                appWidgetId,
                WidgetConfig.from(
                    getAppWidgetState(context, PreferencesGlanceStateDefinition, id),
                    UsageWidget.defaultsFor(context, appWidgetId),
                ),
            )
        }.getOrNull()
    }.sortedBy { it.appWidgetId }
}
