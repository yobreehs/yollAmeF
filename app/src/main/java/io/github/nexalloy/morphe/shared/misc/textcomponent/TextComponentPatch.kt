package io.github.nexalloy.morphe.shared.misc.textcomponent

import io.github.nexalloy.morphe.shared.SpannableStringBuilderFingerprint
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContext
import io.github.nexalloy.morphe.shared.spannableStringBuilderGetSpannedMethod
import io.github.nexalloy.patch

val textComponentPatch = patch(
    description = "Provides hooks into text components for extension filtering."
) {
    SpannableStringBuilderFingerprint.hookMethod {
        val getSpannedMethod = ::spannableStringBuilderGetSpannedMethod.method
        after {
            if (it.result == "")
                return@after

            val spannedContext = it.args[0]
            val spanned = getSpannedMethod(it.args[2]) as String
            // TODO EmojiCompat.process(spanned)
            hooks.forEach { it(ConversionContext(spannedContext), spanned) }
        }
    }
}

private val hooks = mutableListOf<(Any, CharSequence) -> Unit>()
private val overrides = mutableListOf<(Any, CharSequence) -> String>()

internal fun hookSpannableString(
    hook: (Any, CharSequence) -> Unit,
//    overrideSpan: Boolean = false
) {
    hooks.add { a, b -> hook(a, b) }
}