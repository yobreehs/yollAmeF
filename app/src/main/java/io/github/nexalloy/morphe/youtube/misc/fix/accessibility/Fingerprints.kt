package io.github.nexalloy.morphe.youtube.misc.fix.accessibility

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral

@RequireAppVersion(maxVersion = "21.23.000")
internal object CaptionTextViewConstructorFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "subtitle_window_identifier"),
        opcode(Opcode.RETURN_VOID)
    )
) {
    init {
        classMatcher { superClass("android.view.View") }
    }
}

@RequireAppVersion("20.51.000", "21.42.000")
internal object TouchDelegateConstructorFingerprint : Fingerprint(
    name = "<init>",
    parameters = listOf("Lcom/facebook/litho/ComponentHost;")
) {
    init {
        classMatcher { superClass("android.view.TouchDelegate") }
    }
}

/**
 * 21.30+
 */
@RequireAppVersion("21.30.000", "21.42.000")
internal object TouchExplorationHoverEventFingerprint : Fingerprint(
    classFingerprint = TouchDelegateConstructorFingerprint,
    name = "onTouchExplorationHoverEvent",
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;")
)
