package io.github.professorquantumuniverse.climit.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.professorquantumuniverse.climit.R
import kotlinx.coroutines.delay

private const val LOGIN_URL = "https://claude.ai/login"
private const val COOKIE_URL = "https://claude.ai"
private const val ALLOWED_HOST = "claude.ai"

/** What checking a session cookie from the sign-in page came to. */
enum class CandidateResult {
    ACCEPTED,

    /** The server does not take this cookie; wait for a different one. */
    REJECTED,

    /** It could not be checked (no connection, rate limit); try the same cookie again. */
    RETRY,
}

/**
 * Shows the claude.ai sign-in page in a locked-down WebView and waits for the session cookie.
 *
 * [onCandidate] gets every new `sessionKey` value and says what the server made of it.
 * Once one is accepted the WebView's cookies and storage are wiped, so the app's encrypted
 * copy is the only one left on the device.
 */
@Composable
fun LoginScreen(
    onCandidate: suspend (sessionKey: String) -> CandidateResult,
    onSignedIn: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var checking by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // No screenshots or screen recording of the sign-in page.
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            wipeWebData(webView)
        }
    }

    // The cookie is HttpOnly and the page is a single-page app, so there is no event to hook:
    // look at the cookie jar until a key shows up that the server accepts.
    LaunchedEffect(Unit) {
        var rejected: String? = null
        while (true) {
            delay(700)
            val candidate = readSessionKey() ?: continue
            if (candidate == rejected) continue
            checking = true
            val result = onCandidate(candidate)
            checking = false
            when (result) {
                CandidateResult.ACCEPTED -> {
                    onSignedIn()
                    return@LaunchedEffect
                }
                CandidateResult.REJECTED -> rejected = candidate
                // Offline or a hiccup: the same key may well work in a moment, so ask again.
                CandidateResult.RETRY -> delay(3_000)
            }
        }
    }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) view.goBack() else onCancel()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.login_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            }
            if (checking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext -> createLoginWebView(viewContext).also { webView = it } },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createLoginWebView(context: android.content.Context): WebView {
    // Start from a clean slate so a stale or foreign session is never picked up.
    wipeWebData(null)

    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        settings.apply {
            javaScriptEnabled = true // the sign-in page does not work without it
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            safeBrowsingEnabled = true
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

        webViewClient = object : WebViewClient() {
            // The top-level page may only ever be claude.ai over HTTPS. Embedded frames
            // (the Cloudflare check) are not affected.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val url = request.url
                val allowed = url.scheme == "https" && url.host == ALLOWED_HOST
                return !allowed
            }
        }
        loadUrl(LOGIN_URL)
    }
}

private fun readSessionKey(): String? =
    CookieManager.getInstance().getCookie(COOKIE_URL)
        ?.split(';')
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith("sessionKey=") }
        ?.substringAfter('=')
        ?.takeIf { it.isNotEmpty() }

/** Removes every trace the WebView keeps: cookies, storage, cache, history. */
private fun wipeWebData(webView: WebView?) {
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
    }
    WebStorage.getInstance().deleteAllData()
    webView?.apply {
        stopLoading()
        clearHistory()
        clearCache(true)
        clearFormData()
        (parent as? ViewGroup)?.removeView(this)
        destroy()
    }
}
