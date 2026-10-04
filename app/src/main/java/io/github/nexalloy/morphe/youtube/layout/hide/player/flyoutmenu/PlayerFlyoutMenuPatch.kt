package io.github.nexalloy.morphe.youtube.layout.hide.player.flyoutmenu

import app.morphe.extension.youtube.patches.HidePlayerFlyoutMenuPatch as ExtensionHidePlayerFlyoutMenuPatch
import app.morphe.extension.youtube.patches.components.PlayerFlyoutMenuComponentsFilter
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.shared.misc.litho.node.hookTreeNodeResult
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

/**
 * Adds options to hide menu components that appear when pressing the gear icon in the video
 * player (player flyout menu). Each item can be hidden independently via a sub-screen with
 * toggles.
 */
val HidePlayerFlyoutMenu = patch(
    name = "Hide player flyout menu components",
    description = "Adds options to hide menu components that appear when pressing the gear icon in the video player.",
) {
    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_hide_player_flyout",
            preferences = setOf(
                SwitchPreference("morphe_hide_player_flyout_additional_settings", summary = true),
                SwitchPreference("morphe_hide_player_flyout_ambient_mode", summary = true),
                SwitchPreference("morphe_hide_player_flyout_audio_track", summary = true),
                SwitchPreference("morphe_hide_player_flyout_audio_track_footer", summary = true),
                SwitchPreference("morphe_hide_player_flyout_captions", summary = true),
                SwitchPreference("morphe_hide_player_flyout_captions_footer", summary = true),
                SwitchPreference("morphe_hide_player_flyout_captions_header", summary = true),
                SwitchPreference("morphe_hide_player_flyout_help", summary = true),
                SwitchPreference("morphe_hide_player_flyout_listen_with_youtube_music", summary = true),
                SwitchPreference("morphe_hide_player_flyout_lock_screen", summary = true),
                SwitchPreference("morphe_hide_player_flyout_loop_video", summary = true),
                SwitchPreference("morphe_hide_player_flyout_on_the_go", summary = true),
                SwitchPreference("morphe_hide_player_flyout_quality", summary = true),
                SwitchPreference("morphe_hide_player_flyout_quality_footer", summary = true),
                SwitchPreference("morphe_hide_player_flyout_quality_header", summary = true),
                SwitchPreference("morphe_hide_player_flyout_sleep_timer", summary = true),
                SwitchPreference("morphe_hide_player_flyout_speed", summary = true),
                SwitchPreference("morphe_hide_player_flyout_stable_volume", summary = true),
                SwitchPreference("morphe_hide_player_flyout_watch_in_vr", summary = true),
            ),
        ),
    )

    addLithoFilter(PlayerFlyoutMenuComponentsFilter())

    // Native bottom sheet header (e.g. for quality / captions sheets).
    hookElement { bytes ->
        ExtensionHidePlayerFlyoutMenuPatch.hideNativeBottomSheetHeader(bytes)
    }

    // Native bottom sheet footer (element list tree-node hook).
    hookTreeNodeResult(
        { path, list ->
            ExtensionHidePlayerFlyoutMenuPatch.hideNativeBottomSheetFooter(path, list)
        },
        isLazilyConvertedElement = false,
    )
}