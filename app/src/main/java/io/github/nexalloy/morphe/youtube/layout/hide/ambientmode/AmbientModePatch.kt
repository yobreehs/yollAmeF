package io.github.nexalloy.morphe.youtube.layout.hide.ambientmode

import android.os.PowerManager
import android.view.View
import app.morphe.extension.youtube.patches.AmbientModePatch as ExtensionAmbientModePatch
import app.morphe.extension.youtube.settings.Settings
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

/**
 * Adds options to bypass power saving restrictions for Ambient mode and disable it entirely
 * or in fullscreen.
 *
 * Ported from Morphe v1.45.0 `ambientModePatch`:
 *  - "Disable Ambient mode" forces the ambient experiment flag (45376186) off;
 *  - "Bypass Ambient mode restrictions" makes power save mode reports false, which is what
 *    powers the ambient gating;
 *  - "Disable Ambient mode in fullscreen" restores an opaque background after the player view
 *    lays out, reverting YouTube's fullscreen dim.
 */
val AmbientMode = patch(
    name = "Ambient mode",
    description = "Adds options to bypass power saving restrictions for Ambient mode and disable it entirely or in fullscreen.",
) {
    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_ambient_mode_screen",
            preferences = setOf(
                SwitchPreference("morphe_bypass_ambient_mode_restrictions", summary = true),
                SwitchPreference("morphe_disable_ambient_mode"),
                SwitchPreference("morphe_disable_fullscreen_ambient_mode"),
            ),
        ),
    )

    // Disable Ambient mode entirely via the experiment flag read through the standard getter.
    insertLiteralOverride(45376186L) { original ->
        ExtensionAmbientModePatch.disableAmbientMode(original)
    }

    // Bypass Ambient mode restrictions: while the setting is on, pretend the device is not in
    // power save mode (power save is exactly what gates Ambient mode off by default).
    DexMethod("Landroid/os/PowerManager;->isPowerSaveMode()Z").hookMethod {
        before { param ->
            if (Settings.BYPASS_AMBIENT_MODE_RESTRICTIONS.get()) {
                param.result = null
            }
        }
    }

    // Disable the fullscreen dim by restoring an opaque background after the player lays out.
    SetFullScreenBackgroundColorFingerprint.hookMethod {
        after { param ->
            val view = param.thisObject as? View ?: return@after
            val color = ExtensionAmbientModePatch.getFullScreenBackgroundColor(0)
            if (color != 0) {
                view.setBackgroundColor(color)
            }
        }
    }
}