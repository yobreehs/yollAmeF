package io.github.nexalloy.morphe.shared.layout.theme

import android.graphics.Paint
import android.graphics.drawable.Drawable
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

private val colorOverrides = mutableListOf<(Int) -> Int>()
private val drawableColorOverrides = mutableListOf<(Drawable, Int) -> Int>()
private var colorHookInstalled = false

fun lithoColorOverrideHook(override: (Int) -> Int) {
    colorOverrides.add(override)
}

/** Hooks the original color of a Litho background drawable, before the color overrides. */
fun lithoDrawableColorHook(override: (Drawable, Int) -> Int) {
    drawableColorOverrides.add(override)
}

internal fun lithoColorHookPatch(useModernHook: () -> Boolean) = patch {
    // Player controls and seekbar colors share this hook and must apply the overrides only once.
    if (colorHookInstalled) return@patch
    val modern = useModernHook()
    val fingerprint = if (modern) LithoOnBoundsChangeFingerprint else LithoOnBoundsChangeLegacyFingerprint
    val paintField = if (modern) ::lithoPaintField.field else ::lithoPaintLegacyField.field
    fingerprint.hookMethod(scopedHook(Paint::class.java.getDeclaredMethod("setColor", Int::class.javaPrimitiveType)) {
        before {
            if (it.thisObject === paintField.get(outerParam.thisObject)) {
                val drawable = outerParam.thisObject as Drawable
                val originalColor = drawableColorOverrides.fold(it.args[0] as Int) { color, override ->
                    override(drawable, color)
                }
                it.args[0] = colorOverrides.fold(originalColor) { color, override -> override(color) }
            }
        }
    })
    colorHookInstalled = true
}
