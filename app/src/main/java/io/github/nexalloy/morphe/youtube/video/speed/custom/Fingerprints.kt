package io.github.nexalloy.morphe.youtube.video.speed.custom

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.fingerprint
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.returns
import io.github.nexalloy.morphe.youtube.shared.SpeedLimiterFingerprint

internal val speedArrayGeneratorFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("[L")
    parameters("L")
    strings("0.0#")
}

// found in com.google.android.libraries.youtube.innertube.model.media.PlayerConfigModel
val speedsFloatArrayField = findFieldDirect {
    speedArrayGeneratorFingerprint().usingFields.single {
        it.field.typeSign == "[F"
    }.field
}

@RequireAppVersion("20.34.00")
internal object ServerSideMaxSpeedFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = listOf(
        literal(45719140L)
    )
)

val clampFloatFingerprint = findMethodDirect {
    SpeedLimiterFingerprint().invokes.findMethod {
        matcher {
            parameters("F", "F", "F")
            returns("F")
        }
    }.single()
}
