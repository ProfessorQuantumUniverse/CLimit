package io.github.professorquantumuniverse.climit.data

import android.content.Context
import android.util.AtomicFile
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Instant

/** What the app and the widgets show: sign-in status plus the last usage that was fetched. */
data class UsageState(
    val signedIn: Boolean,
    /** The server rejected the stored session; the user has to sign in again. */
    val sessionExpired: Boolean,
    val snapshot: UsageSnapshot?,
    val fetchedAt: Instant?,
)

/**
 * The one place that touches the session cookie: sign-in, fetching usage, sign-out.
 * UI and widgets only ever see [UsageState] and [ApiException]s.
 *
 * The last usage response is cached on disk so widgets can draw without a network call.
 * It holds percentages and timestamps only, nothing that identifies or authenticates the account.
 */
class SessionRepository(
    context: Context,
    private val api: ClaudeApi = ClaudeApi(),
) {
    private val appContext = context.applicationContext
    private val vault = SessionVault(appContext)

    // Holds only the organization UUID (an identifier, not a credential) and a status flag.
    private val prefs = appContext.getSharedPreferences("account", Context.MODE_PRIVATE)
    private val cacheFile = AtomicFile(File(appContext.noBackupFilesDir, "usage_cache.json"))

    fun isSignedIn(): Boolean =
        DemoData.usageJson(appContext) != null || (vault.hasSession() && prefs.getString(KEY_ORG, null) != null)

    /** Reads the current state from disk and publishes it to [state]. */
    suspend fun load(): UsageState = withContext(Dispatchers.IO) {
        DemoData.usageJson(appContext)?.let { demo ->
            return@withContext UsageState(true, false, UsageParser.parse(demo), Instant.now())
                .also { mutableState.value = it }
        }
        val cached = try {
            val body = String(cacheFile.readFully(), Charsets.UTF_8)
            UsageParser.parse(body) to Instant.ofEpochMilli(cacheFile.baseFile.lastModified())
        } catch (_: IOException) {
            null
        } catch (_: UsageParser.MalformedResponse) {
            null
        }
        UsageState(
            signedIn = isSignedIn(),
            sessionExpired = prefs.getBoolean(KEY_EXPIRED, false),
            snapshot = cached?.first,
            fetchedAt = cached?.second,
        ).also { mutableState.value = it }
    }

    /**
     * Checks [sessionKey] against the server and stores it if it works.
     * @throws ApiException.SignedOut if the server does not accept the key.
     */
    suspend fun completeSignIn(sessionKey: String) {
        val organizations = api.organizations(sessionKey)
        val organization = organizations.firstOrNull { "chat" in it.capabilities }
            ?: organizations.firstOrNull()
            ?: throw ApiException.SignedOut()
        withContext(Dispatchers.IO) { vault.save(sessionKey) }
        prefs.edit {
            putString(KEY_ORG, organization.uuid)
            putBoolean(KEY_EXPIRED, false)
        }
        load()
    }

    /**
     * Fetches fresh usage, caches it and publishes the new state. The app and the background
     * worker may both call this at once; the lock keeps them from writing the cache together.
     */
    suspend fun refresh(): UsageState = lock.withLock { fetch() }

    private suspend fun fetch(): UsageState {
        if (DemoData.usageJson(appContext) != null) return load()
        val organization = prefs.getString(KEY_ORG, null) ?: throw ApiException.SignedOut()
        val sessionKey = withContext(Dispatchers.IO) { vault.load() } ?: throw ApiException.SignedOut()
        val body = try {
            api.usageBody(sessionKey, organization)
        } catch (e: ApiException.SignedOut) {
            prefs.edit { putBoolean(KEY_EXPIRED, true) }
            load()
            throw e
        }
        try {
            UsageParser.parse(body)
        } catch (e: UsageParser.MalformedResponse) {
            throw ApiException.Malformed(e)
        }
        withContext(Dispatchers.IO) {
            val stream = cacheFile.startWrite()
            try {
                stream.write(body.toByteArray(Charsets.UTF_8))
                cacheFile.finishWrite(stream)
            } catch (e: IOException) {
                cacheFile.failWrite(stream)
            }
        }
        prefs.edit { putBoolean(KEY_EXPIRED, false) }
        return load()
    }

    /** Ends the session on the server if possible, then always wipes everything locally. */
    suspend fun signOut() = lock.withLock {
        // Under the lock, so a refresh still in flight cannot write the cache back afterwards.
        val sessionKey = withContext(Dispatchers.IO) { vault.load() }
        if (sessionKey != null) {
            try {
                api.logout(sessionKey)
            } catch (_: ApiException) {
                // Offline or refused: the local wipe below still happens.
            }
        }
        withContext(Dispatchers.IO) {
            vault.clear()
            cacheFile.delete()
        }
        prefs.edit { clear() }
        load()
    }

    companion object {
        private const val KEY_ORG = "organization_uuid"
        private const val KEY_EXPIRED = "session_expired"

        private val mutableState = MutableStateFlow<UsageState?>(null)
        private val lock = Mutex()

        /** Latest state in this process; null until [load] has run once. */
        val state: StateFlow<UsageState?> = mutableState
    }
}
