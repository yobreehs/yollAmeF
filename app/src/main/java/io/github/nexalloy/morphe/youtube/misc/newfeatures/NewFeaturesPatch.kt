package io.github.nexalloy.morphe.youtube.misc.newfeatures

import app.morphe.extension.youtube.patches.ForceFullscreenLandscapePatch
import app.morphe.extension.youtube.shared.PlayerType
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

/**
 * Player chapter ("chapter 5") of the "New features" screen. Filled by the individual player
 * patches (autoplay preview, playlist autoplay, flyout menu, ambient mode, miniplayer,
 * overlay buttons) so everything new lives in one place.
 */
internal val newFeaturesPlayerChapterPreferences = mutableSetOf<BasePreference>()

internal fun addNewFeaturesPlayerPreferences(vararg preferences: BasePreference) {
    newFeaturesPlayerChapterPreferences += preferences
}

/**
 * FemAlloy convenience section: settings hoisted into "New features" that upstream v1.45
 * registers only on their own screens. All toggles read the same extension settings, so this
 * patch only registers the UI (plus any fork-specific wiring).
 */
val NewFeatures = patch(
    name = "<NewFeatures>",
) {
    PreferenceScreen.NEW_PATCHES.addPreferences(
        PreferenceCategory(
            key = "morphe_new_patches_chapter_2",
            titleKey = "morphe_new_patches_chapter_2_title",
            preferences = setOf(
                SwitchPreference("morphe_volume_boost", summary = true),
                SwitchPreference("morphe_force_fullscreen_landscape", summary = true),
            ),
        ),
        PreferenceCategory(
            key = "morphe_new_patches_chapter_3",
            titleKey = "morphe_new_patches_chapter_3_title",
            preferences = setOf(
                SwitchPreference("morphe_hide_live_streams", summary = true),
                SwitchPreference("morphe_hide_history_shelf", summary = true),
            ),
        ),
        // The shared mutable set keeps the references, so preferences registered later by the
        // player patches are visible when the settings screen is built (SettingsHook runs last).
        PreferenceCategory(
            key = "morphe_new_patches_chapter_5",
            titleKey = "morphe_new_patches_chapter_5_title",
            preferences = newFeaturesPlayerChapterPreferences,
        ),
    )

    // Force fullscreen landscape only applies on tablets/large screens (checked inside the
    // extension), so hooking the player type change is harmless on phones.
    PlayerType.onChange.addObserver { type ->
        ForceFullscreenLandscapePatch.onPlayerTypeChanged(type)
    }
}