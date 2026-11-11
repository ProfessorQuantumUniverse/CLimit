package io.github.professorquantumuniverse.climit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.professorquantumuniverse.climit.data.ApiException
import io.github.professorquantumuniverse.climit.data.SessionRepository
import io.github.professorquantumuniverse.climit.ui.ClimitTheme
import io.github.professorquantumuniverse.climit.ui.LoginScreen
import io.github.professorquantumuniverse.climit.ui.UsageScreen
import io.github.professorquantumuniverse.climit.ui.WelcomeScreen
import io.github.professorquantumuniverse.climit.widget.PlacedWidget
import io.github.professorquantumuniverse.climit.widget.RefreshWorker
import io.github.professorquantumuniverse.climit.widget.UsageWidget
import io.github.professorquantumuniverse.climit.widget.UsageWidgetCompactReceiver
import io.github.professorquantumuniverse.climit.widget.UsageWidgetReceiver
import io.github.professorquantumuniverse.climit.widget.WidgetConfigActivity
import io.github.professorquantumuniverse.climit.widget.placedWidgets
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = SessionRepository(applicationContext)
        setContent {
            ClimitTheme { App(repository) }
        }
    }
}

private enum class Screen { WELCOME, LOGIN, USAGE }

@Composable
private fun App(repository: SessionRepository) {
    var screen by rememberSaveable {
        mutableStateOf(if (repository.isSignedIn()) Screen.USAGE else Screen.WELCOME)
    }
    val state by SessionRepository.state.collectAsState()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ApiException?>(null) }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current
    val context = activity.applicationContext

    suspend fun refresh() {
        loading = true
        try {
            repository.refresh()
            error = null
        } catch (e: ApiException) {
            error = e
        } finally {
            UsageWidget().updateAll(context)
            loading = false
        }
    }

    when (screen) {
        Screen.WELCOME -> WelcomeScreen(onSignIn = { screen = Screen.LOGIN })

        Screen.LOGIN -> LoginScreen(
            onCandidate = { sessionKey ->
                try {
                    repository.completeSignIn(sessionKey)
                    true
                } catch (_: ApiException) {
                    // Not accepted (yet), or offline: keep the sign-in page open.
                    false
                }
            },
            onSignedIn = {
                RefreshWorker.schedule(context)
                screen = Screen.USAGE
            },
            onCancel = { screen = Screen.WELCOME },
        )

        Screen.USAGE -> {
            LaunchedEffect(Unit) {
                // Show the last known numbers at once, then fetch fresh ones.
                repository.load()
                refresh()
            }
            // Widgets come and go on the home screen and change in their settings, so re-read on return.
            var widgets by remember { mutableStateOf(emptyList<PlacedWidget>()) }
            LifecycleResumeEffect(Unit) {
                val job = scope.launch { widgets = placedWidgets(context) }
                onPauseOrDispose { job.cancel() }
            }
            UsageScreen(
                snapshot = state?.snapshot,
                fetchedAt = state?.fetchedAt,
                loading = loading,
                error = error?.let { errorText(it) },
                onRefresh = { scope.launch { refresh() } },
                widgets = widgets,
                onAddWidget = { compact ->
                    scope.launch {
                        val receiver = if (compact) UsageWidgetCompactReceiver::class.java else UsageWidgetReceiver::class.java
                        // Setup is optional, so launchers skip it; open it here once the widget is placed.
                        GlanceAppWidgetManager(context).requestPinGlanceAppWidget(
                            receiver,
                            successCallback = WidgetConfigActivity.afterPin(context),
                        )
                    }
                },
                onEditWidget = { appWidgetId -> activity.startActivity(WidgetConfigActivity.intent(activity, appWidgetId)) },
                onSignOut = {
                    scope.launch {
                        loading = true
                        repository.signOut()
                        RefreshWorker.cancel(context)
                        UsageWidget().updateAll(context)
                        loading = false
                        error = null
                        screen = Screen.WELCOME
                    }
                },
            )
        }
    }
}

@Composable
private fun errorText(error: ApiException): String = when (error) {
    is ApiException.SignedOut -> stringResource(R.string.error_signed_out)
    is ApiException.Blocked -> stringResource(R.string.error_blocked)
    is ApiException.RateLimited -> stringResource(R.string.error_rate_limited)
    is ApiException.Network -> stringResource(R.string.error_network)
    is ApiException.Server, is ApiException.Malformed -> stringResource(R.string.error_unexpected)
}
