package io.github.nexalloy.ui.screens

import android.app.ActivityManager
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nexalloy.Patch
import io.github.nexalloy.R
import io.github.nexalloy.ui.components.AppIcon
import io.github.nexalloy.ui.components.RowDivider
import io.github.nexalloy.ui.components.SectionHeader
import io.github.nexalloy.ui.components.SettingsCard
import io.github.nexalloy.ui.components.SwitchRow
import io.github.nexalloy.ui.components.isPackageInstalled
import io.github.nexalloy.ui.theme.FemAlloyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPatchSettingsScreen(
    appName: String,
    packageName: String,
    patches: List<Patch>,
    remotePrefs: SharedPreferences?,
    loading: Boolean,
    serviceAvailable: Boolean,
    onBack: () -> Unit,
) {
    FemAlloyTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
        ) { padding ->
            if (remotePrefs == null) {
                WaitingContent(
                    loading = loading,
                    serviceAvailable = serviceAvailable,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            } else {
                PatchList(
                    appName = appName,
                    packageName = packageName,
                    patches = patches,
                    remotePrefs = remotePrefs,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
        }
    }
}

@Composable
private fun WaitingContent(
    loading: Boolean,
    serviceAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_connecting),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (serviceAvailable) R.string.settings_prefs_unavailable
                        else R.string.settings_module_not_available,
                    ),
                    modifier = Modifier.padding(20.dp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PatchList(
    appName: String,
    packageName: String,
    patches: List<Patch>,
    remotePrefs: SharedPreferences,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val visiblePatches = remember(patches) {
        patches
            .filter { it.name.isNotBlank() && !it.name.startsWith("<") }
            .sortedBy { it.name.lowercase() }
    }
    val states = remember(visiblePatches) {
        mutableStateMapOf<String, Boolean>().apply {
            visiblePatches.forEach { put(it.name, remotePrefs.getBoolean(it.name, it.use)) }
        }
    }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, visiblePatches) {
        if (query.isBlank()) visiblePatches
        else visiblePatches.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
        }
    }
    val installed = remember(packageName) { isPackageInstalled(context, packageName) }
    val launchIntent = remember(packageName) {
        context.packageManager.getLaunchIntentForPackage(packageName)
    }

    fun persist(key: String, value: Boolean) {
        states[key] = value
        remotePrefs.edit().putBoolean(key, value).apply()
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (vibrator?.hasVibrator() == true) {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25)
        }
    }

    fun stopAndOpen() {
        val intent = launchIntent ?: return
        runCatching {
            context.getSystemService(ActivityManager::class.java)
                ?.killBackgroundProcesses(packageName)
        }
        // Give the system a moment to tear the target process down before bringing it back.
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                context.startActivity(
                    Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.onFailure {
                Toast.makeText(
                    context,
                    it.message ?: "Unable to open $appName",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }, 350)
    }

    fun persistAll(value: (Patch) -> Boolean) {
        val editor = remotePrefs.edit()
        visiblePatches.forEach { patch ->
            val newValue = value(patch)
            states[patch.name] = newValue
            editor.putBoolean(patch.name, newValue)
        }
        editor.apply()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(packageName, appName, size = 52.dp)
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = appName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = packageName,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                RowDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (installed && launchIntent != null) {
                        FilledTonalButton(
                            onClick = { stopAndOpen() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.settings_stop_and_open), fontSize = 12.sp)
                        }
                    }
                    FilledTonalButton(
                        onClick = { persistAll { false } },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    ) {
                        Text(stringResource(R.string.settings_disable_all), fontSize = 12.sp)
                    }
                    if (installed) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.parse("package:$packageName"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.settings_app_info), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.settings_search_patches)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }

        item {
            SectionHeader(
                stringResource(
                    R.string.settings_patches_title_count,
                    filtered.size,
                    visiblePatches.size,
                ),
            )
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.settings_no_patches),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            item {
                SettingsCard {
                    filtered.forEachIndexed { index, patch ->
                        SwitchRow(
                            title = patch.name,
                            summary = patch.description.ifBlank { null },
                            checked = states[patch.name] ?: patch.use,
                            onCheckedChange = { persist(patch.name, it) },
                        )
                        if (index != filtered.lastIndex) RowDivider()
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
