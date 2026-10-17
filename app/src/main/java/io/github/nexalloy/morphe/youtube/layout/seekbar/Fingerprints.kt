package io.github.nexalloy.morphe.youtube.layout.seekbar

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import java.lang.reflect.Modifier

@StrictQuery
internal object WatchHistoryMenuUseProgressDrawableFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(definingClass = "Landroid/widget/ProgressBar;", name = "setMax"),
        opcode(Opcode.MOVE_RESULT),
        literal(-1712394514)
    )
)

internal object LithoLinearGradientFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC),
    returnType = "Landroid/graphics/LinearGradient;",
    parameters = listOf("F", "F", "F", "F", "[I", "[F"),
)

/**
 * 19.49+
 */
internal object PlayerLinearGradientFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("I", "I", "I", "I", "Landroid/content/Context;", "I"),
    returnType = "Landroid/graphics/LinearGradient;",
    filters = listOf(
        resourceLiteral(ResourceType.COLOR, "yt_youtube_magenta"),
        opcode(Opcode.FILLED_NEW_ARRAY, location = MatchAfterWithin(5)),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately())
    )
)

val watchHistoryProgressDrawableCheckMethods = findMethodListDirect {
    val match = WatchHistoryMenuUseProgressDrawableFingerprint.match()
    val reference = match.method.instructions[match.instructionMatches!![1].index - 1].methodRef!!
    findClass {
        matcher { addInterface(reference.declaredClass!!.name) }
    }.flatMap { it.methods }.filter {
        it.name == reference.name && it.paramCount == 0 && it.returnTypeName == "boolean" &&
                !Modifier.isAbstract(it.modifiers)
    }.also { require(it.isNotEmpty()) }
}
