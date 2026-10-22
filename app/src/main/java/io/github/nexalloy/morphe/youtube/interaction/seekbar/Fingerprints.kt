package io.github.nexalloy.morphe.youtube.interaction.seekbar

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.RestrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.shared.FormatStreamModelToStringFingerprint
import io.github.nexalloy.morphe.youtube.shared.VideoStreamingDataToStringFingerprint
import org.luckypray.dexkit.query.enums.MatchType

private object SwipingUpGestureParentFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45379021) // Swipe up fullscreen feature flag
    )
)

internal object ShowSwipingUpGuideFingerprint : Fingerprint(
    classFingerprint = SwipingUpGestureParentFingerprint,
    accessFlags = listOf(AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(1)
    ),
    custom = { modifiers(AccessFlags.FINAL.modifier, MatchType.Equals) }
)

internal object AllowSwipingUpGestureFingerprint : Fingerprint(
    classFingerprint = SwipingUpGestureParentFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    custom = { protoShorty = "VL" }
)

internal object DisableFastForwardGestureFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/watch/nextgenwatch/ui/NextGenWatchLayout;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
    ),
    custom = {
        opCodesMatcher!!.size(31..Int.MAX_VALUE)
    }
)

internal object OnTouchEventHandlerFingerprint : Fingerprint(
    name = "onTouchEvent",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = listOf("L"),
    filters = listOf(
        opcode(Opcode.INVOKE_VIRTUAL), // nMethodReference
        // Skip over a return (21.41 and lower) or goto (21.42+).
        opcode(Opcode.IGET_OBJECT, location = MatchAfterWithin(1)),
        opcode(Opcode.IGET_BOOLEAN, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        opcode(Opcode.INT_TO_FLOAT, location = MatchAfterWithin(1)),
        opcode(Opcode.INT_TO_FLOAT, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()), // oMethodReference
    )
)

@RestrictQuery
internal object TapToSeekFingerprint : Fingerprint(
    name = "onTouchEvent",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;"),
    filters = listOf(
        literal(Int.MAX_VALUE),

        newInstance("Landroid/graphics/Point;"),
        methodCall(
            smali = "Landroid/graphics/Point;-><init>(II)V",
            location = MatchAfterImmediately()
        ),
        methodCall(
            smali = "Lj$/util/Optional;->of(Ljava/lang/Object;)Lj$/util/Optional;",
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Lj$/util/Optional;",
            location = MatchAfterImmediately()
        ),

        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterWithin(10))
    )
)

@RestrictQuery
internal object SlideToSeekFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "F"),
    filters = listOf(
        methodCall(
            returnType = "Z",
            parameters = listOf(),
            location = MatchAfterWithin(10) // Match close to start of method.
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        literal(67108864)
    )
)

@RestrictQuery
internal object VideoStreamingDataAllowSeekingFingerprint : Fingerprint(
    classFingerprint = VideoStreamingDataToStringFingerprint,
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(8),
        opcode(Opcode.IF_EQ, location = MatchAfterImmediately()),
        // Another method in the same class almost matches this fingerprint but uses literal(0) here.
        literal(1, location = MatchAfterImmediately()),
    )
)

// DVR window duration in seconds; 0 for non-DVR streams.
// Caller multiplies result by 1e6 with 4-hour fallback when <= 0, logs "windowMaxMediaTimeUs".
@RestrictQuery
internal object FormatStreamModelMaxDVRDurationFingerprint : Fingerprint(
    classFingerprint = FormatStreamModelToStringFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "D",
    parameters = listOf(),
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        fieldAccess(opcode = Opcode.IGET_WIDE, type = "D", location = MatchAfterImmediately()),
        opcode(Opcode.RETURN_WIDE, location = MatchAfterImmediately()),
    )
)

val tapSeekStartMethod = findMethodDirect {
    OnTouchEventHandlerFingerprint.instructionMatches.first().instruction.methodRef!!
}

val tapSeekUpdateMethod = findMethodDirect {
    OnTouchEventHandlerFingerprint.instructionMatches.last().instruction.methodRef!!
}

val tapSeekTouchDownMethod = findMethodDirect {
    TapToSeekFingerprint.instructionMatches.last().instruction.methodRef!!
}

val tapSeekPointField = findFieldDirect {
    TapToSeekFingerprint.instructionMatches[5].instruction.fieldRef!!
}

val slideToSeekCheckMethod = findMethodDirect {
    SlideToSeekFingerprint.instructionMatches.first().instruction.methodRef!!
}

val fastForwardGestureCheckMethod = findMethodDirect {
    DisableFastForwardGestureFingerprint.instructionMatches[1].instruction.methodRef!!
}

val slideToSeekCheckCallers = findMethodListDirect {
    val checkMethod = slideToSeekCheckMethod()
    SlideToSeekFingerprint().declaredClass!!.methods.filter { method ->
        method.invokes.any { it.descriptor == checkMethod.descriptor }
    }.also { require(it.isNotEmpty()) }
}
