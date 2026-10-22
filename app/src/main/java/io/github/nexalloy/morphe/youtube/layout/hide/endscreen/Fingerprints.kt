package io.github.nexalloy.morphe.youtube.layout.hide.endscreen

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral

/**
 * The end screen is rendered by two independent mechanisms:
 *
 * 1. The creator end screen cards (one video card per creator-placed element, plus the channel
 *    avatar/icons). They are created by [EndScreenLayout*Fingerprint] and the whole group is
 *    shown/hidden by [ShowEndScreenCardsFingerprint].
 * 2. The "up next"/autonav preview, which is laid out by the method found through
 *    [endScreenSuggestedVideoMethod].
 *
 * Both are hooked by the patch, so no runtime Litho identifier has to be known.
 */

internal object AutoNavConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    strings = listOf("main_app_autonav")
)

/**
 * `boolean isAutonavEnabled()` of the autonav controller.
 */
internal object AutoNavStatusFingerprint : Fingerprint(
    classFingerprint = AutoNavConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf()
)

/**
 * Player lifecycle method that detaches from the overlays layout. Used only as an anchor to
 * resolve the end screen method below (same approach as the upstream patcher).
 */
internal object RemoveOnLayoutChangeListenerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT,
            type = "I"
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf(),
            returnType = "V",
            location = MatchAfterWithin(3)
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = "Lcom/google/android/apps/youtube/app/common/player/overlay/YouTubePlayerOverlaysLayout;",
            name = "removeOnLayoutChangeListener",
            returnType = "V"
        )
    )
)

/**
 * The no-arg void method that lays out the end-of-video suggested video ("up next" preview).
 * Skipping it leaves the player at the last frame without the preview.
 */
val endScreenSuggestedVideoMethod = findMethodDirect {
    val endScreenMethod = RemoveOnLayoutChangeListenerFingerprint.instructionMatches[1]
        .instruction.methodRef!!
    val autoNavStatusMethod = AutoNavStatusFingerprint.run()

    Fingerprint(
        definingClass = endScreenMethod.declaredClass!!.descriptor,
        name = endScreenMethod.name,
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
        returnType = "V",
        parameters = listOf(),
        filters = listOf(
            fieldAccess(
                opcode = Opcode.IGET_OBJECT,
                definingClass = "this",
                type = autoNavStatusMethod.declaredClass!!.descriptor
            ),
            methodCall(
                opcode = Opcode.INVOKE_VIRTUAL,
                smali = autoNavStatusMethod.descriptor,
                location = MatchAfterWithin(3)
            )
        )
    ).run()
}

internal object EndScreenLayoutCircleFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Landroid/view/View;",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "endscreen_element_layout_circle"),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterWithin(10)),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, location = MatchAfterImmediately())
    )
)

internal object EndScreenLayoutIconFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "Landroid/view/View;",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "endscreen_element_layout_icon"),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterWithin(10)),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, location = MatchAfterImmediately())
    )
)

internal object EndScreenLayoutVideoFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    returnType = "Landroid/view/View;",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "endscreen_element_layout_video"),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterWithin(10)),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, location = MatchAfterImmediately())
    )
)

/**
 * Shows the creator end screen cards group. Skipping it hides all of them at once.
 *
 * The class holds exactly five methods (upstream patcher uses the same constraint), which
 * disambiguates it from the many other `void f(String)` methods in the app.
 */
internal object ShowEndScreenCardsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Ljava/lang/String;"
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/String;",
            location = MatchAfterImmediately()
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "ordinal",
            location = MatchAfterWithin(7)
        ),
        literal(5),
        literal(8),
        literal(9)
    )
) {
    init {
        classMatcher {
            methodCount(5)
            // The real class holds a single field; synthetic lambda classes that also
            // match the filters have several fields and a synthetic method
            // (0x1011 modifiers), so they are excluded by this.
            fieldCount(1)
        }
    }
}
