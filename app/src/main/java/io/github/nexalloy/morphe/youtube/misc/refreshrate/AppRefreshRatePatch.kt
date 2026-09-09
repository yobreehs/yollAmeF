package io.github.nexalloy.morphe.youtube.misc.refreshrate

import android.app.Activity
import app.morphe.extension.youtube.shared.PlayerType
import app.morphe.extension.youtube.shared.VideoState
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

val AppRefreshRate = patch(
    name = "App refresh rate",
    description = "Adds options to force the display refresh rate for the app, optionally only during video playback.",
) {
    PreferenceScreen.NEW_PATCHES.addPreferences(
        PreferenceCategory(
            titleKey = "morphe_new_patches_chapter_1_title",
            preferences = setOf(
                NonInteractivePreference(
                    key = "morphe_app_refresh_rate",
                    tag = RefreshRatePreference::class.java,
                    selectable = true,
                ),
                ListPreference("morphe_app_refresh_rate_type"),
            )
        )
    )

    DexMethod("$YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE->onCreate(Landroid/os/Bundle;)V").hookMethod {
        before {
            AppRefreshRateController.initialize(it.thisObject as Activity)
        }
    }

    VideoState.onChange.addObserver { state ->
        val type = PlayerType.current
        AppRefreshRateController.setPlayerIsActive(
            state == VideoState.PLAYING && type == PlayerType.WATCH_WHILE_MAXIMIZED,
            state == VideoState.PLAYING && type == PlayerType.WATCH_WHILE_FULLSCREEN,
        )
    }

    PlayerType.onChange.addObserver { type ->
        val state = VideoState.current
        AppRefreshRateController.setPlayerIsActive(
            state == VideoState.PLAYING && type == PlayerType.WATCH_WHILE_MAXIMIZED,
            state == VideoState.PLAYING && type == PlayerType.WATCH_WHILE_FULLSCREEN,
        )
    }
}
