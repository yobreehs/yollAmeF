package io.github.nexalloy.morphe.youtube.misc.settings

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.WindowInsets
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.settings.preference.ImportExportPreference
import app.morphe.extension.shared.settings.preference.about.MorpheAboutPreference
import app.morphe.extension.youtube.settings.YouTubeActivityHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.R
import io.github.nexalloy.invokeOriginalMethod
import io.github.nexalloy.morphe.shared.misc.initialization.initializationPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.shared.settings.preferences
import io.github.nexalloy.morphe.youtube.layout.buttons.overlay.PlayerOverlayButtonsSettings
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.ref.WeakReference

@Suppress("UNREACHABLE_CODE")
val SettingsHook = patch(
    name = "<SettingsHook>"
) {
    dependsOn(
        PlayerOverlayButtonsSettings,
        initializationPatch()
    )

    // The injected settings activity ("Morphe settings"), used to scope the dialog
    // inset fallback below to the settings submenu dialogs only.
    var settingsActivityRef: WeakReference<Activity> = WeakReference(null)

    ::PreferenceFragmentCompat_addPreferencesFromResource.hookMethod {
        val settings_fragment = ResourceUtils.getXmlIdentifier("settings_fragment")
        val settings_fragment_cairo = ResourceUtils.getXmlIdentifier("settings_fragment_cairo")
        before { param ->
            val xml = when (param.args[0] as Int) {
                0 -> return@before
                settings_fragment -> R.xml.yt_morphe_settings
                settings_fragment_cairo -> R.xml.yt_morphe_settings_cairo
                else -> return@before
            }

            param.invokeOriginalMethod(arrayOf(xml))
        }
    }

    val superOnCreateMethod = ::licenseActivitySuperOnCreate.method
    val superOnCreate = xposed.getInvoker(superOnCreateMethod)

    ::licenseActivityOnCreateFingerprint.hookMethod(object : XC_MethodReplacement() {
        override fun replaceHookedMethod(param: XC_MethodHook.MethodHookParam) {
            val activity = param.thisObject as Activity
            settingsActivityRef = WeakReference(activity)
            YouTubeActivityHook.initialize(activity)
            activity.theme.applyStyle(R.style.ListDividerNull, true)
            superOnCreate.invokeSpecial(param.thisObject, *param.args)
            applySettingsInsets(activity)
        }
    })

    // FemAlloy safety net: the extension only applies system-bar insets to submenu dialogs
    // whose PreferenceScreen is a direct child of the root screen. Screens nested inside
    // PreferenceCategory chapters ("New features" → Player → Ambient mode, refresh rate,
    // channel search, ...) are missed on builds without the submodule fix, so every settings
    // submenu dialog is padded here as well.
    DexMethod("Landroid/app/Dialog;->show()V").hookMethod {
        after { param ->
            applyDialogInsets(param.thisObject as Dialog, settingsActivityRef)
        }
    }

    // Remove other methods as they will break as the onCreate method is modified above.
    ::licenseActivityNOTonCreate.dexMethodList.forEach {
        if (it.returnTypeName == "void") it.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    // Update shared dark mode status based on YT theme.
    // This is needed because YT allows forcing light/dark mode
    // which then differs from the system dark mode status.
    ::setThemeFingerprint.hookMethod {
        after { param ->
            YouTubeActivityHook.updateLightDarkModeStatus(param.result as Enum<*>)
        }
    }

    // Add an "About" preference to the top.
    preferences += NonInteractivePreference(
        key = "morphe_settings_screen_00_about",
        icon = "@drawable/morphe_settings_screen_00_about",
        iconBold = "@drawable/morphe_settings_screen_00_about_bold",
        layout = "@layout/preference_with_icon",
        summaryKey = null,
        tag = MorpheAboutPreference::class.java,
        selectable = true,
    )

//    if (!is_21_30_or_greater) {
//        PreferenceScreen.GENERAL.addPreferences(
//            SwitchPreference("morphe_restore_old_settings_menus")
//        )
//    }

//    PreferenceScreen.GENERAL.addPreferences(
//        SwitchPreference("morphe_settings_search_history"),
//        SwitchPreference("morphe_show_menu_icons")
//    )

    PreferenceScreen.MISC.addPreferences(
        TextPreference(
            key = null,
            titleKey = "morphe_pref_import_export_title",
            summaryKey = "morphe_pref_import_export_summary",
            inputType = InputType.TEXT_MULTI_LINE,
            tag = ImportExportPreference::class.java,
        ),
//        ListPreference(
//            key = "morphe_language",
//            tag = SortedListPreference::class.java
//        )
    )

    PreferenceScreen.close()
}

object PreferenceScreen : BasePreferenceScreen() {
    // Sort screens in the root menu by key, to not scatter related items apart
    // (sorting key is set in morphe_prefs.xml).
    // If no preferences are added to a screen, the screen will not be added to the settings.
    val ADS = Screen(
        key = "morphe_settings_screen_01_ads",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_01_ads",
        iconBold = "@drawable/morphe_settings_screen_01_ads_bold",
        layout = "@layout/preference_with_icon",
    )
    val ALTERNATIVE_THUMBNAILS = Screen(
        key = "morphe_settings_screen_02_alt_thumbnails",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_02_alt_thumbnails",
        iconBold = "@drawable/morphe_settings_screen_02_alt_thumbnails_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.UNSORTED,
    )
    val FEED = Screen(
        key = "morphe_settings_screen_03_feed",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_03_feed",
        iconBold = "@drawable/morphe_settings_screen_03_feed_bold",
        layout = "@layout/preference_with_icon",
    )
    val GENERAL = Screen(
        key = "morphe_settings_screen_04_general",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_04_general",
        iconBold = "@drawable/morphe_settings_screen_04_general_bold",
        layout = "@layout/preference_with_icon",
    )
    val PLAYER = Screen(
        key = "morphe_settings_screen_05_player",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_05_player",
        iconBold = "@drawable/morphe_settings_screen_05_player_bold",
        layout = "@layout/preference_with_icon",
    )
    val SHORTS = Screen(
        key = "morphe_settings_screen_06_shorts",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_06_shorts",
        iconBold = "@drawable/morphe_settings_screen_06_shorts_bold",
        layout = "@layout/preference_with_icon",
    )
    val SEEKBAR = Screen(
        key = "morphe_settings_screen_07_seekbar",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_07_seekbar",
        iconBold = "@drawable/morphe_settings_screen_07_seekbar_bold",
        layout = "@layout/preference_with_icon",
    )
    val SWIPE_CONTROLS = Screen(
        key = "morphe_settings_screen_08_swipe_controls",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_08_swipe_controls",
        iconBold = "@drawable/morphe_settings_screen_08_swipe_controls_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.UNSORTED,
    )
    val RETURN_YOUTUBE_DISLIKE = Screen(
        key = "morphe_settings_screen_09_return_youtube_dislike",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_09_return_youtube_dislike",
        iconBold = "@drawable/morphe_settings_screen_09_return_youtube_dislike_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.UNSORTED,
    )
    val SPONSORBLOCK = Screen(
        key = "morphe_settings_screen_10_sponsorblock",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_10_sponsorblock",
        iconBold = "@drawable/morphe_settings_screen_10_sponsorblock_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.UNSORTED,
    )
    val MISC = Screen(
        key = "morphe_settings_screen_11_misc",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_11_misc",
        iconBold = "@drawable/morphe_settings_screen_11_misc_bold",
        layout = "@layout/preference_with_icon",
    )
    val VIDEO = Screen(
        key = "morphe_settings_screen_12_video",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_12_video",
        iconBold = "@drawable/morphe_settings_screen_12_video_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.BY_KEY,
    )

    val NEW_PATCHES = Screen(
        key = "morphe_settings_screen_13_new_patches",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_13_new_patches",
        iconBold = "@drawable/morphe_settings_screen_13_new_patches_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.BY_KEY,
    )

    val DEBUG = Screen(
        key = "morphe_settings_screen_14_debug",
        summaryKey = null,
        icon = "@drawable/morphe_settings_screen_14_debug",
        iconBold = "@drawable/morphe_settings_screen_14_debug_bold",
        layout = "@layout/preference_with_icon",
        sorting = Sorting.BY_KEY,
    )

    override fun commit(screen: PreferenceScreenPreference) {
        preferences += screen
    }
}

/**
 * Android 15+ (YouTube 21.39) enforces edge-to-edge, so the injected settings activity content
 * is drawn behind the system bars. The extension only applies insets to submenu dialogs, so the
 * root screen is padded here instead.
 *
 * The padding is applied through three redundant paths to survive every timing/ordering quirk of
 * the window insets dispatch:
 *  1. synchronously from the current [android.view.WindowInsets] if already available,
 *  2. via an [View.OnLayoutChangeListener] for dynamic inset changes,
 *  3. via a global layout listener that re-applies the padding on every layout pass
 *     (idempotent — [View.setPadding] short-circuits when values are unchanged).
 */
private fun applySettingsInsets(activity: Activity) {
    runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@runCatching
        val window = activity.window ?: return@runCatching
        val decorView = window.decorView ?: return@runCatching

        decorView.post {
            val content = activity.findViewById<View>(android.R.id.content) ?: return@post

            fun applyFrom(insets: WindowInsets?) {
                if (insets == null) return
                val status = insets.getInsets(WindowInsets.Type.statusBars())
                val nav = insets.getInsets(WindowInsets.Type.navigationBars())
                val cutout = insets.getInsets(WindowInsets.Type.displayCutout())
                content.setPadding(cutout.left, status.top, cutout.right, nav.bottom)
                Logger.printDebug {
                    "Settings insets: status=${status.top} nav=${nav.bottom} " +
                        "cutout=(${cutout.left},${cutout.right}) padding=${content.paddingTop}"
                }
            }

            // 1) Apply immediately from the current insets, if already dispatched.
            applyFrom(decorView.rootWindowInsets)

            // 2) Keep padding up to date when insets change.
            content.setOnApplyWindowInsetsListener { _, insets ->
                applyFrom(insets)
                insets
            }

            // 3) Fallback: re-apply on every layout pass.
            decorView.viewTreeObserver.addOnGlobalLayoutListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    applyFrom(decorView.rootWindowInsets)
                }
            }

            decorView.requestApplyInsets()
        }
    }
}

