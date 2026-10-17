package io.github.nexalloy.morphe.shared.misc.textcomponent

import app.morphe.extension.shared.patches.components.ContextInterface
import app.morphe.extension.shared.patches.TextComponentPatch
import io.github.nexalloy.morphe.shared.SpannableStringBuilderFingerprint
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContext
import io.github.nexalloy.morphe.shared.misc.litho.context.conversionContextPatch
import io.github.nexalloy.morphe.shared.spannableStringBuilderGetSpannedMethod
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

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

private val hooks = mutableListOf<(ContextInterface, CharSequence) -> Unit>()
internal fun hookSpannableString(
    hook: (ContextInterface, CharSequence) -> Unit,
//    overrideSpan: Boolean = false
) {
    hooks.add { a, b -> hook(a, b) }
}

/**
 * Hooks the creation and cached lookup of YouTube Litho text Spans.
 */
internal val lithoSpannableStringPatch = patch(
    description = "Provides hooks into Litho text Spans for extension filtering."
) {
    dependsOn(conversionContextPatch)

    // region Hook code for creation and cached lookup of text Spans.

    // This hook handles all situations, as it's where the created Spans are stored and later reused.
    TextComponentLookupFingerprint.hookMethod {
        val contextField = ::textComponentConversionContextField.field
        val textField = ::textComponentTextField.field
        after {
            val data = it.result ?: return@after
            val original = textField.get(data) as? CharSequence ?: return@after
            val context = ConversionContext(contextField.get(it.thisObject) ?: return@after)
            textField.set(data, transformLithoText(context, original))
        }
    }

    // Hook new litho text creation code.
    LithoSpannableStringCreationFingerprint.hookMethod {
        val contextField = ::lithoTextConversionContextField.field
        val textField = ::lithoTextHolderTextField.field
        val constructor = ::lithoTextHolderConstructor.constructor
        val fields = constructor.declaringClass.declaredFields.filter { !Modifier.isStatic(it.modifiers) }
            .onEach { it.isAccessible = true }
        before {
            val holder = it.args[2] ?: return@before
            val original = textField.get(holder) as? CharSequence ?: return@before
            if (original.isEmpty()) return@before
            val context = ConversionContext(contextField.get(it.thisObject) ?: return@before)
            val replacement = transformLithoText(context, original)
            if (replacement === original) return@before

            // Copy the holder so cached text and concurrent readers keep the original Span.
            it.args[2] = constructor.newInstance().also { copy ->
                fields.forEach { field -> field.set(copy, field.get(holder)) }
                textField.set(copy, replacement)
            }
        }
    }

    insertLiteralOverride(45675738L, TextComponentPatch::useNewLithoTextCreation)

    // endregion
}

private val lithoHooks = mutableListOf<(ContextInterface, CharSequence) -> CharSequence>()

private fun transformLithoText(context: ContextInterface, original: CharSequence) =
    lithoHooks.fold(original) { text, hook -> hook(context, text) }

internal fun hookLithoSpannableString(hook: (ContextInterface, CharSequence) -> CharSequence) {
    lithoHooks.add(hook)
}
