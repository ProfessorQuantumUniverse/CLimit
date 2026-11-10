package io.github.professorquantumuniverse.climit.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.max

/** A look the user saved under a name. */
@Serializable
data class SavedPreset(val name: String, val config: WidgetConfig)

/** User presets, kept in app preferences. They hold looks only, nothing personal. */
class PresetStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("presets", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val serializer = ListSerializer(SavedPreset.serializer())

    fun list(): List<SavedPreset> = try {
        json.decodeFromString(serializer, prefs.getString(KEY, null) ?: "[]")
            .map { it.copy(config = it.config.sanitized()) }
    } catch (_: Exception) {
        emptyList()
    }

    fun save(name: String, config: WidgetConfig) {
        val trimmed = name.trim().take(MAX_LABEL).ifEmpty { return }
        val updated = list().filterNot { it.name == trimmed } + SavedPreset(trimmed, config)
        prefs.edit { putString(KEY, json.encodeToString(serializer, updated)) }
    }

    fun delete(name: String) {
        prefs.edit { putString(KEY, json.encodeToString(serializer, list().filterNot { it.name == name })) }
    }

    private companion object {
        const val KEY = "saved"
    }
}

/** Backgrounds for the settings preview, so translucent and card-less looks can be judged. */
enum class PreviewBackground { DYNAMIC, LIGHT, DARK, SUNSET, CUSTOM }

/** Remembers the chosen preview background and keeps a downscaled copy of a picked image. */
class PreviewBackgroundStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("preview", Context.MODE_PRIVATE)
    private val file = File(appContext.filesDir, "preview_background.jpg")

    var background: PreviewBackground
        get() = runCatching { PreviewBackground.valueOf(prefs.getString("background", null)!!) }
            .getOrDefault(PreviewBackground.DYNAMIC)
            .let { if (it == PreviewBackground.CUSTOM && !file.exists()) PreviewBackground.DYNAMIC else it }
        set(value) = prefs.edit { putString("background", value.name) }

    fun customImage(): Bitmap? = if (file.exists()) BitmapFactory.decodeFile(file.path) else null

    /** Copies the picked image, scaled to at most 1440 px, and selects it. */
    fun importImage(uri: Uri): Bitmap? = try {
        val source = ImageDecoder.createSource(appContext.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longest = max(info.size.width, info.size.height)
            if (longest > 1440) {
                val scale = 1440f / longest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        background = PreviewBackground.CUSTOM
        bitmap
    } catch (_: Exception) {
        null
    }
}

private const val ONE_UI_HOME = "com.sec.android.app.launcher"

/** Whether the current home app is Samsung's One UI Home, the only launcher known to blur widgets. */
fun isOneUiHome(context: Context): Boolean {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val info = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
    return info?.activityInfo?.packageName == ONE_UI_HOME
}

fun copyToClipboard(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("CLimit", text))
}

fun readClipboard(context: Context): String? =
    context.getSystemService(ClipboardManager::class.java)?.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()

/** Parses pasted settings; null if the text is not a CLimit configuration. */
fun parseImported(text: String?): WidgetConfig? {
    if (text.isNullOrBlank() || !text.trimStart().startsWith("{") || !text.contains("\"style\"")) return null
    // fromJson hands back its fallback unchanged when parsing fails; compare by identity.
    val fallback = WidgetConfig()
    val parsed = WidgetConfig.fromJson(text, fallback)
    return parsed.takeIf { it !== fallback }
}
