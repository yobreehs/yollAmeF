package io.github.nexalloy.morphe.youtube.video.quality

import app.morphe.extension.shared.settings.preference.NoTitlePreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_40_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val settingsMenuVideoQualityGroup = mutableSetOf<BasePreference>()

val VideoQuality = patch(
    name = "Video quality",
    description = "Adds options to set default video qualities and always use the advanced video quality menu.",
    settings = {
        PreferenceScreen.VIDEO.addPreferences(
            // Keep the preferences organized together.
            PreferenceCategory(
                key = "morphe_01_video_key", // Dummy key to force the quality preferences first.
                titleKey = null,
                sorting = PreferenceScreenPreference.Sorting.UNSORTED,
                tag = NoTitlePreferenceCategory::class.java,
                preferences = settingsMenuVideoQualityGroup
            )
        )

        // Construct the sub-patch so its own settings (the player dialog button) are registered
        // even when this patch is disabled.
        VideoQualityDialogButtonPatch
    },
) {
    dependsOn(
        RememberVideoQuality,
        AdvancedVideoQualityMenu,
        VideoQualityDialogButtonPatch,
        VersionCheck
    )

    // Flag breaks opening advanced quality menu for 20.40+.
    if (is_20_40_or_greater) {
        insertLiteralOverride(45712556)
    }
}