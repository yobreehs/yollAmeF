package io.github.nexalloy.morphe.shared.layout.returnyoutubedislike

import com.facebook.litho.ComponentHost
import io.github.nexalloy.PatchExecutor

/**
 * Draws the like and dislike counts over the action bar buttons, which YouTube and YouTube Music
 * lay out with the same Litho components.
 */
internal fun PatchExecutor.hookLikeDislikeButtons(
    onComponentHostContentDescription: (ComponentHost, CharSequence?) -> CharSequence?,
    onYogaSetWidth: (Long, Float) -> Unit
) {
    // The compact action bar has icon only like and dislike buttons with no text to hook,
    // so the counts are drawn over the button host views, which are found by their view tag.
    ComponentHostSetContentDescriptionFingerprint.hookMethod {
        before {
            it.args[0] = onComponentHostContentDescription(it.thisObject as ComponentHost, it.args[0] as? CharSequence)
        }
    }

    // The dislike icon is given a margin so the button wrapping it grows and leaves room for
    // the count. A width is no use, since the layout stretches the icon over whatever it gets.
    //
    // The only long of the node is the pointer to its native counterpart.
    YogaSetWidthFingerprint.hookMethod {
        val pointerField = ::yogaNodePointerField.field
        before { onYogaSetWidth(pointerField.getLong(it.thisObject), it.args[0] as Float) }
    }
}
