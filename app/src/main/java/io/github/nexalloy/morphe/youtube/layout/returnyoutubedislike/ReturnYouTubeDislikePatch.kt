package io.github.nexalloy.morphe.youtube.layout.returnyoutubedislike

import app.morphe.extension.youtube.patches.ReturnYouTubeDislikePatch
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.endpointServiceNameField
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.hookLikeDislikeButtons
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.likeEndpointParserMethod
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.votePayloadPresenceField
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.votePayloadTargetField
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.votePayloadUnpackMethod
import io.github.nexalloy.morphe.shared.layout.returnyoutubedislike.voteVideoIdField
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.textcomponent.hookLithoSpannableString
import io.github.nexalloy.morphe.shared.misc.textcomponent.lithoSpannableStringPatch
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookPlayerResponseVideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookVideoId
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

/**
 * Shows the dislike count of videos using the Return YouTube Dislike service.
 *
 * The extension implementation is already part of the bundled patches. This app-side patch
 * only wires the video-id events and the Litho text transformation, and registers the UI.
 */
val returnYouTubeDislikePatch = patch(
    name = "Return YouTube Dislike",
    description = "Adds an option to show the dislike count of videos with Return YouTube Dislike.",
    settings = {
        PreferenceScreen.NEW_PATCHES.addPreferences(
            PreferenceCategory(
                key = "morphe_new_features_ryd",
                titleKey = "morphe_settings_screen_09_return_youtube_dislike_title",
                preferences = setOf(
                    SwitchPreference("morphe_ryd_enabled"),
                    SwitchPreference("morphe_ryd_dislike_percentage", summary = true),
                    SwitchPreference("morphe_ryd_estimated_like", summary = true),
                    SwitchPreference("morphe_ryd_toast_on_connection_error", summary = true),
                ),
            ),
        )
    },
) {
    dependsOn(
        lithoSpannableStringPatch,
        VideoInformationPatch,
        VideoId,
    )

    // Update the dislike data when a new video is loaded.
    hookVideoId(ReturnYouTubeDislikePatch::newVideoLoaded)

    // Start loading the data as soon as the player response is parsed.
    hookPlayerResponseVideoId(ReturnYouTubeDislikePatch::preloadVideoId)

    // Hook the like/dislike button clicks to send votes to the API.
    val endpointField = ::endpointServiceNameField.field
    val videoIdField = ::voteVideoIdField.field
    val targetField = ::votePayloadTargetField.field
    val presenceField = ::votePayloadPresenceField.field
    ::likeEndpointParserMethod.hookMethod(scopedHook(::votePayloadUnpackMethod.method) {
        after {
            val payload = it.result ?: return@after
            if (targetField.declaringClass.isInstance(payload) && presenceField.getInt(payload) and 2 != 0) {
                outerParam.setObjectExtra("rydVoteTarget", targetField.get(payload))
            }
        }
    })
    ::likeEndpointParserMethod.hookMethod {
        after {
            if (it.hasThrowable()) return@after
            val target = it.getObjectExtra("rydVoteTarget") ?: return@after
            ReturnYouTubeDislikePatch.sendVote(
                endpointField.get(it.thisObject) as String,
                videoIdField.get(target) as String
            )
        }
    }

    // Replace the like/dislike button text with the RYD count.
    hookLithoSpannableString(ReturnYouTubeDislikePatch::onLithoTextLoaded)

    // Draw the counts over the compact like/dislike buttons.
    hookLikeDislikeButtons(
        ReturnYouTubeDislikePatch::onComponentHostContentDescription,
        ReturnYouTubeDislikePatch::onYogaSetWidth
    )
}
