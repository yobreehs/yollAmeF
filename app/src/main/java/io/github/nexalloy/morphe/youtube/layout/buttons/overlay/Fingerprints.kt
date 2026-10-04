package io.github.nexalloy.morphe.youtube.layout.buttons.overlay

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.resourceLiteral

/**
 * The controller that updates the captions button state. The captions button (a
 * TouchImageView) is read via an instance field at the start of this method; there is no
 * stable resource id for it on 21.39, so the field is used instead.
 */
internal object SubtitleButtonControllerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Lcom/google/android/libraries/youtube/common/ui/TouchImageView;",
        ),
        resourceLiteral(ResourceType.STRING, "accessibility_captions_unavailable"),
        resourceLiteral(ResourceType.STRING, "accessibility_captions_button_name"),
    ),
)

/**
 * The captions button ImageView instance field read by [SubtitleButtonControllerFingerprint].
 */
val captionsButtonImageField = findFieldDirect {
    val method = SubtitleButtonControllerFingerprint.run()
    val index = SubtitleButtonControllerFingerprint.instructionMatches.first().index
    method.instructions[index].fieldRef ?: error("Captions button image field not found")
}