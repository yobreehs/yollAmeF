package io.github.nexalloy.morphe.youtube.misc.recyclerviewtree

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.findMethodDirect
import org.luckypray.dexkit.result.ClassData

internal object RecyclerViewTreeObserverFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CHECK_CAST,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.NEW_INSTANCE,
    ),
    strings = listOf("LithoRVSLCBinder")
)

private val ClassData.isObject
    get() = this.descriptor.startsWith("L")

val RecyclerView_addOnScrollListener = findMethodDirect {
    RecyclerViewTreeObserverFingerprint().let { method ->
        val recyclerView = method.paramTypes[1]
        method.invokes.single {
            it.declaredClass == recyclerView && it.paramTypes.singleOrNull { clz -> clz.isObject } != null
        }
    }
}
