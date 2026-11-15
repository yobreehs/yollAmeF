package io.github.nexalloy.ui

import android.content.Context

/**
 * Local (module APK) UI preferences. These are intentionally separate from the module's
 * Xposed remote preferences: the Xposed service is unavailable when the settings activity
 * is opened before the module is activated.
 */
class ModuleUiSettings(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("femalloy_ui", Context.MODE_PRIVATE)

    /**
     * Show only apps that are installed and enabled in the LSPosed scope.
     */
    var hideUnscopedApps: Boolean
        get() = prefs.getBoolean(KEY_HIDE_UNSCOPED, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_UNSCOPED, value).apply()

    private companion object {
        const val KEY_HIDE_UNSCOPED = "hide_unscoped_apps"
    }
}
