package io.github.nexalloy.morphe.youtube.misc.newfeatures

import app.morphe.extension.shared.settings.preference.AiSListAttributionPreference
import app.morphe.extension.shared.settings.preference.BulletPointPreference
import app.morphe.extension.youtube.patches.ForceFullscreenLandscapePatch
import app.morphe.extension.youtube.settings.preference.AiSListStatsPreferenceCategory
import app.morphe.extension.youtube.shared.PlayerType
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

/**
 * "New features" screen. New patches added by FemAlloy register their preferences here,
 * grouped by the part of the app they affect.
 *
 * The old fork-specific settings that used to live on this screen have been moved to their
 * natural sections (General, Feed, Player, Video) so this screen only contains genuinely new
 * functionality.
 */
val NewFeatures = patch(
    name = "<NewFeatures>",
    settings = {
        PreferenceScreen.NEW_PATCHES.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_hide_aislist_screen",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    PreferenceCategory(
                        key = "morphe_hide_aislist_blocklist_category",
                        sorting = Sorting.UNSORTED,
                        preferences = setOf(
                            SwitchPreference(
                                key = "morphe_hide_aislist_blocklist_home",
                                titleKey = "morphe_hide_aislist_hide_home_title"
                            ),
                            SwitchPreference(
                                key = "morphe_hide_aislist_blocklist_search",
                                titleKey = "morphe_hide_aislist_hide_search_title"
                            )

                        )
                    ),
                    PreferenceCategory(
                        key = "morphe_hide_aislist_warnlist_category",
                        sorting = Sorting.UNSORTED,
                        preferences = setOf(
                            SwitchPreference(
                                key = "morphe_hide_aislist_warnlist_home",
                                titleKey = "morphe_hide_aislist_hide_home_title"
                            ),
                            SwitchPreference(
                                key = "morphe_hide_aislist_warnlist_search",
                                titleKey = "morphe_hide_aislist_hide_search_title"
                            )
                        )
                    ),
                    PreferenceCategory(
                        key = "morphe_hide_aislist_stats_category",
                        titleKey = "morphe_hide_stats_category_title",
                        sorting = Sorting.UNSORTED,
                        preferences = emptySet(),
                        tag = AiSListStatsPreferenceCategory::class.java
                    ),
                    PreferenceCategory(
                        key = "morphe_aislist_submit_category",
                        sorting = Sorting.UNSORTED,
                        preferences = setOf(
                            SwitchPreference("morphe_aislist_submit_flyout_menu", summary = true),
                            TextPreference(
                                key = "morphe_aislist_submit_username",
                                inputType = InputType.TEXT
                            )
                        )
                    ),
                    PreferenceCategory(
                        key = "morphe_hide_aislist_about_category",
                        titleKey = "morphe_hide_about_category_title",
                        sorting = Sorting.UNSORTED,
                        preferences = setOf(
                            NonInteractivePreference(
                                key = "morphe_hide_aislist_about",
                                titleKey = null,
                                summaryKey = "morphe_hide_aislist_about_summary",
                                tag = BulletPointPreference::class.java
                            ),
                            NonInteractivePreference(
                                key = "morphe_hide_aislist_attribution",
                                titleKey = null,
                                tag = AiSListAttributionPreference::class.java,
                                selectable = true
                            )
                        )
                    )
                )
            ),
        )

    },
) {

    // Force fullscreen landscape only applies on tablets/large screens (checked inside the
    // extension), so hooking the player type change is harmless on phones.
    PlayerType.getOnChange().addObserver { type ->
        ForceFullscreenLandscapePatch.onPlayerTypeChanged(type)
    }
}
