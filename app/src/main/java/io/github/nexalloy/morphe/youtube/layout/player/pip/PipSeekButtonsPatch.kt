package io.github.nexalloy.morphe.youtube.layout.player.pip

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.settings.BooleanSetting
import app.morphe.extension.shared.settings.Setting.Availability
import app.morphe.extension.youtube.patches.VideoInformation
import app.morphe.extension.youtube.settings.Settings
import io.github.nexalloy.IHookCallback
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.ref.WeakReference

private const val ACTION_SEEK_BACK = "io.github.nexalloy.morphe.PIP_SEEK_BACK"
private const val ACTION_SEEK_FORWARD = "io.github.nexalloy.morphe.PIP_SEEK_FORWARD"
private const val SEEK_OFFSET_MS = 5_000L
private const val SEEK_BACK_TITLE = "-5 s"
private const val SEEK_FORWARD_TITLE = "+5 s"

/** Restore YouTube's own PiP buttons when less than this amount of video is left. */
private const val NEAR_END_THRESHOLD_MS = 30_000L

private const val RESTORE_NEAR_END_KEY = "morphe_pip_seek_buttons_restore_near_end"

/**
 * Setting backing the "restore near end" toggle. Created lazily so it also exists when the
 * patch is disabled but its settings rows are still shown.
 */
private val restoreNearEndSetting by lazy {
    BooleanSetting(
        RESTORE_NEAR_END_KEY,
        false,
        Availability { Settings.PIP_SEEK_BUTTONS.get() },
    )
}

/**
 * Adds rewind/forward (±5 seconds) buttons to the Picture-in-picture window.
 *
 * The buttons are injected into the actions of the Picture-in-picture params that YouTube builds.
 * Each action posts a broadcast that is handled in the YouTube process and seeks through the
 * regular [VideoInformation] playback controller, so the buttons keep working while the app is
 * in the background / PiP.
 */
