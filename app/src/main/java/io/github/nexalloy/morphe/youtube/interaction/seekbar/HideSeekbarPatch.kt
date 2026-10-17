/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.interaction.seekbar

import app.morphe.extension.youtube.patches.HideSeekbarPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.layout.seekbar.seekbarColorPatch
import io.github.nexalloy.morphe.youtube.layout.sponsorblock.seekbarOnDrawFingerprint
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_28_or_greater
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_36_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val hideSeekbarPatch = patch(
    description = "Adds an option to hide the seekbar.",
) {
    dependsOn(seekbarColorPatch, VersionCheck)

    PreferenceScreen.SEEKBAR.addPreferences(
        SwitchPreference("morphe_hide_seekbar"),
        SwitchPreference("morphe_hide_seekbar_thumbnail", summary = true)
    )

    if (is_20_28_or_greater && !is_21_36_or_greater) {
        PreferenceScreen.SEEKBAR.addPreferences(
            SwitchPreference("morphe_fullscreen_large_seekbar")
        )

        insertLiteralOverride(45691569, HideSeekbarPatch::useFullscreenLargeSeekbar)
    }

    ::seekbarOnDrawFingerprint.hookMethod {
        before {
            if (HideSeekbarPatch.hideSeekbar()) it.result = null
        }
    }
}
