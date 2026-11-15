package io.github.nexalloy.morphe.youtube.shared

import io.github.nexalloy.morphe.youtube.layout.captions.StartVideoInformerFingerprint
import io.github.nexalloy.patch

private val videoStartedHooks = mutableListOf<() -> Unit>()

/**
 * Shared hook fired every time a video starts playing.
 *
 * Used by patches that need to reset their state between videos.
 */
val startVideoInformerPatch = patch(
    description = "Provides shared hooks for when a video starts playing."
) {
    StartVideoInformerFingerprint.hookMethod {
        before { videoStartedHooks.forEach { hook -> hook() } }
    }
}

fun hookVideoStarted(callback: () -> Unit) {
    videoStartedHooks.add(callback)
}
