package io.github.nexalloy.morphe.youtube.video.quality

import app.morphe.extension.youtube.patches.playback.quality.RememberVideoQualityPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.shared.VideoQualityReceiver
import io.github.nexalloy.morphe.youtube.shared.videoQualityChangedFingerprint
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.VideoQualityProxy
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import `j$`.util.Optional

val RememberVideoQuality = patch {
    dependsOn(
        VideoInformationPatch,
        PlayerTypeHook,
    )

    settingsMenuVideoQualityGroup.addAll(
        listOf(
            ListPreference(
                key = "morphe_video_quality_default_mobile",
                entriesKey = "morphe_video_quality_default_entries",
                entryValuesKey = "morphe_video_quality_default_entry_values"
            ),
            ListPreference(
                key = "morphe_video_quality_default_wifi",
                entriesKey = "morphe_video_quality_default_entries",
                entryValuesKey = "morphe_video_quality_default_entry_values"
            ),
            SwitchPreference("morphe_remember_video_quality_last_selected", summary = true),

            ListPreference(
                key = "morphe_shorts_quality_default_mobile",
                entriesKey = "morphe_shorts_quality_default_entries",
                entryValuesKey = "morphe_shorts_quality_default_entry_values",
            ),
            ListPreference(
                key = "morphe_shorts_quality_default_wifi",
                entriesKey = "morphe_shorts_quality_default_entries",
                entryValuesKey = "morphe_shorts_quality_default_entry_values"
            ),
            SwitchPreference("morphe_remember_shorts_quality_last_selected", summary = true),
            SwitchPreference("morphe_remember_video_quality_last_selected_toast", summary = true)
        )
    )

    // Inject a call to override initial video quality.
    ::PlaybackStartParametersInit.hookMethod {
        val initialResolutionField = ::InitialResolutionField.field
        after {
            val oldValue = initialResolutionField.get(it.thisObject)
            val newValue = RememberVideoQualityPatch.getInitialVideoQuality(oldValue as Optional<*>?)
            initialResolutionField.set(it.thisObject, newValue)
        }
    }

    // Turn off the YouTube automatic quality selection when the video starts,
    // so the initial video quality is used as a fixed quality.
    insertLiteralOverride(45408049L, RememberVideoQualityPatch::useInitialVideoQuality)

    // Send the minimum and maximum quality with the SABR requests,
    // so the server cannot choose a quality outside the quality of the player.
    insertLiteralOverride(45738253L, RememberVideoQualityPatch::sendQualityConstraintToServer)

    // Use the 'Advanced' video quality setting instead of the 'Video quality preferences'
    // (Auto, Higher picture quality, Data saver), both on the device and with the SABR requests.
    val advancedMenu = ::advancedVideoQualitySettingField.field.get(null)
    ::getVideoQualitySettingMethod.hookMethod {
        before {
            if (RememberVideoQualityPatch.useAdvancedVideoQualitySetting()) {
                it.result = advancedMenu
            }
        }
    }

    // Inject a call to remember the selected quality for Shorts.
    ::videoQualityItemOnClickFingerprint.hookMethod {
        before { param ->
            RememberVideoQualityPatch.userChangedShortsQuality(param.args[2] as Int)
        }
    }

    // Inject a call to remember the user selected quality for regular videos.
    // The quality object is used, as its name is needed to get the fixed resolution.
    ::videoQualityChangedFingerprint.hookMethod(scopedHook(::VideoQualityReceiver.member) {
        before { param ->
            RememberVideoQualityPatch.userChangedQuality(VideoQualityProxy(param.args[0]))
        }
    })

    // If this flag is enabled, Shorts restart whenever the quality changes.
    // The restart uses the initial quality of the Short, so the quality selected by the user is lost.
    insertLiteralOverride(45387052)
    insertLiteralOverride(45399743)
}
