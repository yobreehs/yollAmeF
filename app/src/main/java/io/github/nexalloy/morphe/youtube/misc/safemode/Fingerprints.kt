package io.github.nexalloy.morphe.youtube.misc.safemode

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.string

/**
 * Matches `com.google.android.libraries.youtube.innertube.model.SafetyMode#isEnabled()`.
 * The class is obfuscated on current YouTube versions, but the method logs
 * 'SafetyMode.java' / 'Failed to read safemode' / 'isEnabled', so those strings pin it down.
 */
internal object SafetyModeIsEnabledFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("Failed to read safemode"),
        string("SafetyMode.java"),
        string("isEnabled"),
    )
)