package io.github.nexalloy.morphe.youtube.interaction.seekbar

import app.morphe.extension.youtube.patches.DisablePreciseSeekingGesturePatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val disablePreciseSeekingGesturePatch = patch(
    description = "Adds an option to disable precise seeking when swiping up on the seekbar.",
) {
    PreferenceScreen.SEEKBAR.addPreferences(
        SwitchPreference("morphe_disable_precise_seeking_gesture", summary = true),
    )

    AllowSwipingUpGestureFingerprint.hookMethod {
        before {
            if (DisablePreciseSeekingGesturePatch.isGestureDisabled()) it.result = null
        }
    }

    ShowSwipingUpGuideFingerprint.hookMethod {
        before {
            if (DisablePreciseSeekingGesturePatch.isGestureDisabled()) it.result = false
        }
    }
}
