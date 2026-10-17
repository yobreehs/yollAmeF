package io.github.nexalloy.morphe.shared.layout.theme

import android.graphics.Paint
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

private val colorOverrides = mutableListOf<(Int) -> Int>()

fun lithoColorOverrideHook(override: (Int) -> Int) {
    colorOverrides.add(override)
}

internal fun lithoColorHookPatch(useModernHook: () -> Boolean) = patch {
    val modern = useModernHook()
    val fingerprint = if (modern) LithoOnBoundsChangeFingerprint else LithoOnBoundsChangeLegacyFingerprint
    val paintField = if (modern) ::lithoPaintField.field else ::lithoPaintLegacyField.field
    fingerprint.hookMethod(scopedHook(Paint::class.java.getDeclaredMethod("setColor", Int::class.javaPrimitiveType)) {
        before {
            if (it.thisObject === paintField.get(outerParam.thisObject)) {
                it.args[0] = colorOverrides.fold(it.args[0] as Int) { color, override -> override(color) }
            }
        }
    })
}
