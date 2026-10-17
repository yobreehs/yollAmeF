/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.misc.loadvideo

import android.content.Intent
import android.os.Parcelable
import app.morphe.extension.youtube.patches.LoadVideoPatch
import app.morphe.extension.youtube.patches.LoadVideoPatch.PlayerInterface
import io.github.nexalloy.createProxy
import io.github.nexalloy.patch

internal val loadVideoHookPatch = patch(
    description = "Hooks the player so the extension can dismiss it and open videos."
) {
    val dismissPlayer = ::dismissPlayerMethod.method
    val openNewVideoParcelable = OpenNewVideoIntentParcelableFingerprint.method

    // Add interface and helper methods to allow extension code to call obfuscated methods.
    // Add methods to access obfuscated player methods.
    ::playerControllerConstructor.hookMethod {
        after {
            LoadVideoPatch.initialize(it.thisObject.createProxy { reference ->
                object : PlayerInterface {
                    override fun patch_dismissPlayer() {
                        reference.get()?.let { player -> dismissPlayer(player) }
                    }

                    override fun patch_getIntentParcelable(intent: Intent): Parcelable? =
                        openNewVideoParcelable(null, intent) as Parcelable?
                }
            })
        }
    }

    BackButtonFinishActivityOnNewVideoIntentFingerprint.hookMethod {
        before { it.result = null }
    }
}
