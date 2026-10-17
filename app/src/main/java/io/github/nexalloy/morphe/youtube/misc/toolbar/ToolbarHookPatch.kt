package io.github.nexalloy.morphe.youtube.misc.toolbar

import android.view.View
import android.widget.ImageView
import io.github.nexalloy.HookDsl
import io.github.nexalloy.IScopedHookCallback
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

private val toolbarHooks = mutableListOf<(String, View, ImageView) -> Unit>()

val toolBarHookPatch = patch(
    description = "toolBarHookPatch"
) {
    val imageViewField = ::toolbarImageViewField.field
    val drawableMethod = DexMethod(
        "Landroid/content/res/Resources;->getDrawable(I)Landroid/graphics/drawable/Drawable;"
    ).toMethod()

    val enumHook: HookDsl<IScopedHookCallback>.() -> Unit = {
        after {
            outerParam.setObjectExtra("toolbarIconEnum", (it.result as? Enum<*>)?.name)
        }
    }
    val imageViewHook: HookDsl<IScopedHookCallback>.() -> Unit = {
        before {
            val enumName = outerParam.getObjectExtra("toolbarIconEnum") as? String ?: return@before
            outerParam.setObjectExtra("toolbarIconEnum", null)
            val imageView = imageViewField.get(outerParam.thisObject) as ImageView
            val parentView = imageView.parent as? View ?: return@before
            toolbarHooks.forEach { hook -> hook(enumName, parentView, imageView) }
        }
    }

    ToolBarButtonFingerprint.hookMethod(scopedHook(
        ::toolbarIconEnumMethod.member to enumHook,
        drawableMethod to imageViewHook
    ))
}

fun hookToolBar(callback: (String, View, ImageView) -> Unit) {
    toolbarHooks.add(callback)
}
