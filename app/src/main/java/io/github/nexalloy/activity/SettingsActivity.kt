@file:Suppress("DEPRECATION")

package io.github.nexalloy.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.settings.preference.about.MorpheAboutPreference
import io.github.libxposed.service.XposedService
import io.github.nexalloy.AppPatchInfo
import io.github.nexalloy.R
import io.github.nexalloy.common.UpdateChecker
import io.github.nexalloy.ui.ModuleUiSettings
import io.github.nexalloy.ui.screens.ModuleSettingsScreen
import kotlin.system.exitProcess

/**
 * LSPosed entry point for the module settings.
 *
 * The screen is built with Jetpack Compose Material 3 and mirrors the visual language of the
 * Femboy Root Detector settings screen: section headers, rounded cards, inset dividers and
 * switch rows.
 */
class SettingsActivity : ComponentActivity(), SettingApplication.ServiceStateListener {

    private val serviceState = mutableStateOf<XposedService?>(null)
    private val activatedState = mutableStateOf(false)
    private val loadingState = mutableStateOf(true)
    private val hideIconState = mutableStateOf(false)
    private val disableAutoCheckState = mutableStateOf(false)
    private val hideUnscopedAppsState = mutableStateOf(true)
    private val scopedPackagesState = mutableStateOf<Set<String>>(emptySet())

    private lateinit var uiSettings: ModuleUiSettings
    private lateinit var aboutPreference: MorpheAboutPreference
    private lateinit var updateChecker: UpdateChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        Utils.setContext(this)
        uiSettings = ModuleUiSettings(this)
        hideUnscopedAppsState.value = uiSettings.hideUnscopedApps

        aboutPreference = MorpheAboutPreference(this).apply {
            setTitle(R.string.about_title)
        }
        hideIconState.value = isLauncherIconHidden()

        updateChecker = UpdateChecker().apply {
            setActivity(this@SettingsActivity)
            autoCheckUpdate()
        }

        onBackPressedDispatcher.addCallback(this) {
            finishAndRemoveTask()
            exitProcess(0)
        }

        setContent {
            ModuleSettingsScreen(
                loading = loadingState.value,
                activated = activatedState.value,
                serviceAvailable = serviceState.value != null,
                hideIcon = hideIconState.value,
                disableAutoCheck = disableAutoCheckState.value,
                hideUnscopedApps = hideUnscopedAppsState.value,
                scopedPackages = scopedPackagesState.value,
                onHideIconChange = ::setLauncherIconHidden,
                onHideUnscopedAppsChange = ::setHideUnscopedApps,
                onDisableAutoCheckChange = ::setDisableAutoCheck,
                onOpenApp = ::openAppPatchSettings,
                onAbout = {
                    aboutPreference.onPreferenceClickListener
                        ?.onPreferenceClick(aboutPreference)
                },
                onCheckUpdate = {
                    updateChecker.checkUpdate(silent = false)
                },
                onOpenUrl = ::openUrl,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        SettingApplication.addServiceStateListener(this, true)
    }

    override fun onStop() {
        SettingApplication.removeServiceStateListener(this)
        super.onStop()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        runOnUiThread {
            loadingState.value = false
            serviceState.value = service
            activatedState.value = service != null && runCatching {
                service!!.getRemotePreferences("prefs")
                service.apiVersion
                true
            }.getOrDefault(false)
            disableAutoCheckState.value = service != null && runCatching {
                service!!.getRemotePreferences("prefs")
                    .getBoolean(KEY_DISABLE_AUTO_CHECK, false)
            }.getOrDefault(false)
            scopedPackagesState.value = runCatching {
                service?.scope?.toSet() ?: emptySet()
            }.getOrDefault(emptySet())
        }
    }

    private fun openAppPatchSettings(info: AppPatchInfo) {
        startActivity(
            Intent(this, AppPatchSettingsActivity::class.java)
                .putExtra(AppPatchSettingsActivity.ARGUMENT_APP_NAME, info.appName),
        )
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            Toast.makeText(this, it.message ?: "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setHideUnscopedApps(hide: Boolean) {
        hideUnscopedAppsState.value = hide
        uiSettings.hideUnscopedApps = hide
    }

    private fun setDisableAutoCheck(disabled: Boolean) {        val service = serviceState.value ?: return
        runCatching {
            service.getRemotePreferences("prefs").edit()
                .putBoolean(KEY_DISABLE_AUTO_CHECK, disabled)
                .apply()
            disableAutoCheckState.value = disabled
        }.onFailure {
            Toast.makeText(this, it.message ?: "Unable to save preference", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun launcherAlias(): ComponentName =
        ComponentName(this, SettingsActivity::class.java.name + "Alias")

    private fun isLauncherIconHidden(): Boolean =
        packageManager.getComponentEnabledSetting(launcherAlias()) ==
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    private fun setLauncherIconHidden(hidden: Boolean) {
        packageManager.setComponentEnabledSetting(
            launcherAlias(),
            if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        hideIconState.value = hidden
    }

    private companion object {
        const val KEY_DISABLE_AUTO_CHECK = "disable_auto_check_update"
    }
}
