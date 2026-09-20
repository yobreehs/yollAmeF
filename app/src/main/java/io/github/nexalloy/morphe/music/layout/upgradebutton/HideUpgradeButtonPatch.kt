package io.github.nexalloy.morphe.music.layout.upgradebutton

import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.patch
import java.lang.reflect.Field

val HideUpgradeButton = patch(
    name = "Hide upgrade button",
    description = "Hides the upgrade tab from the pivot bar.",
) {
    // TODO Patch is obsolete and was replaced by navigation bar patch
    ::pivotBarConstructorFingerprint.hookMethod {
        var pivotBarElementField: Field? = null

        after { param ->
            if (pivotBarElementField == null) {
                pivotBarElementField = param.thisObject.let {
                    it.javaClass.declaredFields.first { f ->
                        f.get(it) is List<*>
                    }
                }
            }

            val list = pivotBarElementField.get(param.thisObject) as ArrayList<*>
            try {
                list.removeAt(4)
            } catch (e: XposedHelpers.InvocationTargetError) {
                if (e.cause !is IndexOutOfBoundsException) throw e
            }
        }
    }
}