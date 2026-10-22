package io.github.nexalloy.morphe.shared.misc.spans

import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.shared.SpannableStringBuilderFingerprint
import io.github.nexalloy.morphe.string

@SkipTest
internal object CustomCharacterStyleFingerprint : Fingerprint(
    returnType = "Landroid/graphics/Path;",
    parameters = listOf("Landroid/text/Layout;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/text/Layout;->getLineCount()I"
        ),
        string("IOOBDiagnostics: line:%s tl:%s lc:%s so:%s eo:%s fl:%s ll:%s rtl:%s sp:%s isp:%s esp:%s")
    )
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
