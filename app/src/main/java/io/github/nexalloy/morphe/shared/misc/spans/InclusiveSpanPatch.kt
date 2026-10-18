package io.github.nexalloy.morphe.shared.misc.spans

import android.text.SpannableString
import app.morphe.extension.shared.patches.spans.InclusiveSpanPatch
import app.morphe.extension.shared.patches.spans.SpanFilter
import io.github.nexalloy.morphe.shared.misc.textcomponent.hookSpannableString
import io.github.nexalloy.morphe.shared.misc.textcomponent.textComponentPatch
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

fun addSpanFilter(filter: SpanFilter) = InclusiveSpanPatch.addFilter(filter)

val inclusiveSpanPatch = patch(
    description = "Hooks SpannableString setting and filters specific span components."
) {
    dependsOn(textComponentPatch)

    hookSpannableString(InclusiveSpanPatch::setConversionContext)
    InclusiveSpanPatch.setCustomCharacterStyle(::customCharacterStyleClass.clazz)

    ::setTextStyleMethod.hookMethod(scopedHook(
        DexMethod("Landroid/text/SpannableString;->setSpan(Ljava/lang/Object;III)V").toMethod()
    ) {
        before {
            if (innerDepth != 0) return@before
            if (InclusiveSpanPatch.shouldSkipSpan(
                    it.thisObject as SpannableString, it.args[0],
                    it.args[1] as Int, it.args[2] as Int, it.args[3] as Int
                )) {
                it.result = null
            }
        }
    })
}
