package io.github.nexalloy.morphe.youtube.misc.safemode

import app.morphe.extension.shared.settings.Setting
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

/**
 * Disables the app's Safety Mode (Restricted Mode) when the setting is enabled.
 *
 * The toggle lives in the in-app Morphe settings → "New features" screen and is stored
 * in the target app's `morphe_prefs` (the same file the in-app preference manager uses),
 * so it works in-process without the module prefs round trip.
 *
 * Hooks `SafetyMode#isEnabled()` to return false, so comments and content are no longer
 * gated by Safety Mode. Requires an app restart after toggling.
 */
val DisableSafeMode = patch(
    name = "Disable safe mode",
    description = "Disables the app's Safety Mode (Restricted Mode) so comments and content are not hidden.",
) {
    PreferenceScreen.NEW_PATCHES.addPreferences(
        SwitchPreference(
            key = "morphe_disable_safe_mode",
            summary = true,
        )
    )

    if (Setting.preferences.getBoolean("morphe_disable_safe_mode", false)) {
        app.morphe.extension.shared.Logger.printDebug { "Disabling Safety Mode (SafetyMode.isEnabled -> false)" }
        SafetyModeIsEnabledFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    }
}