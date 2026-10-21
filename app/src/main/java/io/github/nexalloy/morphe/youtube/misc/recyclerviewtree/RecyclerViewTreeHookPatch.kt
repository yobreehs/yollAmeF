package io.github.nexalloy.morphe.youtube.misc.recyclerviewtree

import android.support.v7.widget.RecyclerView
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

private val recyclerViewTreeHooks = mutableListOf<(RecyclerView) -> Unit>()

fun addRecyclerViewTreeHook(hook: (recyclerView: RecyclerView) -> Unit) {
    recyclerViewTreeHooks.add(hook)
}

val recyclerViewTreeHook = patch {
    RecyclerViewTreeObserverFingerprint.hookMethod(scopedHook(::RecyclerView_addOnScrollListener.member) {
        before {
            val recyclerView = it.thisObject as RecyclerView
            recyclerViewTreeHooks.forEach { hook ->
                hook(recyclerView)
            }
        }
    })
}
