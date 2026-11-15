package io.github.nexalloy.morphe.youtube.layout.thumbnails

import app.morphe.extension.youtube.patches.dearrow.DeArrowPatch.handleCronetFailure
import app.morphe.extension.youtube.patches.dearrow.DeArrowPatch.handleCronetSuccess
import app.morphe.extension.youtube.patches.dearrow.DeArrowPatch.overrideImageURL
import app.morphe.extension.youtube.settings.preference.DeArrowAboutPreference
import io.github.nexalloy.patch
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.addImageUrlErrorCallbackHook
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.addImageUrlHook
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.addImageUrlSuccessCallbackHook
import io.github.nexalloy.morphe.youtube.misc.imageurlhook.cronetImageUrlHookPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen

val AlternativeThumbnailsPatch = patch(
    name = "DeArrow",
    description = "Adds options to replace video thumbnails and titles using the DeArrow API, " +
            "or replace video thumbnails with image captures from the video.",
    settings = {
        fun thumbnailPreference(key: String, titleKey: String) = ListPreference(
            key = key,
            titleKey = titleKey,
            entriesKey = "morphe_dearrow_thumbnail_options_entries",
            entryValuesKey = "morphe_dearrow_thumbnail_options_entry_values"
        )

        PreferenceScreen.ALTERNATIVE_THUMBNAILS.addPreferences(
            PreferenceCategory(
                key = "morphe_dearrow_titles_category",
                titleKey = "morphe_dearrow_titles_title",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("morphe_dearrow_titles_home"),
                    SwitchPreference("morphe_dearrow_titles_subscription"),
                    SwitchPreference("morphe_dearrow_titles_library"),
                    SwitchPreference("morphe_dearrow_titles_player"),
                    SwitchPreference("morphe_dearrow_titles_search"),
                    SwitchPreference("morphe_dearrow_titles_icon", summary = true),
                    SwitchPreference("morphe_dearrow_casual_mode", summary = true),
                    TextPreference("morphe_dearrow_casual_mode_min_votes", inputType = InputType.NUMBER)
                )
            ),
            PreferenceCategory(
                key = "morphe_dearrow_thumbnails_category",
                titleKey = "morphe_dearrow_thumbnails_title",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    thumbnailPreference("morphe_dearrow_thumbnail_home", "morphe_dearrow_titles_home_title"),
                    thumbnailPreference("morphe_dearrow_thumbnail_subscription", "morphe_dearrow_titles_subscription_title"),
                    thumbnailPreference("morphe_dearrow_thumbnail_library", "morphe_dearrow_titles_library_title"),
                    thumbnailPreference("morphe_dearrow_thumbnail_player", "morphe_dearrow_titles_player_title"),
                    thumbnailPreference("morphe_dearrow_thumbnail_search", "morphe_dearrow_titles_search_title"),
                    NonInteractivePreference("morphe_dearrow_thumbnail_stills_about"),
                    ListPreference("morphe_dearrow_thumbnail_stills_time")
                )
            ),
            PreferenceCategory(
                key = "morphe_dearrow_general_category",
                titleKey = "morphe_settings_screen_04_general_title",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    TextPreference("morphe_dearrow_api_url"),
                    SwitchPreference("morphe_dearrow_connection_toast", summary = true)
                )
            ),
            NonInteractivePreference(
                key = "morphe_dearrow_about",
                titleKey = null,
                summaryKey = "morphe_dearrow_about_summary",
                // Custom about preference with link to the DeArrow website.
                tag = DeArrowAboutPreference::class.java,
                selectable = true,
            ),
        )
    },
) {
    dependsOn(
        cronetImageUrlHookPatch,
    )

    addImageUrlHook(::overrideImageURL)
    addImageUrlSuccessCallbackHook(::handleCronetSuccess)
    addImageUrlErrorCallbackHook(::handleCronetFailure)
}
