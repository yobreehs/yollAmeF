package io.github.nexalloy.morphe.youtube.layout.buttons.action

import app.morphe.extension.youtube.innertube.NextResponseOuterClass
import app.morphe.extension.youtube.patches.components.QuickActionButtonsFilter
import app.morphe.extension.youtube.patches.components.VideoActionButtonsFilter
import io.github.nexalloy.callMethod
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.youtube.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.youtube.misc.litho.node.TreeNodeElementHook
import io.github.nexalloy.morphe.youtube.misc.litho.node.hookTreeNodeResult
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.WatchNextResponseParserFingerprint
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch

val HideVideoActionButtons = patch(
    name = "Hide video action buttons",
    description = "Adds options to hide video action buttons in fullscreen and portrait modes.",
) {
    dependsOn(
        LithoFilter,
        TreeNodeElementHook,
        VideoInformationPatch
    )

    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_action_buttons_screen",
            preferences = setOf(
                PreferenceCategory(
                    titleKey = "morphe_portrait_buttons",
                    preferences = setOf(
                        SwitchPreference("morphe_disable_like_subscribe_glow"),
                        SwitchPreference("morphe_hide_action_bar", summaryKey = null),
                        SwitchPreference("morphe_hide_ask_button", summaryKey = null),
                        SwitchPreference("morphe_hide_clip_button", summaryKey = null),
                        SwitchPreference("morphe_hide_comments_button", summaryKey = null),
                        SwitchPreference("morphe_hide_download_button", summaryKey = null),
                        SwitchPreference("morphe_hide_hype_button", summaryKey = null),
                        SwitchPreference("morphe_hide_like_dislike_button", summaryKey = null),
                        SwitchPreference("morphe_hide_promote_button", summaryKey = null),
                        SwitchPreference("morphe_hide_remix_button", summaryKey = null),
                        SwitchPreference("morphe_hide_report_button", summaryKey = null),
                        SwitchPreference("morphe_hide_save_button", summaryKey = null),
                        SwitchPreference("morphe_hide_share_button", summaryKey = null),
                        SwitchPreference("morphe_hide_shop_button", summaryKey = null),
                        SwitchPreference("morphe_hide_stop_ads_button", summaryKey = null),
                        SwitchPreference("morphe_hide_thanks_button", summaryKey = null)
                    )
                ),
                PreferenceCategory(
                    titleKey = "morphe_fullscreen_buttons",
                    preferences = setOf(
//                        NonInteractivePreference(
//                            key = "morphe_quick_actions_top_margin",
//                            tag = "app.morphe.extension.shared.settings.preference.SeekBarPreference"
//                        ),
                        SwitchPreference("morphe_hide_quick_actions", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_ask_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_comments_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_dislike_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_like_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_live_chat_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_mix_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_more_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_more_videos_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_playlist_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_save_button", summaryKey = null),
                        SwitchPreference("morphe_hide_quick_actions_share_button", summaryKey = null)
                    )
                )
            )
        )
    )

    addLithoFilter(VideoActionButtonsFilter())
    addLithoFilter(QuickActionButtonsFilter())

    hookTreeNodeResult { identifier, list ->
        VideoActionButtonsFilter.onLazilyConvertedElementLoaded(identifier, list)
    }

    WatchNextResponseParserFingerprint.hookMethod {
        before {
            val messageLite = it.args[0]

            val nextResponse =
                NextResponseOuterClass.NextResponse.parseFrom(messageLite.callMethod("toByteArray") as ByteArray)

            if (!nextResponse.hasPrimaryContents()) {
                return@before
            }

            val primaryContents = nextResponse.primaryContents
            if (primaryContents.hasSingleColumnWatchNextResults()) {
                VideoActionButtonsFilter.onSingleColumnWatchNextResultsLoaded(primaryContents.singleColumnWatchNextResults)
            }
        }
    }
}
