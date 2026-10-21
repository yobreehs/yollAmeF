package io.github.nexalloy.morphe.youtube.layout.buttons.navigation

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall

/**
 * Sets the bottom padding of the navigation bar to the bottom window inset, the room for
 * the system navigation buttons when the bar is drawn behind them. The bar height also grows
 * by the same value, which is first stored in a field of the bar.
 */
@SkipTest
internal object NavigationBarBottomPaddingFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT,
            definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
            type = "I"
        ),
        methodCall(
            definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
            name = "setPadding",
            location = MatchAfterWithin(10)
        )
    )
)

internal val navigationBarBottomPaddingMethods = findMethodListDirect {
    NavigationBarBottomPaddingFingerprint.matchAll().map { it.method }
}

internal val navigationBarBottomPaddingField = findFieldDirect {
    NavigationBarBottomPaddingFingerprint.matchAll().map {
        it.instructionMatches.first().instruction.fieldRef!!
    }.distinctBy { it.descriptor }.single()
}

internal val navigationBarRelayoutMethods = findMethodListDirect {
    val paddingField = navigationBarBottomPaddingField()
    NavigationBarBottomPaddingFingerprint.matchAll().map { match ->
        // The stored padding is consumed by the immediately following height update.
        val instruction = match.method.instructions[match.instructionMatches.first().index + 1]
        check(instruction.opcode == Opcode.INVOKE_VIRTUAL.opCode ||
                instruction.opcode == Opcode.INVOKE_VIRTUAL_RANGE.opCode)
        val reference = instruction.methodRef!!
        check(reference.declaredClass!!.descriptor == paddingField.declaredClass!!.descriptor &&
                reference.returnType!!.descriptor == "V" && reference.paramTypes.isEmpty())
        // A cross-dex method reference may have no body; resolve its definition before checking it.
        Fingerprint(
            definingClass = reference.declaredClass!!.descriptor,
            name = reference.name,
            returnType = "V",
            parameters = emptyList()
        ).match().method.also { method ->
            check(method.instructions.any {
                it.opcode == Opcode.IGET.opCode &&
                        it.fieldRef?.descriptor == paddingField.descriptor
            })
        }
    }.distinctBy { it.descriptor }
}

internal object CreatePivotBarFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    parameters = listOf(
        "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
        "Landroid/widget/TextView;",
        "Ljava/lang/CharSequence;",
    ),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
    ),
)

internal object AutoHideNavigationBarOnFeedScrollingFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/support/v7/widget/RecyclerView;", "I", "I"),
    filters = listOf(
        methodCall("Landroid/view/ViewConfiguration;->get(Landroid/content/Context;)Landroid/view/ViewConfiguration;"),
        methodCall(
            smali = "Landroid/view/ViewConfiguration;->getScaledTouchSlop()I",
            location = MatchAfterWithin(5)
        )
    )
)

internal object AutoHideNavigationBarOnDismissMiniplayerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("F"),
    filters = listOf(
        literal(2),
        methodCall(
            smali = "Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            smali = "Ljava/util/Set;->iterator()Ljava/util/Iterator;",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            smali = "Ljava/util/Iterator;->hasNext()Z",
            location = MatchAfterWithin(5)
        ),
        methodCall(
            smali = "Ljava/util/Iterator;->next()Ljava/lang/Object;",
            location = MatchAfterWithin(5)
        )
    )
)
