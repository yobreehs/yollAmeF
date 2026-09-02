package io.github.nexalloy.morphe.youtube.video.audio

import io.github.nexalloy.morphe.shared.misc.audio.tracks.forceOriginalAudioPatch
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_26_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.YouTubeActivityOnCreateFingerprint

val ForceOriginalAudio = forceOriginalAudioPatch(
    block =  {
        dependsOn(
            VersionCheck
        )
    },
    // Localized audio track flag was removed in 21.26+ but might be replaced with 45673827L
    fixUseLocalizedAudioTrackFlag = { !is_21_26_or_greater },
    forcedServerAdaptiveStreaming = { is_21_26_or_greater },
    mainActivityOnCreateFingerprint = YouTubeActivityOnCreateFingerprint,
    subclassExtensionClassDescriptor = "Lapp/morphe/extension/youtube/patches/ForceOriginalAudioPatch;",
    preferenceScreen = PreferenceScreen.VIDEO,
)
