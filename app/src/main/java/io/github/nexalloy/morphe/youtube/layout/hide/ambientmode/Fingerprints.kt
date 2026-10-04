package io.github.nexalloy.morphe.youtube.layout.hide.ambientmode

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

/**
 * The (non-obfuscated) YouTube player view whose onLayout applies the fullscreen
 * Ambient mode dimming.
 */
internal object SetFullScreenBackgroundColorFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/player/YouTubePlayerViewNotForReflection;",
    name = "onLayout",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "I", "I", "I", "I"),
)