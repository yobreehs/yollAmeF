package io.github.nexalloy.morphe.shared.misc.litho.node

import app.morphe.extension.shared.patches.TreeNodeElementPatch
import io.github.nexalloy.Patch
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContext
import io.github.nexalloy.patch


private val componentLoadedHooks = mutableListOf<(String, MutableList<Any?>) -> Unit>()
private val lazilyConvertedElementLoadedHooks =
    mutableListOf<(String, MutableList<Any?>) -> Unit>()

/**
 * Register a handler to be called when a lazily converted element list is loaded.
 *
 * @param handler receives the identifier string and the mutable list of elements.
 *                The handler can modify the list in-place to filter elements.
 */
fun hookTreeNodeResult(
    handler: (String, MutableList<Any?>) -> Unit,
    isLazilyConvertedElement: Boolean = true
) {
    val list =
        if (isLazilyConvertedElement) lazilyConvertedElementLoadedHooks
        else componentLoadedHooks
    list.add(handler)
}

fun onComponentLoaded(path: String, treeNodeResultList: MutableList<Any?>) {
    componentLoadedHooks.forEach { hook -> hook(path, treeNodeResultList) }
}

fun onLazilyConvertedElementLoaded(
    identifier: String,
    treeNodeResultList: MutableList<Any?>
) {
    lazilyConvertedElementLoadedHooks.forEach { hook -> hook(identifier, treeNodeResultList) }
}

/**
 * Shared factory for the tree-node element hook patch used by both YouTube and YT Music.
 *
 * Hooks the tree-node result list from Litho so that patched extensions can inspect (and
 * physically remove entries from) the list before it is converted into rendered components.
 *
 * @param sharedExtensionPatchDep The app-specific `sharedExtensionPatch`.
 * @param conversionContextPatchDep The app-specific `conversionContextPatch`.
 */
internal fun createTreeNodeElementHookPatch(
    sharedExtensionPatchDep: Patch,
    conversionContextPatchDep: Patch,
    addLithoContainerInterface: Boolean,
    useLegacyContextRegister: PatchExecutor.() -> Boolean
) = patch(
    description = "Hooks the tree node element lists to the extension."
) {
    dependsOn(
        sharedExtensionPatchDep,
        conversionContextPatchDep
    )

    TreeNodeResultListFingerprint.hookMethod {
        after {
            @Suppress("UNCHECKED_CAST")
            val list = it.result as? MutableList<Any> ?: return@after
            val conversionContext = it.args[1]
            TreeNodeElementPatch.onTreeNodeResultLoaded(
                ConversionContext(conversionContext),
                list
            )
        }
    }

    // TODO addLithoContainerInterface for YT Music
}
