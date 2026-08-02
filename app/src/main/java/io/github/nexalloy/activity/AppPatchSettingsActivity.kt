@file:Suppress("DEPRECATION")

package io.github.nexalloy.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Context.MODE_WORLD_READABLE
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.Button
import io.github.nexalloy.appPatchConfigurations
import io.github.nexalloy.R
import java.io.File

class AppPatchSettingsActivity : Activity() {

    companion object {
        const val ARGUMENT_APP_NAME = "app_name_key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_patch_settings)

        actionBar?.setDisplayHomeAsUpEnabled(true)

        val appName = intent.getStringExtra(ARGUMENT_APP_NAME)
        actionBar?.title = appName

        if (savedInstanceState != null) return
        val fragment = AppPatchSettingsFragment().apply {
            arguments = Bundle().apply {
                putString(ARGUMENT_APP_NAME, appName)
            }
        }
        fragmentManager.beginTransaction()
            .replace(R.id.app_patch_settings_container, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("WorldReadableFiles")
    class AppPatchSettingsFragment : PreferenceFragment() {

        @Deprecated("Deprecated in Java")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Retrieve appName from the Activity's Intent extras
            val appName = arguments?.getString(ARGUMENT_APP_NAME)
            val appPatchInfo = appPatchConfigurations.find { it.appName == appName }
            if (appPatchInfo == null) throw Exception("AppPatchInfo not found, app_name: $appName")
            val defaultPatchStates = appPatchInfo.patches.associate { it.name to it.use }

            val screen = preferenceManager.createPreferenceScreen(context)
            /** XSharedPreference
             * @see io.github.nexalloy.PatchExecutor.patchPreferences */
            preferenceManager.sharedPreferencesMode = resolvePreferencesMode(appPatchInfo.packageName)
            preferenceManager.sharedPreferencesName = appPatchInfo.packageName
            // Force-create the preference file (so target apps can read the default patch states
            // even before the user touches anything) and make it readable by other UIDs.
            runCatching {
                preferenceManager.sharedPreferences.let { prefs ->
                    val editor = prefs.edit()
                    defaultPatchStates.forEach { (key, value) ->
                        if (!prefs.contains(key)) editor.putBoolean(key, value)
                    }
                    editor.commit()
                    makePrefsWorldReadable(prefs)
                }
            }

            object : Preference(context) {
                @Deprecated("Deprecated in Java")
                override fun onBindView(view: View) {
                    super.onBindView(view)
                    view.findViewById<Button>(R.id.button_default).setOnClickListener {
                        restoreDefaultPreferences(defaultPatchStates)
                    }
                    view.findViewById<Button>(R.id.button_none).setOnClickListener {
                        setAllPreferences(false)
                    }
                    val isInstalled = runCatching {
                        context.packageManager.getPackageInfo(appPatchInfo.packageName, 0)
                    }.isSuccess

                    view.findViewById<Button>(R.id.button_app_info).apply {
                        if (!isInstalled) visibility = View.GONE
                        setOnClickListener {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${appPatchInfo.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                }
            }.apply {
                layoutResource = R.layout.preference_header_buttons
                screen.addPreference(this)
            }

            for (patchInfo in appPatchInfo.patches.sortedBy { it.name }) {
                if (patchInfo.name == "") continue
                if (patchInfo.name.startsWith("<")) continue
                CheckBoxPreference(context).apply {
                    /** XSharedPreference
                     * @see io.github.nexalloy.PatchExecutor.applyPatches */
                    key = patchInfo.name // Pref Key
                    title = patchInfo.name
                    summary = patchInfo.description
                    setDefaultValue(patchInfo.use)
                    setOnPreferenceChangeListener { preference, newValue ->
                        val vibrator =
                            context.getSystemService(VIBRATOR_SERVICE) as Vibrator?
                        if (vibrator?.hasVibrator() ?: false) {
                            vibrator.vibrate(50)
                        }
                        // Persist synchronously so the target app's XSharedPreferences observes the
                        // change immediately, and keep the file accessible from other UIDs.
                        runCatching {
                            preference.preferenceManager.sharedPreferences.let { prefs ->
                                prefs.edit().putBoolean(preference.key, newValue as Boolean).commit()
                                makePrefsWorldReadable(prefs)
                            }
                        }
                        true
                    }
                    screen.addPreference(this)
                }
            }

            preferenceScreen = screen
        }

        /**
         * LSPosed (new XSharedPreferences, min API 93) redirects the module's preference files into
         * `/data/misc/<uuid>/prefs/<modulePkg>/` and only publishes them world-readable when the
         * `MODE_WORLD_READABLE` flag is used. Frameworks that do not hook `checkMode` throw a
         * [SecurityException] on Android 14+ (targetSdk >= 34), in which case fall back to
         * [MODE_PRIVATE] and rely on the explicit chmod in [makePrefsWorldReadable].
         */
        @SuppressLint("WorldReadableFiles")
        private fun resolvePreferencesMode(prefsName: String): Int =
            try {
                context!!.getSharedPreferences(prefsName, MODE_WORLD_READABLE)
                MODE_WORLD_READABLE
            } catch (_: SecurityException) {
                MODE_PRIVATE
            }

        /**
         * Target apps run under a different UID, so the preference file (and its parent directory)
         * must be readable/writable by everyone. On LSPosed this targets the world-readable redirect
         * file under `/data/misc`. Best-effort: never crashes when the framework rejects the change.
         */
        @SuppressLint("WorldReadableFiles")
        private fun makePrefsWorldReadable(prefs: SharedPreferences) {
            runCatching {
                val file = prefs.javaClass.getMethod("getFile").invoke(prefs) as File
                file.parentFile?.setReadable(true, false)
                file.parentFile?.setExecutable(true, false)
                file.setReadable(true, false)
                file.setWritable(true, false)
            }
        }

        fun setAllPreferences(enable: Boolean) {
            if (!isAdded) return
            for (i in 0 until preferenceScreen.preferenceCount) {
                val preference = preferenceScreen.getPreference(i)
                if (preference is CheckBoxPreference) {
                    preference.isChecked = enable
                }
            }
        }

        fun restoreDefaultPreferences(defaultPatchStates: Map<String, Boolean>) {
            if (!isAdded) return
            for (i in 0 until preferenceScreen.preferenceCount) {
                val preference = preferenceScreen.getPreference(i)
                if (preference is CheckBoxPreference) {
                    preference.isChecked = defaultPatchStates[preference.key] ?: preference.isChecked
                }
            }
        }
    }
}
