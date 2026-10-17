package io.github.nexalloy.morphe.youtube.shared

import io.github.nexalloy.patch

private val videoStartedHooks = mutableListOf<() -> Unit>()

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
