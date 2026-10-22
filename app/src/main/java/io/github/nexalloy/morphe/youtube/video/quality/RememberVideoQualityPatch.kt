package io.github.nexalloy.morphe.youtube.video.quality

import `j$`.util.Optional
import app.morphe.extension.youtube.patches.playback.quality.RememberVideoQualityPatch
import io.github.nexalloy.callMethod
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.interaction.downloads.GeneratedRegistryFingerprint
import io.github.nexalloy.morphe.youtube.interaction.downloads.ProtobufClassParseByteArrayWithRegistryFingerprint
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.shared.VideoQualityChangedFingerprint
import io.github.nexalloy.morphe.youtube.shared.VideoQualityReceiver
import io.github.nexalloy.morphe.youtube.shared.hookVideoIntent
import io.github.nexalloy.morphe.youtube.shared.openVideoIntentPatch
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.VideoQualityProxy
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val RememberVideoQuality = patch {
    dependsOn(
        VideoInformationPatch,
        PlayerTypeHook,
        openVideoIntentPatch,
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

    // The server chooses the quality of SABR streams from the client ABR state sent with each request.
    // The first request of a video sends the initial video quality as the sticky resolution,
    // but the requests sent while the video plays have none, so the server can switch to another quality.
    // The quality is sent as if the user just selected it, in every request.
    val parseFrom = ProtobufClassParseByteArrayWithRegistryFingerprint.method
    val registry = GeneratedRegistryFingerprint.method.invoke(null)
    ClientAbrStateFingerprint.hookMethod {
        after {
            val state = it.result ?: return@after
            val bytes = RememberVideoQualityPatch.overrideClientAbrStateBytes(
                state.callMethod("toByteArray") as ByteArray
            ) ?: return@after
            val defaultInstance = state.javaClass.getMethod("getDefaultInstance").invoke(null)
            it.result = parseFrom.invoke(null, defaultInstance, bytes, registry)
        }
    }

    // The initial video quality is only used when the video starts. While the video plays,
    // the player and the SABR requests use the quality last selected in the quality menu,
    // and without one the quality is chosen automatically, such as 720p for the size of the player.
    // The default quality is used as the quality last selected, as if the user just selected it.
    val (selectedTime, videoId, playbackStartTime, resolution, direction) = ::lastSelectedVideoQualityFields.fieldList
    LastSelectedVideoQualityConstructorFingerprint.hookMethod {
        after {
            val quality = RememberVideoQualityPatch.getLastSelectedVideoQuality()
            if (quality >= 0 && !it.hasThrowable()) {
                resolution.setInt(it.thisObject, quality)
                direction.setInt(it.thisObject, 0)
                val now = System.currentTimeMillis()
                selectedTime.setLong(it.thisObject, now)
                playbackStartTime.setLong(it.thisObject, now)
                if (videoId.get(it.thisObject) == null) videoId.set(it.thisObject, "")
            }
        }
    }

    // With some accounts, the quality selected in the quality menu is used only for the video
    // it was selected for, and the player of other videos chooses the quality automatically.
    // The default quality is used as if the user selected it for the current video.
    ::getVideoQualityConstraintMethod.hookMethod(scopedHook(::lastSelectedVideoQualityGetter.method) {
        after {
            if (innerDepth == 0 && RememberVideoQualityPatch.getLastSelectedVideoQuality() >= 0 && it.result != null) {
                videoId.set(it.result, outerParam.args[2])
            }
        }
    })

    // A quality selected by the user is used again if YouTube reloads the video,
    // until another video is opened.
    hookVideoIntent { map, videoId -> RememberVideoQualityPatch.onVideoIntentLoaded(map, videoId) }

    // Inject a call to remember the selected quality for Shorts.
    VideoQualityItemOnClickFingerprint.hookMethod {
        before { param ->
            RememberVideoQualityPatch.userChangedShortsQuality(param.args[2] as Int)
        }
    }

    // Inject a call to remember the user selected quality for regular videos.
    // The quality object is used, as its name is needed to get the fixed resolution.
    VideoQualityChangedFingerprint.hookMethod(scopedHook(::VideoQualityReceiver.member) {
        before { param ->
            RememberVideoQualityPatch.userChangedQuality(VideoQualityProxy(param.args[0]))
        }
    })

    // If this flag is enabled, Shorts restart whenever the quality changes.
    // The restart uses the initial quality of the Short, so the quality selected by the user is lost.
    insertLiteralOverride(45387052)
    insertLiteralOverride(45399743)
}
