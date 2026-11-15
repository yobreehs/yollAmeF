/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3592
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.interaction.copytext

import io.github.nexalloy.morphe.Fingerprint

internal object ComponentHostDispatchTouchEventFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/litho/ComponentHost;",
    name = "dispatchTouchEvent",
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;")
)
