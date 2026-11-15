package io.github.nexalloy.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nexalloy.AppPatchInfo
import io.github.nexalloy.BuildConfig
import io.github.nexalloy.R
import io.github.nexalloy.appPatchConfigurations
import io.github.nexalloy.ui.components.AppIcon
import io.github.nexalloy.ui.components.Chevron
import io.github.nexalloy.ui.components.RowDivider
import io.github.nexalloy.ui.components.SectionHeader
import io.github.nexalloy.ui.components.SettingsCard
import io.github.nexalloy.ui.components.SettingsRow
import io.github.nexalloy.ui.components.SwitchRow
import io.github.nexalloy.ui.components.isPackageInstalled
import io.github.nexalloy.ui.theme.FemAlloyTheme

private data class LicenseEntry(val title: String, val summary: String, val url: String)

private val licenses = listOf(
    LicenseEntry("Morphe Patches", "GNU General Public License v3.0", "https://github.com/MorpheApp/morphe-patches"),
    LicenseEntry("hoodles Morphe Patches", "GNU General Public License v3.0", "https://github.com/hoo-dles/morphe-patches"),
    LicenseEntry("Morphe Patcher", "GNU General Public License v3.0", "https://github.com/MorpheApp/morphe-patcher"),
    LicenseEntry("ReVanced Patches", "GNU General Public License v3.0", "https://github.com/revanced/revanced-patches"),
    LicenseEntry("DexKit", "LGPL-3.0 license, LuckyPray", "https://github.com/LuckyPray/DexKit"),
    LicenseEntry("Kotlin", "Apache License 2.0, Kotlin Team", "https://kotlinlang.org/"),
    LicenseEntry("Gson", "Apache License 2.0, Copyright 2008 Google Inc.", "https://github.com/google/gson/"),
    LicenseEntry("Fuel", "MIT License, Copyright (c) 2020 Kittinun Vantasin", "https://github.com/kittinunf/fuel"),
    LicenseEntry("LSPosed", "GNU General Public License v3.0, Copyright (C) 2021 LSPosed Contributors", "https://github.com/LSPosed/LSPosed"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleSettingsScreen(
    loading: Boolean,
    activated: Boolean,
    serviceAvailable: Boolean,
    hideIcon: Boolean,
    disableAutoCheck: Boolean,
    hideUnscopedApps: Boolean,
    scopedPackages: Set<String>,
    onHideIconChange: (Boolean) -> Unit,
    onHideUnscopedAppsChange: (Boolean) -> Unit,
    onDisableAutoCheckChange: (Boolean) -> Unit,
    onOpenApp: (AppPatchInfo) -> Unit,
    onAbout: () -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var showLicenses by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val visibleApps = remember(hideUnscopedApps, scopedPackages) {
        appPatchConfigurations.filter { info ->
            isPackageInstalled(context, info.packageName) &&
                (!hideUnscopedApps || scopedPackages.isEmpty() || info.packageName in scopedPackages)
        }
    }

    FemAlloyTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        if (loading) {
                            LoadingCard()
                        } else {
                            StatusCard(activated = activated)
                        }
                    }

                    if (activated) {
                        item {
                            Column {
                                SectionHeader(stringResource(R.string.settings_section_apps))
                                SettingsCard {
                                    SwitchRow(
                                        title = stringResource(R.string.settings_hide_unscoped_title),
                                        summary = stringResource(R.string.settings_hide_unscoped_summary),
                                        checked = hideUnscopedApps,
                                        onCheckedChange = onHideUnscopedAppsChange,
                                    )
                                    RowDivider()
                                    if (visibleApps.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.settings_no_apps),
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    } else {
                                        visibleApps.forEachIndexed { index, info ->
                                            SettingsRow(
                                                title = info.appName,
                                                summary = stringResource(
                                                    R.string.settings_patches_count,
                                                    info.patches.size,
                                                ),
                                                leading = {
                                                    AppIcon(info.packageName, info.appName)
                                                },
                                                trailing = { Chevron() },
                                                onClick = { onOpenApp(info) },
                                            )
                                            if (index != visibleApps.lastIndex) RowDivider()
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.force_stop_to_apply_summary),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    item {
                        Column {
                            SectionHeader(stringResource(R.string.settings_section_module))
                            SettingsCard {
                                SwitchRow(
                                    title = stringResource(R.string.hide_icon_title),
                                    summary = stringResource(R.string.hide_icon_summary),
                                    checked = hideIcon,
                                    onCheckedChange = onHideIconChange,
                                )
                                if (serviceAvailable) {
                                    RowDivider()
                                    SwitchRow(
                                        title = stringResource(R.string.disable_auto_check_update_title),
                                        checked = disableAutoCheck,
                                        onCheckedChange = onDisableAutoCheckChange,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            SectionHeader(stringResource(R.string.settings_section_about))
                            SettingsCard {
                                SettingsRow(
                                    title = stringResource(R.string.about_title),
                                    summary = stringResource(R.string.settings_about_morphe_summary),
                                    trailing = { Chevron() },
                                    onClick = onAbout,
                                )
                                RowDivider()
                                SettingsRow(
                                    title = stringResource(R.string.faq_title),
                                    trailing = { Chevron() },
                                    onClick = {
                                        onOpenUrl(
                                            "https://github.com/sheerboy/FemAlloy/wiki/Frequently-Asked-Questions",
                                        )
                                    },
                                )
                                RowDivider()
                                SettingsRow(
                                    title = stringResource(R.string.settings_morphe_website),
                                    summary = "https://morphe.software",
                                    trailing = { Chevron() },
                                    onClick = { onOpenUrl("https://morphe.software") },
                                )
                                RowDivider()
                                SettingsRow(
                                    title = stringResource(R.string.open_source_license),
                                    summary = stringResource(R.string.settings_licenses_summary),
                                    trailing = { Chevron() },
                                    onClick = { showLicenses = true },
                                )
                                RowDivider()
                                SettingsRow(
                                    title = stringResource(R.string.check_for_update_title),
                                    summary = versionSummary(),
                                    trailing = { Chevron() },
                                    onClick = onCheckUpdate,
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = stringResource(
                                R.string.settings_footer,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.COMMIT_HASH,
                                BuildConfig.BUILD_TYPE,
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (showLicenses) {
            AlertDialog(
                onDismissRequest = { showLicenses = false },
                title = { Text(stringResource(R.string.open_source_license)) },
                text = {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        itemsIndexed(licenses) { index, entry ->
                            SettingsRow(
                                title = entry.title,
                                summary = entry.summary,
                                onClick = { onOpenUrl(entry.url) },
                            )
                            if (index != licenses.lastIndex) RowDivider()
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLicenses = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.settings_connecting),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun StatusCard(activated: Boolean) {
    val container = if (activated) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (activated) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (activated) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(
                        text = stringResource(
                            if (activated) R.string.settings_status_active
                            else R.string.settings_status_inactive,
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = content,
                    )
                    Text(
                        text = stringResource(
                            if (activated) R.string.settings_status_active_summary
                            else R.string.module_not_activated_summary,
                        ),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = content.copy(alpha = 0.8f),
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = content.copy(alpha = 0.2f),
            )
            Text(
                text = stringResource(R.string.slogan_summary),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = content.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun versionSummary(): String {
    val buildDate = DateUtils.getRelativeTimeSpanString(BuildConfig.COMMIT_DATE * 1000)
    return stringResource(
        R.string.settings_version_summary,
        BuildConfig.VERSION_NAME,
        BuildConfig.COMMIT_HASH,
        BuildConfig.BUILD_TYPE,
        buildDate.toString(),
    )
}
