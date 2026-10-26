package io.github.nexalloy.morphe.youtube.video.codecs

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.TextView
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import app.morphe.extension.youtube.patches.LegacyPlayerControlsPatch
import app.morphe.extension.youtube.videoplayer.PlayerOverlayButton
import io.github.nexalloy.R
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.layout.player.buttons.addPlayerBottomButton
import io.github.nexalloy.morphe.youtube.layout.player.buttons.playerOverlayButtonsHook
import io.github.nexalloy.morphe.youtube.misc.newfeatures.addNewFeaturesPlayerPreferences
import io.github.nexalloy.patch
import java.lang.ref.WeakReference

private const val CODEC_BUTTON_PREFERENCE_KEY = "morphe_video_codec_button"

internal fun isVideoCodecButtonEnabled(): Boolean = runCatching {
    Utils.getContext()
        .getSharedPreferences(CODEC_PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getBoolean(CODEC_BUTTON_PREFERENCE_KEY, false)
}.getOrDefault(false)

/**
 * Adds a text button to the player overlay. Tapping it opens the codec chooser, long
 * pressing cycles through the available codecs. Changing the codec reloads the video
 * at the current position.
 */
val VideoCodecButton = patch(
    name = "Video codec button",
    description = "Adds a button to the video player for changing the video codec during playback."
) {
    dependsOn(
        VideoCodec,
        playerOverlayButtonsHook,
    )

    addNewFeaturesPlayerPreferences(
        SwitchPreference(CODEC_BUTTON_PREFERENCE_KEY, summary = true),
    )
    addPlayerBottomButton(VideoCodecButtonUi::initializeButton)
}

internal object VideoCodecButtonUi {

    private var overlayText = WeakReference<TextView>(null)
    private var lastControlsView = WeakReference<View>(null)

    fun initializeButton(controlsView: View) {
        try {
            if (LegacyPlayerControlsPatch.RESTORE_OLD_PLAYER_BUTTONS) return
            if (lastControlsView.get() === controlsView) return
            lastControlsView = WeakReference(controlsView)

            val textView = PlayerOverlayButton.addButtonWithTextOverlay(
                controlsView,
                { isVideoCodecButtonEnabled() },
                { view -> showCodecDialog(view.context) },
                { view ->
                    cycleCodec(view.context)
                    true
                },
            ) ?: return

            overlayText = WeakReference(textView)
            updateText()
        } catch (ex: Throwable) {
            Logger.printException({ "VideoCodec: failed to initialize player button" }, ex)
        }
    }

    fun updateText() {
        val textView = overlayText.get() ?: return
        val label = codecLabel(selectedVideoCodec())

        Utils.runOnMainThread {
            runCatching { textView.text = label }
        }
    }

    private fun showCodecDialog(context: Context) {
        try {
            val entries = context.resources.getStringArray(R.array.morphe_video_codec_entries)
            val values = context.resources.getStringArray(R.array.morphe_video_codec_entry_values)
            if (entries.size != values.size) return

            val selectedCodec = selectedVideoCodec()
            val selectedIndex = values.indexOf(selectedCodec).coerceAtLeast(0)

            AlertDialog.Builder(Utils.getActivity() ?: context)
                .setTitle(R.string.morphe_video_codec_title)
                .setSingleChoiceItems(entries, selectedIndex) { dialog, which ->
                    setSelectedVideoCodec(values[which])
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } catch (ex: Throwable) {
            Logger.printException({ "VideoCodec: failed to show codec dialog" }, ex)
        }
    }

    private fun cycleCodec(context: Context) {
        try {
            val values = context.resources.getStringArray(R.array.morphe_video_codec_entry_values)
            val currentIndex = values.indexOf(selectedVideoCodec())
            val nextCodec = values[(currentIndex + 1).mod(values.size)]
            setSelectedVideoCodec(nextCodec)

            val toast = runCatching {
                context.getString(R.string.morphe_video_codec_selected, codecLabel(nextCodec))
            }.getOrDefault(codecLabel(nextCodec))
            Utils.showToastShort(toast)
        } catch (ex: Throwable) {
            Logger.printException({ "VideoCodec: failed to cycle codec" }, ex)
        }
    }
}

internal fun codecLabel(codec: String): String = when (codec) {
    CODEC_AV1 -> "AV1"
    CODEC_VP9 -> "VP9"
    CODEC_AVC -> "AVC"
    else -> "AUTO"
}
