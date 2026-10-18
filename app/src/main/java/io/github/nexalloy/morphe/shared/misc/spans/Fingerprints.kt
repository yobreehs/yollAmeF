package io.github.nexalloy.morphe.shared.misc.spans

import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.shared.SpannableStringBuilderFingerprint

@SkipTest
internal object CustomCharacterStyleFingerprint : Fingerprint(
    returnType = "Landroid/graphics/Path;",
    parameters = listOf("Landroid/text/Layout;")
)

val customCharacterStyleClass = findClassDirect {
    val builder = SpannableStringBuilderFingerprint()
    // Both Elements implementations have this signature; use the span created by this text builder.
    CustomCharacterStyleFingerprint.matchAll().map { it.method.declaredClass!! }.distinct().single { candidate ->
        builder.instructions.any { it.opcode == Opcode.NEW_INSTANCE.ordinal && it.classRef == candidate }
    }
}

val setTextStyleMethod = findMethodDirect {
    SpannableStringBuilderFingerprint.instructionMatches[2].instruction.methodRef!!
}
