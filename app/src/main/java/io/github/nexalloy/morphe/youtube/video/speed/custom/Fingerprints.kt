package io.github.nexalloy.morphe.youtube.video.speed.custom

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.parameters
import io.github.nexalloy.morphe.returns
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.youtube.shared.SpeedLimiterFingerprint

internal object SpeedArrayGeneratorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "[L",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(
            name = "size",
            returnType = "I"
        ),
        newInstance("Ljava/text/DecimalFormat;"),
        string("0.0#"),
        literal(7),
        opcode(Opcode.NEW_ARRAY),
        fieldAccess(
            opcode = Opcode.SGET_OBJECT,
            type = "[F"
        )
    )
)

// found in com.google.android.libraries.youtube.innertube.model.media.PlayerConfigModel
val speedsFloatArrayField = findFieldDirect {
    SpeedArrayGeneratorFingerprint.instructionMatches.last().instruction.fieldRef!!
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

internal object TapAndHoldSpeedFingerprint : Fingerprint(
    name = "run",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Landroid/os/Handler;"
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V"
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf(),
            returnType = "Z"
        ),
        opcode(Opcode.IF_EQZ),
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            type = "Z"
        ),
        opcode(Opcode.IF_NEZ),
        opcode(Opcode.IGET_OBJECT),
        literal(2.0f)
    )
)

/**
 * Checks if the audio offload supports changing the playback speed.
 */
internal object AudioOffloadVariableRateSupportedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("offloadVariableRateSupported")
    )
)

/**
 * Checks if the audio offload is enabled.
 */
internal object AudioOffloadEnabledFingerprint : Fingerprint(
    classFingerprint = AudioOffloadVariableRateSupportedFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_BOOLEAN,
        Opcode.CONST_4,
        Opcode.IF_NEZ,
        Opcode.IGET_BOOLEAN,
        Opcode.CONST_4,
        Opcode.IF_NEZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_BOOLEAN
    )
)
