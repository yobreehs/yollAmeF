package io.github.nexalloy.morphe.youtube.misc.refreshrate

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.Window
import android.view.WindowManager
import app.morphe.extension.shared.Utils
import de.robv.android.xposed.XSharedPreferences
import io.github.nexalloy.BuildConfig

/**
 * Sets a preferred display refresh rate on the activity window.
 *
 * Reads the module settings fresh on every application (the settings are edited in the same
 * process, in the target app's "morphe_prefs" file), so a change takes effect immediately
 * instead of only after a process restart.
 */
object AppRefreshRateController {

    private enum class RefreshRateType {
        ALWAYS,
        PORTRAIT,
        FULLSCREEN,
        PORTRAIT_FULLSCREEN;

        fun appliesTo(portrait: Boolean, fullscreen: Boolean): Boolean = when (this) {
            ALWAYS -> true
            PORTRAIT -> portrait
            FULLSCREEN -> fullscreen
            PORTRAIT_FULLSCREEN -> portrait || fullscreen
        }
    }

    private fun readEnabledTargetRate(): Int? {
        // The in-app refresh rate preference (RefreshRatePreference) stores the value in the
        // target app's "morphe_prefs" as a string. Fall back to the module prefs where older
        // builds stored it.
        val fromInAppPrefs = runCatching {
            Utils.getContext().getSharedPreferences("morphe_prefs", Context.MODE_PRIVATE)
                .getString("morphe_app_refresh_rate", null)?.toIntOrNull()
        }.getOrNull() ?: 0
        val modulePrefs = runCatching {
            XSharedPreferences(BuildConfig.APPLICATION_ID, Utils.getContext().packageName)
                .getInt("morphe_app_refresh_rate", 0)
        }.getOrDefault(0)

        val rate = if (fromInAppPrefs > 0) fromInAppPrefs else modulePrefs
        return rate.takeIf { it > 0 }
    }

    private fun readRefreshRateType(): RefreshRateType {
        val typeString = runCatching {
            Utils.getContext().getSharedPreferences("morphe_prefs", Context.MODE_PRIVATE)
                .getString("morphe_app_refresh_rate_type", "ALWAYS")
        }.getOrDefault("ALWAYS")
        return runCatching { RefreshRateType.valueOf(typeString ?: "ALWAYS") }
            .getOrDefault(RefreshRateType.ALWAYS)
    }

    @Volatile
    private var preferredDisplayModeId: Int? = null

    @Volatile
    private var preferredRefreshRate: Float? = null

    @Volatile
    private var playbackPortrait = false

    @Volatile
    private var playbackFullscreen = false

    private val trackedWindows = mutableSetOf<Window>()

    /**
     * Injection point: called on the main activity creation and resume.
     */
    @JvmStatic
    fun initialize(activity: Activity) {
        setWindowRefreshRate(activity, activity.window)
    }

    @JvmStatic
    fun setPlayerIsActive(portrait: Boolean, fullscreen: Boolean) {
        playbackPortrait = portrait
        playbackFullscreen = fullscreen
        synchronized(trackedWindows) {
            trackedWindows.toList().forEach { applyRefreshRateToWindow(it) }
        }
    }

    private fun setWindowRefreshRate(context: Context, window: Window?) {
        if (window == null) return
        val targetRate = readEnabledTargetRate() ?: return

        runCatching {
            if (preferredDisplayModeId == null) {
                val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display
                } else {
                    (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                }
                if (display == null) return@runCatching

                val supportedModes = display.supportedModes ?: return@runCatching
                val currentMode = display.mode
                val resolutionModes = supportedModes.filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                            it.physicalHeight == currentMode.physicalHeight
                }

                val bestMode = resolutionModes
                    .filter { Math.round(it.refreshRate) <= targetRate }
                    .maxByOrNull { it.refreshRate }

                if (bestMode != null) {
                    preferredDisplayModeId = bestMode.modeId
                    preferredRefreshRate = bestMode.refreshRate
                }
            }

            synchronized(trackedWindows) {
                trackedWindows.add(window)
            }
            applyRefreshRateToWindow(window)
        }
    }

    private fun applyRefreshRateToWindow(window: Window) {
        val shouldOverride = readRefreshRateType().appliesTo(playbackPortrait, playbackFullscreen)

        val params = window.attributes
        if (shouldOverride && preferredDisplayModeId != null && preferredDisplayModeId!! > 0) {
            params.preferredDisplayModeId = preferredDisplayModeId!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && preferredRefreshRate != null) {
                params.preferredRefreshRate = preferredRefreshRate!!
            }
        } else {
            params.preferredDisplayModeId = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                params.preferredRefreshRate = 0f
            }
        }
        window.attributes = params
    }
}