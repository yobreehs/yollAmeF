package io.github.nexalloy.morphe.youtube.interaction.seekbar

import io.github.nexalloy.patch

/**
 * Seekbar related options. The individual toggles are registered into
 * [io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen.SEEKBAR].
 */
val seekbarPatch = patch(
    name = "Seekbar",
    description = "Adds options to disable precise seeking when swiping up on the seekbar, " +
            "tap the seekbar to seek, slide to seek instead of playing at 2x speed, " +
            "and enable seeking on live streams without DVR.",
) {
    dependsOn(
        disablePreciseSeekingGesturePatch,
        enableSlideToSeekPatch,
        enableTapToSeekPatch,
        liveStreamDVRPatch,
    )
}
