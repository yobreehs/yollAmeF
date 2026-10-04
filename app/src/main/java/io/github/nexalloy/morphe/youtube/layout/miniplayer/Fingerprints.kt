package io.github.nexalloy.morphe.youtube.layout.miniplayer

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.RestrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.opcode

/**
 * Draggable miniplayer rect getter, used as the class holder for the horizontal drag hooks.
 */
internal object MiniplayerRectDragFieldsNameFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/graphics/Rect;",
    parameters = listOf("I", "I"),
    filters = listOf(
        opcode(Opcode.IF_GEZ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Landroid/graphics/Rect;",
            location = MatchAfterImmediately(),
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "width",
            returnType = "I",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.NEG_INT, location = MatchAfterImmediately()),
        opcode(Opcode.GOTO, location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IGET,
            type = "I",
            location = MatchAfterImmediately(),
        ),
    ),
)

/**
 * The method that repositions the miniplayer with the dragged rect.
 */
internal object MiniplayerHorizontalRepositionFingerprint : Fingerprint(
    classFingerprint = MiniplayerRectDragFieldsNameFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/graphics/Rect;"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET,
            definingClass = "Landroid/graphics/Rect;",
            name = "left",
        ),
    ),
)

/**
 * Offscreen miniplayer bounds handler.
 */
internal object MiniplayerOffscreenHandlerFingerprint : Fingerprint(
    classFingerprint = MiniplayerRectDragFieldsNameFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "I", "I", "I"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/graphics/Rect;"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "set",
            parameters = listOf("I", "I", "I", "I"),
            returnType = "V",
        ),
    ),
)

/**
 * Offscreen rect validator used when dragging the miniplayer.
 */
internal object MiniplayerOffscreenRectValidatorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("I", "I", "I"),
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Landroid/graphics/Rect;",
            location = MatchAfterImmediately(),
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "centerX",
            returnType = "I",
            location = MatchAfterImmediately(),
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "centerY",
            returnType = "I",
            location = MatchAfterWithin(3),
        ),
        newInstance(type = "Landroid/graphics/Point;", location = MatchAfterWithin(3)),
    ),
)

/**
 * The watch layout that intercepts touch events to detect the offscreen miniplayer button press.
 */
internal object NextGenWatchLayoutOnInterceptTouchEventFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/watch/nextgenwatch/ui/NextGenWatchLayout;",
    name = "onInterceptTouchEvent",
    parameters = listOf("Landroid/view/MotionEvent;"),
)

/**
 * The previous drag rect field of [MiniplayerRectDragFieldsNameFingerprint], read to block
 * offscreen repositioning.
 */
val miniplayerPreviousRectField = findFieldDirect {
    val match = MiniplayerRectDragFieldsNameFingerprint.instructionMatches[1]
    match.instruction.fieldRef ?: error("Miniplayer previous rect field not found")
}