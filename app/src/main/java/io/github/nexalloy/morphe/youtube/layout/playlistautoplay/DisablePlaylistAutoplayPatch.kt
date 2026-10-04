package io.github.nexalloy.morphe.youtube.layout.playlistautoplay

import app.morphe.extension.youtube.patches.DisablePlaylistAutoplayPatch as ExtensionDisablePlaylistAutoplayPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

/**
 * Adds an option to stop a playlist from automatically advancing to the next video.
 */
val DisablePlaylistAutoplay = patch(
    name = "Disable playlist autoplay",
    description = "Adds an option to stop a playlist from automatically advancing to the next video.",
) {
    PreferenceScreen.PLAYER.addPreferences(
        SwitchPreference("morphe_disable_playlist_autoplay", summary = true),
    )

    // The navigation intent wrapper passed to the queue-advancing methods carries the enum;
    // extract it and let the extension decide whether the playlist should advance.
    // Each method is hooked independently so one unroutable method cannot fail the whole patch.
    ::playlistAutoplayNavigationMethods.dexMethodList.forEach { method ->
        runCatching {
            method.hookMethod {
                before { param ->
                    val wrapper = param.args.firstOrNull() ?: return@before
                    val navigationIntent = runCatching {
                        ::playlistAutoplayNavigationIntentField.field.get(wrapper) as? Enum<*>
                    }.getOrNull() ?: return@before

                    if (ExtensionDisablePlaylistAutoplayPatch.shouldSkipPlaylistAutoplay(navigationIntent)) {
                        param.result = null
                    }
                }
            }
        }.onFailure { error ->
            app.morphe.extension.shared.Logger.printDebug {
                "Disable playlist autoplay: hook for $method failed: ${error.message}"
            }
        }
    }
}