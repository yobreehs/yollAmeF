@file:Suppress("DEPRECATION")

package io.github.nexalloy.activity

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import io.github.libxposed.service.XposedService
import io.github.nexalloy.AppPatchInfo
import io.github.nexalloy.Patch
import io.github.nexalloy.appPatchConfigurations
import io.github.nexalloy.ui.screens.AppPatchSettingsScreen

class AppPatchSettingsActivity : ComponentActivity(), SettingApplication.ServiceStateListener {

    companion object {
        const val ARGUMENT_APP_NAME = "app_name_key"
    }

    private val serviceState = mutableStateOf<XposedService?>(null)
    private val remotePrefsState = mutableStateOf<SharedPreferences?>(null)
    private val loadingState = mutableStateOf(true)

    private lateinit var appPatchInfo: AppPatchInfo
    private lateinit var patches: List<Patch>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra(ARGUMENT_APP_NAME)
        val info = appPatchConfigurations.firstOrNull { it.appName == appName }
        if (info == null) {
            finish()
            return
        }
        appPatchInfo = info
        patches = info.patches.toList()

        setContent {
            AppPatchSettingsScreen(
                appName = appPatchInfo.appName,
                packageName = appPatchInfo.packageName,
                patches = patches,
                remotePrefs = remotePrefsState.value,
                loading = loadingState.value,
                serviceAvailable = serviceState.value != null,
                onBack = { finish() },
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
            remotePrefsState.value = service?.let {
                runCatching { it.getRemotePreferences(appPatchInfo.packageName) }.getOrNull()
            }
        }
    }
}
