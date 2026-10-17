/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.interaction.seekbar

import app.morphe.extension.youtube.patches.LiveStreamDVRPatch
import app.morphe.extension.youtube.settings.preference.LiveStreamDVRDurationPreference
import app.morphe.extension.youtube.settings.preference.LiveStreamDVRPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

@Suppress("unused")
val liveStreamDVRPatch = patch(
    description = "Enables video seeking on live streams that have disabled DVR (Digital Video Recorder).",
) {
    PreferenceScreen.SEEKBAR.addPreferences(
        noTitleUnsortedPreferenceCategory(
            SwitchPreference(
                key = "morphe_live_stream_dvr",
                summary = true,
                tag = LiveStreamDVRPreference::class.java
            ),
            SwitchPreference(
                key = "morphe_expand_live_stream_dvr_duration",
                summary = true,
                tag = LiveStreamDVRDurationPreference::class.java
            )
        )
    )

    VideoStreamingDataAllowSeekingFingerprint.hookMethod {
        after { it.result = LiveStreamDVRPatch.enableLiveStreamDVR(it.result as Boolean) }
    }

    FormatStreamModelMaxDVRDurationFingerprint.hookMethod {
        after { it.result = LiveStreamDVRPatch.overrideMaxDVRDurationSeconds(it.result as Double) }
    }
}
