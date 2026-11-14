package io.github.professorquantumuniverse.climit.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Why a fetch failed, in terms the UI can act on. */
sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** The session cookie is missing, expired or revoked: the user has to sign in again. */
    class SignedOut : ApiException("Session is no longer valid")

    /** Cloudflare or the server refused this client without it being an auth problem. */
    class Blocked(code: Int) : ApiException("Request was blocked (HTTP $code)")

    class RateLimited : ApiException("Too many requests")

    class Server(code: Int) : ApiException("Unexpected HTTP $code")

    class Network(cause: IOException) : ApiException("Network error", cause)

    class Malformed(cause: Throwable) : ApiException("Unreadable response", cause)
}

data class Organization(val uuid: String, val name: String?, val capabilities: List<String>)

/**
 * Talks to claude.ai with the user's session cookie.
 *
 * The cookie is passed in per call and never stored or logged here. Redirects are not followed,
 * so the cookie cannot be carried to another host.
 */
class ClaudeApi(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://claude.ai",
) {

    suspend fun organizations(sessionKey: String): List<Organization> {
        val body = get("/api/organizations", sessionKey)
        return try {
            (Json.parseToJsonElement(body) as JsonArray).mapNotNull { element ->
                val o = element as? JsonObject ?: return@mapNotNull null
                val uuid = (o["uuid"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                    ?: return@mapNotNull null
                Organization(
                    uuid = uuid,
                    name = (o["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content,
                    capabilities = (o["capabilities"] as? JsonArray).orEmpty()
                        .mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content },
                )
            }
        } catch (e: Exception) {
            throw ApiException.Malformed(e)
        }
    }

    /** Raw JSON of the usage endpoint; parse it with [UsageParser]. */
    suspend fun usageBody(sessionKey: String, organizationUuid: String): String {
        // A damaged stored id cannot be fixed by retrying; a fresh sign-in stores a new one.
        if (!UUID_PATTERN.matches(organizationUuid)) throw ApiException.SignedOut()
        return get("/api/organizations/$organizationUuid/usage", sessionKey)
    }

    /** Asks the server to end this session, the same call the website's sign-out makes. */
    suspend fun logout(sessionKey: String) {
        execute(sessionKey) {
            url("$baseUrl/api/auth/logout").post("{}".toRequestBody("application/json".toMediaType()))
        }
    }

    private suspend fun get(path: String, sessionKey: String): String =
        execute(sessionKey) { url(baseUrl + path) }

    private suspend fun execute(
        sessionKey: String,
        configure: Request.Builder.() -> Unit,
    ): String = withContext(Dispatchers.IO) {
        // A malformed key can never be valid, and must not reach a header.
        if (!SESSION_KEY_PATTERN.matches(sessionKey)) throw ApiException.SignedOut()
        val request = Request.Builder()
            .apply(configure)
            .header("Cookie", "sessionKey=$sessionKey")
            .header("Accept", "application/json")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                when (val code = response.code) {
                    200 -> response.body?.string() ?: throw ApiException.Malformed(IOException("Empty body"))
                    401 -> throw ApiException.SignedOut()
                    // claude.ai answers an invalid session with 403 + JSON; Cloudflare with 403 + HTML.
                    403 -> if (response.header("Content-Type").orEmpty().contains("json")) {
                        throw ApiException.SignedOut()
                    } else {
                        throw ApiException.Blocked(code)
                    }
                    429 -> throw ApiException.RateLimited()
                    // Redirects are disabled; a redirect here usually points at the login page.
                    in 300..399 -> throw ApiException.SignedOut()
                    else -> throw ApiException.Server(code)
                }
            }
        } catch (e: IOException) {
            throw ApiException.Network(e)
        }
    }

    companion object {
        private val UUID_PATTERN = Regex("[0-9a-fA-F-]{36}")

        // Keeps header injection out: no whitespace, semicolons or control characters.
        private val SESSION_KEY_PATTERN = Regex("[A-Za-z0-9._~+/=-]{16,4096}")

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
