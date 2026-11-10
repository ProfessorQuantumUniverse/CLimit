package io.github.professorquantumuniverse.climit.data

import android.content.Context
import java.io.File

/**
 * Debug builds only: if files/demo_usage.json exists, the app shows it instead of fetching real
 * usage. Used for screenshots and for testing widgets without an account. Release builds have
 * a version of this object that always returns null.
 */
object DemoData {
    fun usageJson(context: Context): String? =
        File(context.applicationContext.filesDir, "demo_usage.json").takeIf { it.isFile }?.readText()
}
