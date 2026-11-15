package io.github.nexalloy.morphe.youtube.misc.refreshrate

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.WindowManager
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.patches.BaseAppRefreshRatePatch
import app.morphe.extension.shared.settings.preference.AppRefreshRateListPreference
import app.morphe.extension.youtube.shared.PlayerType
import app.morphe.extension.youtube.shared.VideoState
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.ref.WeakReference

/**
 * App refresh rate.
 *
 * The actual implementation lives in the extension
 * ([BaseAppRefreshRatePatch]) and is the same code upstream Morphe uses.
 * Upstream injects `setActivityRefreshRate` into every activity's onCreate with smali patches;
 * here the injection is done with Xposed hooks on YouTube's main activity instead.
 *
 * Note: `BaseAppRefreshRatePatch.isPatchIncluded()` is hardcoded to true in our extension fork,
 * because the upstream patcher rewrites that method during patching and we do not run the patcher.
 */
val AppRefreshRate = patch(
    name = "App refresh rate",
    description = "Adds options to force the display refresh rate for the app, optionally only during video playback.",
    settings = {
        PreferenceScreen.GENERAL.addPreferences(
            PreferenceCategory(
                key = "morphe_app_refresh_rate_category",
                titleKey = "morphe_app_refresh_rate_category_title",
                preferences = setOf(
                    // The extension preference builds its entries from the display modes that were
                    // actually reported by the device, and stores "DEFAULT" when no override is set.
                    NonInteractivePreference(
                        key = "morphe_app_refresh_rate",
                        summaryKey = null,
                        tag = AppRefreshRateListPreference::class.java,
                        selectable = true,
                    ),
                    ListPreference("morphe_app_refresh_rate_type"),
                )
            )
        )
    },
) {
    fun applyToActivity(activity: Activity) {
        Logger.printDebug { "AppRefreshRate: applying to ${activity.javaClass.simpleName}" }
        try {
            BaseAppRefreshRatePatch.setActivityRefreshRate(activity)
        } catch (ex: Throwable) {
            Logger.printException({ "AppRefreshRate: failed to set the activity refresh rate" }, ex)
        }
    }

    // The settings screen lives inside the same activity, so changing the value does not trigger
    // onCreate/onResume of the main activity. Without this listener the new rate was only applied
    // after a full app restart.
    var currentActivity: WeakReference<Activity>? = null

    // SharedPreferences stores its listeners in a WeakHashMap, so a strong reference is required.
    var preferenceListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun reapplyToCurrentActivity() {
        val activity = currentActivity?.get() ?: return
        applyToActivity(activity)
    }

    fun registerPreferenceListener(activity: Activity) {
        if (preferenceListener != null) return
        try {
            val preferences = activity.applicationContext
                .getSharedPreferences("morphe_prefs", Context.MODE_PRIVATE)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == REFRESH_RATE_KEY || key == REFRESH_RATE_TYPE_KEY) {
                    Logger.printDebug { "AppRefreshRate: $key changed, re-applying" }
                    // Deferred: the preference screen updates the in-memory Setting after the
                    // SharedPreferences listener chain has run, so read the value a tick later.
                    Utils.runOnMainThread { reapplyToCurrentActivity() }
                }
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            preferenceListener = listener
        } catch (ex: Throwable) {
            Logger.printException({ "AppRefreshRate: failed to register the preference listener" }, ex)
        }
    }

    // Applied after super.onCreate/onResume, so the window is attached and the display is ready.
    // Every activity is hooked (not only the main one): the settings screen and other activities
    // live in the same process and otherwise kept running at the system refresh rate.
    fun handleActivity(activity: Activity, scheduleRetries: Boolean) {
        currentActivity = WeakReference(activity)
        registerPreferenceListener(activity)
        applyToActivity(activity)

        if (!scheduleRetries) return

        // The window is attached and gets its final parameters only after the first frames,
        // so apply again shortly after the activity became visible.
        val activityRef = WeakReference(activity)
        for (delay in REAPPLY_DELAYS_MS) {
            Utils.runOnMainThreadDelayed({
                activityRef.get()?.let { applyToActivity(it) }
            }, delay)
        }
    }

    safeHook("Activity.onCreate") {
        DexMethod("Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V").hookMethod {
            after {
                val activity = it.thisObject as? Activity ?: return@after
                handleActivity(activity, scheduleRetries = true)
            }
        }
    }

    // The concrete activity is hooked as well: it is the one that is guaranteed to exist in the
    // app class loader and it is what upstream patches inject into.
    safeHook("MainActivity.onCreate") {
        DexMethod("$YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE->onCreate(Landroid/os/Bundle;)V").hookMethod {
            after {
                val activity = it.thisObject as? Activity ?: return@after
                handleActivity(activity, scheduleRetries = true)
            }
        }
    }

    safeHook("Activity.onResume") {
        DexMethod("Landroid/app/Activity;->onResume()V").hookMethod {
            after {
                val activity = it.thisObject as? Activity ?: return@after
                handleActivity(activity, scheduleRetries = true)
            }
        }
    }

    // The window parameters are final only after the first layout/focus, and the host app
    // rewrites them while scrolling, so re-apply whenever the window gains focus.
    safeHook("Activity.onWindowFocusChanged") {
        DexMethod("Landroid/app/Activity;->onWindowFocusChanged(Z)V").hookMethod {
            after {
                if (it.args[0] as? Boolean != true) return@after
                val activity = it.thisObject as? Activity ?: return@after
                applyToActivity(activity)
            }
        }
    }

    // The host app overwrites its window attributes (home feed, player start, scrolling), which
    // dropped the override until the next resume. Re-inject the preferred display mode into every
    // attribute update. Our own calls go through these hooks too, but they are idempotent.
    safeHook("PhoneWindow.setAttributes") {
        DexMethod("Lcom/android/internal/policy/PhoneWindow;->setAttributes(Landroid/view/WindowManager\$LayoutParams;)V").hookMethod {
            before {
                (it.args[0] as? WindowManager.LayoutParams)?.let { params ->
                    BaseAppRefreshRatePatch.applyToWindowAttributes(params)
                }
            }
        }
    }

    safeHook("WindowManagerImpl.updateViewLayout") {
        DexMethod("Landroid/view/WindowManagerImpl;->updateViewLayout(Landroid/view/View;Landroid/view/ViewGroup\$LayoutParams;)V").hookMethod {
            before {
                (it.args[1] as? WindowManager.LayoutParams)?.let { params ->
                    BaseAppRefreshRatePatch.applyToWindowAttributes(params)
                }
            }
        }
    }

    // The player and the feed request frame rates for their surfaces (120 FPS for scrolling,
    // the video frame rate during playback). Those requests win over the window mode while the
    // content is moving, which is why the override only held while the screen was idle.
    safeHook("Surface.setFrameRate") {
        val replaceFrameRate = { param: de.robv.android.xposed.XC_MethodHook.MethodHookParam ->
            BaseAppRefreshRatePatch.trackSurface(param.thisObject as? android.view.Surface)
            val requestedFrameRate = param.args[0] as? Float
            if (requestedFrameRate != null) {
                val frameRate = BaseAppRefreshRatePatch.overrideSurfaceFrameRate(requestedFrameRate)
                if (frameRate != requestedFrameRate) {
                    param.args[0] = frameRate
                    // Force the rate: with the default compatibility mode the display is still
                    // allowed to run faster for animations and scrolling.
                    if (param.args.size > 1) param.args[1] = 1 // FRAME_RATE_COMPATIBILITY_FIXED_SOURCE
                    if (param.args.size > 2) param.args[2] = 1 // CHANGE_FRAME_RATE_ALWAYS
                }
            }
        }
        DexMethod("Landroid/view/Surface;->setFrameRate(FI)V").hookMethod {
            before { replaceFrameRate(it) }
        }
        DexMethod("Landroid/view/Surface;->setFrameRate(FII)V").hookMethod {
            before { replaceFrameRate(it) }
        }
    }

    // Re-apply when playback starts or stops, so "portrait/fullscreen playback" modes work.
    // These observers are registered here instead of calling AppRefreshRatePatch.initialize(),
    // which bails out when the setting is still default at app startup.
    VideoState.getOnChange().addObserver { state ->
        val type = PlayerType.getCurrent()
        updatePlayerIsActive(type, state)
    }

    PlayerType.getOnChange().addObserver { type ->
        val state = VideoState.getCurrent() ?: return@addObserver
        updatePlayerIsActive(type, state)
    }
}

private val REAPPLY_DELAYS_MS = longArrayOf(400L, 1200L, 3000L)

/**
 * Hooks a framework method without aborting the rest of the patch when the method
 * is missing on this Android version.
 */
private fun safeHook(name: String, block: () -> Unit) {
    try {
        block()
    } catch (ex: Throwable) {
        Logger.printException({ "AppRefreshRate: failed to hook $name" }, ex)
    }
}

private const val REFRESH_RATE_KEY = "morphe_app_refresh_rate"
private const val REFRESH_RATE_TYPE_KEY = "morphe_app_refresh_rate_type"

private fun updatePlayerIsActive(type: PlayerType, state: VideoState) {
    try {
        val isPlaying = state == VideoState.PLAYING
        BaseAppRefreshRatePatch.setVideoPlayerIsActive(
            isPlaying && type == PlayerType.WATCH_WHILE_MAXIMIZED,
            isPlaying && type == PlayerType.WATCH_WHILE_FULLSCREEN,
        )
    } catch (ex: Throwable) {
        Logger.printException({ "AppRefreshRate: failed to update the player state" }, ex)
    }
}
