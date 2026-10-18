package io.github.nexalloy.morphe.youtube.interaction.seekbar

import io.github.nexalloy.patch

@Suppress("unused")
val seekbarPatch = patch(
    name = "Seekbar",
    description = "Adds options to show old seekbar thumbnails, " +
            "disable precise seeking when swiping up on the seekbar, " +
            "slide to seek instead of playing at 2x speed when pressing and holding, " +
            "tapping the player seekbar to seek, " +
            "hiding the video player seekbar, " +
            "enabling seeking in live streams, " +
            "and expanding the live stream DVR duration."
) {
    dependsOn(
        disablePreciseSeekingGesturePatch,
        enableSlideToSeekPatch,
        enableTapToSeekPatch,
        hideSeekbarPatch,
//        liveStreamDVRPatch,
        seekbarThumbnailPreviewPatch
    )
}
