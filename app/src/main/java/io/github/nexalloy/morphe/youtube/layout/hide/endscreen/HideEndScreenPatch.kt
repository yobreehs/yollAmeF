package io.github.nexalloy.morphe.youtube.layout.hide.endscreen

import android.content.Context
import android.view.View
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.settings.BooleanSetting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.newfeatures.addNewFeaturesPlayerPreferences
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch

private const val SETTING_KEY = "morphe_hide_end_screen"

/** Name of the preferences file used by the in-app Morphe settings screen. */
private const val SETTINGS_PREFERENCES_NAME = "morphe_prefs"

/**
 * Setting backing the toggle. Created eagerly inside the patch (the Setting constructor needs
 * `Utils.getContext()`, which is already available when patches are applied) so the settings
 * screen always finds it in `Setting.getSettingFromPath()` and syncs both directions.
 *
 * MUST NOT be a top-level val: eager initialization at class load kills the whole module.
 */
internal lateinit var hideEndScreenSetting: BooleanSetting
    private set

@Volatile
private var lastLoggedValue: Boolean? = null

/**
 * Reads the toggle directly from the in-app preferences file on every call.
 *
 * The settings screen writes the switch value there, and reading the file avoids relying on the
 * cached [BooleanSetting] value. This keeps the feature in sync with the UI even if the
 * Setting/preference sync is skipped (for example when the settings screen was opened before
 * the Setting object was created).
 */
fun isHideEndScreenEnabled(): Boolean {
    val fromPreferences = runCatching {
        Utils.getContext()
            .getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
            // A missing key means the switch is at its default (false).
            .getBoolean(SETTING_KEY, false)
    }.getOrNull()

    val enabled = fromPreferences ?: runCatching { hideEndScreenSetting.get() }.getOrDefault(false)
    if (lastLoggedValue != enabled) {
        lastLoggedValue = enabled
        Logger.printDebug {
            "HideEndScreen: enabled=$enabled (preferences=${fromPreferences ?: "unavailable"})"
        }
    }
    return enabled
}

/**
 * Adds an option to hide the end screen (video suggestions with channel icons) that YouTube
 * shows at the end of a video.
 *
 * The end screen is composed of the creator end screen cards (channel avatar / icon / video
 * cards) and the autonav suggested-video preview. Both are hidden through dedicated method
 * hooks (see [Fingerprints.kt]):
 *
 * - `ShowEndScreenCardsFingerprint` — the method that displays the whole card group.
 * - `endScreenSuggestedVideoMethod` — the method that lays out the "up next" preview.
 *
 * The previous implementation hooked `ViewStub.setVisibility` / `View.findViewById` /
 * `View.setVisibility` by resource id. That never worked on current versions because the ids
 * `full_screen_end_screen` / `related_endscreen_results` are plain views inside layouts (not
 * ViewStubs), so the hooks either never matched or added a global `View.setVisibility` hook
 * that only cost performance.
 */
val HideEndScreen = patch(
    name = "Hide end screen",
    description = "Hides the video suggestions end screen (and its channel icons) shown at the end of a video.",
) {
    dependsOn(VersionCheck)

    hideEndScreenSetting = BooleanSetting(SETTING_KEY, false)

    // Registered only once, in the "New features → Player" chapter. Registering the same key a
    // second time (previously also on the main "Player" screen) leaves two independent
    // SwitchPreference objects that do not update each other, so the other screen keeps showing
    // a stale (often still checked) state until the settings activity is recreated.
    addNewFeaturesPlayerPreferences(
        SwitchPreference(SETTING_KEY, summary = true),
    )

    // NOTE: the previous revision also registered a Litho filter for the identifier
    // "teaser_carousel_with_controller". That identifier is not exclusive to the end screen:
    // the upstream comments filter uses the same component for the comments teaser
    // (see CommentsFilter's "comments" group), so filtering it made the comments disappear from
    // the watch page. The component is now left alone: the method hooks below already hide the
    // end screen, and they do not touch comments.

    // 1) Hide the creator end screen cards (video cards, channel avatar and icons).
    ShowEndScreenCardsFingerprint.hookMethod {
        before {
            if (isHideEndScreenEnabled()) {
                Logger.printDebug { "HideEndScreen: suppressing end screen cards" }
                it.result = null
            }
        }
    }

    // Hide the cards as well right after they are created, in case a variant of the layout is
    // added to the player without going through the method above.
    listOf(
        EndScreenLayoutCircleFingerprint,
        EndScreenLayoutIconFingerprint,
        EndScreenLayoutVideoFingerprint,
    ).forEach { fingerprint ->
        fingerprint.memberOrNull?.hookMethod {
            after {
                if (isHideEndScreenEnabled()) {
                    (it.result as? View)?.visibility = View.GONE
                }
            }
        }
    }

    // 2) Hide the suggested video ("up next") preview.
    ::endScreenSuggestedVideoMethod.memberOrNull?.hookMethod {
        before {
            if (isHideEndScreenEnabled()) {
                Logger.printDebug { "HideEndScreen: suppressing suggested video end screen" }
                it.result = null
            }
        }
    }
}
