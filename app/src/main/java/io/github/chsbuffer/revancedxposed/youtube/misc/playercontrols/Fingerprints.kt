package io.github.chsbuffer.revancedxposed.youtube.misc.playercontrols

import io.github.chsbuffer.revancedxposed.AccessFlags
import io.github.chsbuffer.revancedxposed.fingerprint
import io.github.chsbuffer.revancedxposed.resourceMappings

val controls_layout_stub_id get() = resourceMappings["id", "controls_layout_stub"]
val fullscreen_button_id get() = resourceMappings["id", "fullscreen_button"]
val heatseeker_viewstub_id get() = resourceMappings["id", "heatseeker_viewstub"]
val inset_overlay_view_layout_id get() = resourceMappings["id", "inset_overlay_view_layout"]
val scrim_overlay_id get() = resourceMappings["id", "scrim_overlay"]

val youtubeControlsOverlayFingerprint = fingerprint {
    returns("V")
    parameters()
    methodMatcher {
        addInvoke { name = "setFocusableInTouchMode" }
        addUsingNumber(inset_overlay_view_layout_id)
        addUsingNumber(scrim_overlay_id)
    }
}

val motionEventFingerprint = fingerprint {
    returns("V")
    parameters("Landroid/view/MotionEvent;")
    methodMatcher { addInvoke { name = "setTranslationY" } }
    classMatcher { className(youtubeControlsOverlayFingerprint(dexkit).className) }
}

val playerTopControlsInflateFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters()
    literal { controls_layout_stub_id }
}

val overlayViewInflateFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters("Landroid/view/View;")
    methodMatcher {
        addUsingNumber(fullscreen_button_id)
        addUsingNumber(heatseeker_viewstub_id)
    }
}

val controlsOverlayVisibilityFingerprint = fingerprint {
    accessFlags(AccessFlags.PRIVATE, AccessFlags.FINAL)
    returns("V")
    parameters("Z", "Z")
    classMatcher {
        className(playerTopControlsInflateFingerprint(dexkit).className)
    }
}