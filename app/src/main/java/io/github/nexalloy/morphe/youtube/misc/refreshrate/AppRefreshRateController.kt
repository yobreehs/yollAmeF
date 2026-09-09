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
 * Reads the module settings from the module's `<targetPackage>.xml` file (the file the settings
 * UI writes to, see [HomeShortsFilter] for the same approach) once at startup.
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

    private val enabledTargetRate: Int? by lazy {
        val prefs = runCatching {
            XSharedPreferences(BuildConfig.APPLICATION_ID, Utils.getContext().packageName)
        }.getOrNull() ?: return@lazy null

        val rate = prefs.getInt("morphe_app_refresh_rate", 0)
        if (rate > 0) rate else null
    }

    private val refreshRateType: RefreshRateType by lazy {
        val prefs = runCatching {
            XSharedPreferences(BuildConfig.APPLICATION_ID, Utils.getContext().packageName)
        }.getOrNull()
        val typeString = prefs?.getString("morphe_app_refresh_rate_type", "ALWAYS")
        runCatching { RefreshRateType.valueOf(typeString ?: "ALWAYS") }.getOrDefault(RefreshRateType.ALWAYS)
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
     * Injection point: called on the main activity creation.
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
        val targetRate = enabledTargetRate ?: return

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
        val shouldOverride = refreshRateType.appliesTo(playbackPortrait, playbackFullscreen)

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