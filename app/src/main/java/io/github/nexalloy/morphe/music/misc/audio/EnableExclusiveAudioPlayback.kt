package io.github.nexalloy.morphe.music.misc.audio

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.morphe.music.misc.playservice.is_9_32_or_greater
import io.github.nexalloy.morphe.music.misc.playservice.versionCheckPatch
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val EnableExclusiveAudioPlayback = patch(
    name = "Enable exclusive audio playback",
    description = "Enables the option to play audio without video.",
) {
    dependsOn(
        versionCheckPatch
    )
    val fingerprint = if (is_9_32_or_greater) {
        AllowExclusiveAudioPlaybackFingerprint
    } else {
        AllowExclusiveAudioPlaybackLegacyFingerprint
    }

    fingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))

    // YTM tries to skip podcast episodes marked FEATURE_AVAILABILITY_BLOCKED ("Audio-only not available")
    // and stops playback if the skip request returns "action unavailable" (11). Bypass that restriction
    // so playback can start from Android Auto without pressing Play on the phone.
    // The phone's Audio/Video switch also uses this restriction check, so force false only here.
    ExternalAudioRestrictionFingerprint.hookMethod(scopedHook(::externalAudioRestrictionMethod.method) {
        after { it.result = false }
    })
}