val PipSeekButtons = patch(
    name = "Picture-in-picture seek buttons",
    description = "Adds rewind and forward (±5 seconds) buttons to the Picture-in-picture window.",
    settings = {
        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("morphe_pip_seek_buttons", summary = true),
            SwitchPreference(RESTORE_NEAR_END_KEY, summary = true),
        )
    },
) {
    dependsOn(VideoInformationPatch)

    val context = appContext

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            if (!Settings.PIP_SEEK_BUTTONS.get()) return

            Logger.printDebug {
                "PipSeekButtons: receiver action=${intent?.action} " +
                        "videoTime=${VideoInformation.getVideoTime()}"
            }

            when (intent?.action) {
                ACTION_SEEK_BACK -> VideoInformation.seekToRelative(-SEEK_OFFSET_MS)
                ACTION_SEEK_FORWARD -> VideoInformation.seekToRelative(SEEK_OFFSET_MS)
            }
        }
    }

    val filter = IntentFilter(ACTION_SEEK_BACK).apply { addAction(ACTION_SEEK_FORWARD) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(receiver, filter)
    }

    fun pendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(action).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    val seekBackAction by lazy {
        RemoteAction(
            Icon.createWithResource("android", android.R.drawable.ic_media_rew),
            SEEK_BACK_TITLE,
            SEEK_BACK_TITLE,
            pendingIntent(ACTION_SEEK_BACK, 1),
        )
    }

    val seekForwardAction by lazy {
        RemoteAction(
            Icon.createWithResource("android", android.R.drawable.ic_media_ff),
            SEEK_FORWARD_TITLE,
            SEEK_FORWARD_TITLE,
            pendingIntent(ACTION_SEEK_FORWARD, 2),
        )
    }

    /**
     * Merges the seek buttons into the actions YouTube provides.
     *
     * Android only shows a handful of PiP actions (usually three). App-provided actions take
     * precedence over the media-session controls, so:
     * - an empty list (YouTube clears/initializes the params) is left untouched, otherwise the
     *   seek buttons would hide the media-session play/pause until YouTube sets its own actions;
     * - for a full list the first and last actions are replaced, keeping the middle one
     *   (YouTube's play/pause).
     */
    fun mergeSeekActions(actions: List<RemoteAction>): List<RemoteAction> {
        when (actions.size) {
            0 -> return actions
            1 -> return listOf(seekBackAction, seekForwardAction, actions[0])
            else -> {
                val merged = actions.toMutableList()
                merged[0] = seekBackAction
                merged[merged.lastIndex] = seekForwardAction
                return merged
            }
        }
    }

    /** Milliseconds left until the end of the video, or -1 when unknown. */
    fun remainingMs(): Long = runCatching {
        val length = VideoInformation.getVideoLength()
        if (length <= 0) -1 else length - VideoInformation.getVideoTime()
    }.getOrDefault(-1)

    fun restoreOriginalNearEnd(actions: List<RemoteAction>): Boolean =
        restoreNearEndSetting.get() && remainingMs() in 0 until NEAR_END_THRESHOLD_MS && actions.isNotEmpty()

    // Dynamic ("on the fly") switch: while the video is in PiP and the seek buttons are shown,
    // re-apply YouTube's own actions when the last 30 seconds begin. YouTube does not refresh
    // the PiP params by itself at that point.
    val handler = Handler(Looper.getMainLooper())
    var pendingRestore: Runnable? = null
    var restoringOriginal = false
    var lastOriginalActions: List<RemoteAction> = emptyList()
    var lastActivity: WeakReference<Activity>? = null

    fun scheduleRestore(activity: Activity?) {
        pendingRestore?.let { handler.removeCallbacks(it) }
        pendingRestore = null
        if (activity != null) lastActivity = WeakReference(activity)
        if (!Settings.PIP_SEEK_BUTTONS.get() || !restoreNearEndSetting.get()) return

        val remaining = remainingMs()
        if (remaining <= 0) return
        val delay = (remaining - NEAR_END_THRESHOLD_MS).coerceAtLeast(0) + 200

        val runnable = object : Runnable {
            override fun run() {
                try {
                    if (!Settings.PIP_SEEK_BUTTONS.get() || !restoreNearEndSetting.get()) return
                    val currentRemaining = remainingMs()
                    // Seeked backwards, keep the seek buttons and check again later.
                    if (currentRemaining > NEAR_END_THRESHOLD_MS) {
                        scheduleRestore(lastActivity?.get())
                        return
                    }
                    val pipActivity = lastActivity?.get() ?: return
                    if (!pipActivity.isInPictureInPictureMode || lastOriginalActions.isEmpty()) return

                    Logger.printDebug {
                        "PipSeekButtons: restoring original buttons, remaining=${currentRemaining}ms"
                    }
                    restoringOriginal = true
                    pipActivity.setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setActions(lastOriginalActions)
                            .build()
                    )
                } catch (e: Throwable) {
                    Logger.printException({ "PipSeekButtons: failed to restore original actions" }, e)
                } finally {
                    restoringOriginal = false
                }
            }
        }
        pendingRestore = runnable
        handler.postDelayed(runnable, delay)
    }

    fun logActions(stage: String, actions: List<RemoteAction>) {
        Logger.printDebug {
            "PipSeekButtons: $stage actions=${actions.size} " +
                    "titles=${actions.joinToString { it.title.toString() }}"
        }
    }

    // Primary hook: replace the outer actions of whatever list YouTube passes to the PiP builder
    // so the seek buttons always fit in the limited number of visible PiP action slots.
    // Works on every API level, as the builder's existing list is preserved by the app itself.
    runCatching {
        DexMethod(
            "Landroid/app/PictureInPictureParams\$Builder;->setActions(Ljava/util/List;)" +
                    "Landroid/app/PictureInPictureParams\$Builder;"
        ).hookMethod {
            before { param ->
                if (!Settings.PIP_SEEK_BUTTONS.get() || restoringOriginal) return@before

                @Suppress("UNCHECKED_CAST")
                val actions = param.args[0] as? List<RemoteAction> ?: return@before
                if (actions.isNotEmpty()) lastOriginalActions = actions
                logActions("builder.setActions in", actions)

                if (restoreOriginalNearEnd(actions)) {
                    Logger.printDebug {
                        "PipSeekButtons: near the end, keeping original buttons " +
                                "(remaining=${remainingMs()}ms)"
                    }
                    return@before
                }

                val merged = mergeSeekActions(actions)
                logActions("builder.setActions out", merged)
                param.args[0] = merged
            }
        }
    }.onFailure { e ->
        Logger.printException({ "PipSeekButtons: failed to hook the PiP actions builder" }, e)
    }

    // Fallback hook: some entry points set the params directly without calling setActions again.
    // Reading the current actions requires API 33 (Android 13), so it is only used there.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val addToParams: IHookCallback = { param ->
            if (Settings.PIP_SEEK_BUTTONS.get() && !restoringOriginal) {
                val activity = param.thisObject as? Activity
                val params = param.args[0] as? PictureInPictureParams
                if (params != null) {
                    logActions("pipParams in", params.actions)

                    if (restoreOriginalNearEnd(params.actions)) {
                        Logger.printDebug {
                            "PipSeekButtons: near the end, keeping original buttons " +
                                    "(remaining=${remainingMs()}ms)"
                        }
                    } else {
                        val merged = mergeSeekActions(params.actions)
                        if (merged.map { it.title } != params.actions.map { it.title }) {
                            logActions("pipParams out", merged)
                            param.args[0] = PictureInPictureParams.Builder(params)
                                .setActions(merged)
                                .build()
                        }
                        // Re-arm the on-the-fly switch for the last 30 seconds.
                        scheduleRestore(activity)
                    }
                }
            }
        }

        runCatching {
            DexMethod(
                "Landroid/app/Activity;->setPictureInPictureParams(Landroid/app/PictureInPictureParams;)V"
            ).hookMethod {
                before(addToParams)
            }

            DexMethod(
                "Landroid/app/Activity;->enterPictureInPictureMode(Landroid/app/PictureInPictureParams;)Z"
            ).hookMethod {
                before(addToParams)
            }
        }.onFailure { e ->
            Logger.printException({ "PipSeekButtons: failed to hook the PiP params entry points" }, e)
        }
    }
}