/**
 * FemAlloy safety net for submenu dialogs of the injected settings screen.
 *
 * The extension's toolbar/insets handling only walks PreferenceScreens that are direct children
 * of the root screen, so screens nested inside PreferenceCategory chapters ("New features" →
 * Player → Ambient mode, ...) open without system bar padding on builds without the submodule
 * fix. This hooks every Dialog shown from the settings activity and pads the dialog's content
 * root using the same redundant paths as [applySettingsInsets].
 *
 * Only dialogs that expose a preference list (android.R.id.list) are touched, so unrelated
 * dialogs (restart confirmation, import/export, log viewer, ...) are left untouched.
 */
private fun applyDialogInsets(dialog: Dialog, settingsActivityRef: WeakReference<Activity>) {
    runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@runCatching
        val activity = settingsActivityRef.get() ?: return@runCatching
        val dialogActivity = unwrapActivity(dialog.context) ?: return@runCatching
        if (dialogActivity !== activity) return@runCatching

        val window = dialog.window ?: return@runCatching
        val decorView = window.decorView ?: return@runCatching
        // Legacy PreferenceScreen submenu dialogs expose the list under android.R.id.list.
        if (decorView.findViewById<View>(android.R.id.list) == null) return@runCatching
        val root = decorView.findViewById<View>(android.R.id.content)?.parent as? View ?: return@runCatching

        fun applyFrom(insets: WindowInsets?) {
            if (insets == null) return
            val status = insets.getInsets(WindowInsets.Type.statusBars())
            val nav = insets.getInsets(WindowInsets.Type.navigationBars())
            val cutout = insets.getInsets(WindowInsets.Type.displayCutout())
            root.setPadding(cutout.left, status.top, cutout.right, nav.bottom)
            Logger.printDebug {
                "Settings dialog insets: status=${status.top} nav=${nav.bottom} " +
                    "cutout=(${cutout.left},${cutout.right}) padding=${root.paddingTop}"
            }
        }

        // 1) Apply immediately from the current insets, if already dispatched.
        applyFrom(decorView.rootWindowInsets)

        // 2) Keep padding up to date when insets change.
        root.setOnApplyWindowInsetsListener { _, insets ->
            applyFrom(insets)
            insets
        }

        // 3) Fallback: re-apply on every layout pass.
        decorView.viewTreeObserver.addOnGlobalLayoutListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                applyFrom(decorView.rootWindowInsets)
            }
        }

        // Force a fresh insets dispatch in case the window already delivered them.
        root.requestApplyInsets()
    }
}

/**
 * Walks ContextWrapper chains (e.g. dialog contexts created from an activity with the activity
 * theme applied) up to the enclosing Activity, or null if none.
 */
private fun unwrapActivity(context: Context): Activity? {
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? Activity
}
