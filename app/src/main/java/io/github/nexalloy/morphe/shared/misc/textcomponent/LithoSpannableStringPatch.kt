package io.github.nexalloy.morphe.shared.misc.textcomponent

import app.morphe.extension.shared.patches.components.ContextInterface
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContext
import io.github.nexalloy.morphe.youtube.shared.conversionContextFingerprintToString
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Modifier

internal object TextComponentConstructorFingerprint : Fingerprint(
    name = "<init>",
    strings = listOf("TextComponent")
)

internal object TextComponentLookupFingerprint : Fingerprint(
    classFingerprint = TextComponentConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    strings = listOf("…")
)

internal object TextComponentDataFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L", "L"),
    strings = listOf("text")
) {
    init {
        classMatcher {
            addField { type = "java.util.BitSet" }
        }
    }
}

internal object LithoSpannableStringCreationFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;", "L"),
    filters = listOf(
        newInstance(type = "Landroid/text/SpannableString;"),
        methodCall(
            smali = "Landroid/text/SpannableString;-><init>(Ljava/lang/CharSequence;)V",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            smali = "Landroid/text/SpannableString;->getSpans(IILjava/lang/Class;)[Ljava/lang/Object;",
            location = MatchAfterWithin(5)
        ),
        methodCall(name = "addOnLayoutChangeListener", parameters = listOf("Landroid/view/View\$OnLayoutChangeListener;"))
    )
)

private val textComponentConversionContextFieldData = findFieldDirect {
    val contextClass = conversionContextFingerprintToString(this).declaredClass!!
    TextComponentConstructorFingerprint().declaredClass!!.fields.single { it.type == contextClass }
}

private val lithoTextConversionContextFieldData = findFieldDirect {
    val contextClass = conversionContextFingerprintToString(this).declaredClass!!
    LithoSpannableStringCreationFingerprint().declaredClass!!.fields.single { it.type == contextClass }
}

private val textComponentTextFieldData = findFieldDirect {
    val match = TextComponentLookupFingerprint.match()
    val dataClass = TextComponentDataFingerprint().declaredClass!!
    val newData = match.method.instructions.indexOfFirst {
        it.opcode == Opcode.NEW_INSTANCE.ordinal && it.classRef == dataClass
    }
    check(newData >= 0)
    match.method.instructions.drop(newData).first {
        it.opcode == Opcode.IPUT_OBJECT.ordinal && it.fieldRef?.typeSign == "Ljava/lang/CharSequence;"
    }.fieldRef!!
}

private val lithoTextHolderTextFieldData = findFieldDirect {
    val match = LithoSpannableStringCreationFingerprint.match()
    val constructor = match.instructionMatches[1]
    var register = constructor.instruction.register(1)
    val source = match.method.instructions.take(constructor.index).asReversed().first {
        when {
            it.registerOrNull(0) != register -> false
            it.opcode in Opcode.MOVE_OBJECT.ordinal..Opcode.MOVE_OBJECT_16.ordinal -> {
                register = it.register(1)
                false
            }
            else -> it.opcode == Opcode.IGET_OBJECT.ordinal
        }
    }
    source.fieldRef!!.also { check(it.typeSign == "Ljava/lang/Object;") }
}

private val lithoTextHolderConstructorData = findMethodDirect {
    lithoTextHolderTextFieldData().declaredClass!!.methods.single {
        it.name == "<init>" && it.paramCount == 0
    }
}

private val lithoHooks = mutableListOf<(ContextInterface, CharSequence) -> CharSequence>()

private fun transformLithoText(context: ContextInterface, original: CharSequence): CharSequence =
    lithoHooks.fold(original) { text, hook -> hook(context, text) }

/**
 * Hooks the creation and cached lookup of YouTube Litho text spans, allowing a patch to
 * replace the text before it is drawn (used by Return YouTube Dislike and Copy text).
 */
internal val lithoSpannableStringPatch = patch(
    description = "Provides hooks into Litho text Spans for extension filtering."
) {
    // This hook handles all situations, as it's where the created Spans are stored and later reused.
    TextComponentLookupFingerprint.hookMethod {
        val contextField = ::textComponentConversionContextFieldData.field
        val textField = ::textComponentTextFieldData.field
        after {
            val data = it.result ?: return@after
            val original = textField.get(data) as? CharSequence ?: return@after
            val context = ConversionContext(contextField.get(it.thisObject) ?: return@after)
            textField.set(data, transformLithoText(context, original))
        }
    }

    // Hook new Litho text creation code.
    LithoSpannableStringCreationFingerprint.hookMethod {
        val contextField = ::lithoTextConversionContextFieldData.field
        val textField = ::lithoTextHolderTextFieldData.field
        val constructor = ::lithoTextHolderConstructorData.constructor
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

    // Use the new Litho text creation path where it exists.
    insertLiteralOverride(45675738L, true)
}

internal fun hookLithoSpannableString(hook: (ContextInterface, CharSequence) -> CharSequence) {
    lithoHooks.add(hook)
}
