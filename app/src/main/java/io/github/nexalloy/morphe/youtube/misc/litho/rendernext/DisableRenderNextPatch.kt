package io.github.nexalloy.morphe.youtube.misc.litho.rendernext

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.patch

/**
 * Disables RenderNext (ElementsView), so the app is always rendered with Litho.
 *
 * RenderNext items are not seen by the Litho filters/hooks, so when it is enabled by a server
 * A/B test components such as the home feed Shorts shelf are never filtered.
 *
 * Ported from Morphe v1.45.0: `disableRenderNextPatch` (#3347).
 */
val DisableRenderNext = patch(
    name = "Disable RenderNext",
    description = "Disables the RenderNext (ElementsView) renderer so all components are filtered by Litho.",
) {
    // Force the RenderNext feature flag off, if read through the standard flag getter.
    insertLiteralOverride(RenderNextFlagLiteral)

    // The no-argument RenderNext flag getter.
    RenderNextFeatureFlagFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))

    // Whatever the server asked for, never present elements with RenderNext.
    RenderNextEnablementCheckFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))

    // Never convert Litho components to RenderNext.
    RenderNextTemplateCheckFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
}