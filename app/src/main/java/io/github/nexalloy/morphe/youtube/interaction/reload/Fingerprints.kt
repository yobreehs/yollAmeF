package io.github.nexalloy.morphe.youtube.interaction.reload

import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.RestrictQuery
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

private val miniAppOpenYtContentCommandClassFingerprint = Fingerprint(
    strings = listOf(
        "InvalidProtocolBufferException while decoding MiniAppWebToNativeParams for MiniAppOpenYTContentCommand: "
    )
)

/**
 * Finds the mini app command endpoint whose tail call is the player dismiss method.
 */
@RestrictQuery
internal object MiniAppOpenYtContentCommandEndpointFingerprint : Fingerprint(
    classFingerprint = miniAppOpenYtContentCommandClassFingerprint,
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        opcode(Opcode.CHECK_CAST),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            returnType = "V",
            parameters = emptyList(),
            location = InstructionLocation.MatchAfterImmediately(),
        ),
    ),
    strings = listOf("no error message"),
)

/**
 * The obfuscated no-arg method that closes the current player instance.
 */
val dismissPlayerMethod = findMethodDirect {
    MiniAppOpenYtContentCommandEndpointFingerprint
        .instructionMatches.last()
        .instruction.methodRef!!
}
