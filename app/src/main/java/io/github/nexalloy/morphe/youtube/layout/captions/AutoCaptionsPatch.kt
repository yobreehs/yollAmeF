package io.github.nexalloy.morphe.youtube.layout.captions

import app.morphe.extension.shared.Logger
import app.morphe.extension.youtube.patches.AutoCaptionsPatch
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_26_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.onCreateHook

val AutoCaptions = patch(
    name = "Auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
) {
    dependsOn(VersionCheck)

    PreferenceScreen.PLAYER.addPreferences(
        if (is_20_26_or_greater) {
            ListPreference("morphe_auto_captions_style")
        } else {
            ListPreference(
                key = "morphe_auto_captions_style",
                entriesKey = "morphe_auto_captions_style_legacy_entries",
                entryValuesKey = "morphe_auto_captions_style_legacy_entry_values"
            )
        }
    )

    // TODO disableAutoCaptions — SubtitleManagerFingerprint METHOD_MID

    onCreateHook.add { AutoCaptionsPatch.newVideoStarted(it) }

    StartVideoInformerFingerprint.hookMethod {
        before { AutoCaptionsPatch.videoInformationLoaded() }
    }

    // Disable mute auto captions feature flag.
    // 21.30.209: this flag method no longer exists (async refactor removed the
    // `()Z` getter). Resolve it opportunistically so a missing fingerprint only
    // skips the mute-captions hook instead of failing the whole patch.
    if (is_20_26_or_greater) {
        NoVolumeCaptionsFeatureFlagFingerprint.memberOrNull?.let { member ->
            member.hookMethod {
                before {
                    it.result = AutoCaptionsPatch.disableMuteAutoCaptions()
                }
            }
        } ?: Logger.printInfo { "NoVolumeCaptionsFeatureFlagFingerprint not found; skipping mute captions hook" }
    }
}
