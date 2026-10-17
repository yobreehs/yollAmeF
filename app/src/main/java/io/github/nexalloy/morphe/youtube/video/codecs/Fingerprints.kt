package io.github.nexalloy.morphe.youtube.video.codecs

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

internal object Vp9CapabilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    strings = listOf(
        "vp9_supported",
        "video/x-vnd.on2.vp9"
    )
)

/**
 * Codec support check in the class of [Vp9CapabilityFingerprint]. The second parameter is the mime type.
 */
internal object CodecSupportCheckFingerprint : Fingerprint(
    classFingerprint = Vp9CapabilityFingerprint,
    returnType = "Z",
    parameters = listOf(
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Ljava/util/Set;",
        "Ljava/util/Set;",
        "L",
        "I"
    )
)
