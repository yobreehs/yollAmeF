package io.github.nexalloy.morphe.youtube.layout.originaltitles

import android.media.MediaMetadata
import android.media.session.MediaSession
import android.view.View
import android.widget.TextView
import app.morphe.extension.youtube.patches.originaltitles.RestoreOriginalTitlesPatch
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.getResourceId
import io.github.nexalloy.morphe.shared.MediaSessionSetMetadataFingerprint
import io.github.nexalloy.morphe.shared.misc.litho.relayout.lithoRelayoutPatch
import io.github.nexalloy.morphe.shared.misc.proto.hookElement
import io.github.nexalloy.morphe.shared.misc.textcomponent.hookLithoSpannableString
import io.github.nexalloy.morphe.shared.misc.textcomponent.lithoSpannableStringPatch
import io.github.nexalloy.morphe.youtube.misc.proto.elementProtoParserHookPatch
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.hookVideoId
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

internal val videoTitlesHookPatch = patch(
    description = "Hooks the video titles, video descriptions and channel descriptions."
) {
    dependsOn(
        elementProtoParserHookPatch,
        lithoSpannableStringPatch,
        lithoRelayoutPatch,
        VideoId
    )

    // Elements are parsed on the main thread, where the original title cannot be fetched.
    // The element hook finds the translated title and starts fetching the original title,
    // and the text hook replaces the title text when the element is laid out. Titles not yet
    // fetched are shown as loading, and the Litho views are laid out again when fetched.
    hookElement(RestoreOriginalTitlesPatch::restoreOriginalTitle)
    hookLithoSpannableString(RestoreOriginalTitlesPatch::onLithoTextLoaded)

    // The original description is fetched when the video is opened,
    // and replaced when the description panel is opened.
    hookVideoId(RestoreOriginalTitlesPatch::newVideoLoaded)

    // The title of the media notification.
    val setMetadata = MediaSession::class.java.getMethod("setMetadata", MediaMetadata::class.java)
    MediaSessionSetMetadataFingerprint.hookMethod(scopedHook(setMetadata) {
        before {
            it.args[0] = RestoreOriginalTitlesPatch.restoreMediaMetadataTitle(
                it.thisObject as MediaSession, it.args[0] as? MediaMetadata
            )
        }
    })

    // The playlist panel on the watch page does not use Litho.
    val titleFields = ::playlistPanelTitleFields.fieldList
    val videoIdFields = ::playlistPanelVideoIdFields.fieldList
    ::playlistPanelVideoBindMethods.dexMethodList.forEach { method ->
        val holderClass = method.toMethod().declaringClass
        val titleField = titleFields.single { it.declaringClass == holderClass }
        // Video id is stored in a field of the view holder.
        val videoIdField = videoIdFields.single { it.declaringClass == holderClass }
        method.hookMethod {
            after {
                if (it.hasThrowable()) return@after
                RestoreOriginalTitlesPatch.restoreOriginalTitle(
                    titleField.get(it.thisObject) as? TextView,
                    videoIdField.get(it.thisObject) as? String
                )
            }
        }
    }

    val findViewById = View::class.java.getMethod("findViewById", Int::class.javaPrimitiveType)

    // The video information shown while the video is loading shows the title of the element that opened the video,
    // which can be marked with the video id.
    val loadingTitleId = getResourceId(ResourceType.ID, "title")
    LoadingVideoInformationBindFingerprint.hookMethod(scopedHook(findViewById) {
        after {
            if (innerDepth == 0 && it.args[0] == loadingTitleId) {
                RestoreOriginalTitlesPatch.restoreKnownTitles(it.result as? TextView)
            }
        }
    })

    // The fullscreen engagement overlay shows the title of the opened video without the video id.
    val engagementTitleId = getResourceId(ResourceType.ID, "engagement_title")
    FullscreenEngagementOverlayFingerprint.hookMethod(scopedHook(findViewById) {
        after {
            if (innerDepth == 0 && it.args[0] == engagementTitleId) {
                RestoreOriginalTitlesPatch.restorePlayerTitle(it.result as? TextView)
            }
        }
    })

    // The minimal miniplayer shows the title of the opened video without the video id.
    val floatyTitleId = getResourceId(ResourceType.ID, "floaty_title")
    MinimalMiniplayerTitleViewFingerprint.hookMethod(scopedHook(findViewById) {
        after {
            if (innerDepth == 0 && it.args[0] == floatyTitleId) {
                RestoreOriginalTitlesPatch.restorePlayerTitle(it.result as? TextView)
            }
        }
    })

    // The next video of the collapsed playlist panel does not include the video id.
    val nextVideoTitleId = getResourceId(ResourceType.ID, "next_video_title")
    ::nextVideoTitleViewMethods.dexMethodList.forEach { method ->
        method.hookMethod(scopedHook(findViewById) {
            after {
                if (innerDepth == 0 && it.args[0] == nextVideoTitleId) {
                    RestoreOriginalTitlesPatch.restoreKnownTitles(it.result as? TextView)
                }
            }
        })
    }
}
