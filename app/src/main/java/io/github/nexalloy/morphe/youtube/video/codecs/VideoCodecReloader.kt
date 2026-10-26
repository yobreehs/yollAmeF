package io.github.nexalloy.morphe.youtube.video.codecs

import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import app.morphe.extension.youtube.patches.LoadVideoPatch
import app.morphe.extension.youtube.patches.VideoInformation
import app.morphe.extension.youtube.patches.playback.speed.RememberPlaybackSpeedPatch
import app.morphe.extension.youtube.shared.PlayerType
import java.lang.ref.WeakReference
import java.lang.reflect.Method

/**
 * Reloads the currently playing video. The player is dismissed with the same obfuscated
 * method the upstream "Reload video" patch uses, then the video is reopened at the
 * same position. If that method cannot be resolved, only the intent is opened.
 */
internal object VideoCodecReloader {

    private var dismissPlayerMethod: Method? = null
    private var playerInstance = WeakReference<Any>(null)

    fun initialize(dismissMethod: Method?) {
        dismissPlayerMethod = dismissMethod
    }

    fun capturePlayerInstance(instance: Any) {
        playerInstance = WeakReference(instance)
    }

    fun reload() {
        if (PlayerType.current.isNoneOrHidden()) {
            Logger.printDebug { "VideoCodec: not reloading, player is not active" }
            return
        }

        val videoId = runCatching {
            if (VideoInformation.lastPlayerResponseIsShort()) {
                VideoInformation.getVideoId()
            } else {
                VideoInformation.getPlayerResponseVideoId()
            }
        }.getOrNull()

        if (videoId.isNullOrEmpty()) {
            Logger.printDebug { "VideoCodec: not reloading, video id is empty" }
            return
        }

        val playlistId = runCatching { VideoInformation.getPlaylistId() }.getOrDefault("")
        val videoTimeSeconds = runCatching { VideoInformation.getVideoTime() }.getOrDefault(0L) / 1000

        val url = buildString {
            append("https://www.youtube.com/watch?v=")
            append(videoId)
            if (playlistId.isNotEmpty()) {
                append("&list=")
                append(playlistId)
            }
            if (videoTimeSeconds > 0) {
                append("&t=")
                append(videoTimeSeconds)
                append('s')
            }
        }

        Logger.printDebug { "VideoCodec: reloading $url" }

        runCatching { RememberPlaybackSpeedPatch.preservePlaybackParametersForReload() }

        val dismissed = dismissPlayer()
        Utils.runOnMainThreadDelayed({
            LoadVideoPatch.openVideoIntent(url, false)
        }, if (dismissed) 500L else 0L)
    }

    private fun dismissPlayer(): Boolean {
        val method = dismissPlayerMethod ?: return false
        val instance = playerInstance.get() ?: return false

        return runCatching {
            method.isAccessible = true
            method.invoke(instance)
            true
        }.getOrElse {
            Logger.printDebug { "VideoCodec: could not dismiss player: $it" }
            false
        }
    }
}
