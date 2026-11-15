package io.github.nexalloy.morphe.youtube.layout.buttons.navigation.glass

import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.ResourceType
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.settings.preference.SeekBarPreference
import app.morphe.extension.shared.ui.glass.GlassCapsuleDrawable
import app.morphe.extension.youtube.patches.NavigationBarPatch
import app.morphe.extension.youtube.patches.YouTubeGlassNavigationBar
import app.morphe.extension.youtube.settings.Settings
import app.morphe.extension.youtube.settings.preference.YouTubeGlassNavigationBarPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.MethodHookParam
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.youtube.layout.buttons.navigation.NavigationBar
import io.github.nexalloy.morphe.youtube.misc.navigation.NavigationBarAnimationAllControllersFingerprint
import io.github.nexalloy.morphe.youtube.misc.navigation.NavigationBarAnimationFingerprint
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

private const val PIVOT_BAR_CLASS =
    "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"

/**
 * Feature flag of the translucent navigation bar background. The glass capsule keeps the
 * background the app draws and clips it to the capsule, so without this flag the capsule has
 * only the tinted fill and no blurred content behind it.
 */
private const val TRANSLUCENT_NAVIGATION_BAR_BACKGROUND_FLAG = 45630927L

/**
 * Feature flag that lays the pages out behind the navigation bar. Needed for the blur: otherwise
 * the bar would blur the window background instead of the feed.
 */
private const val FEED_BEHIND_NAVIGATION_BAR_FLAG = 45631257L

/**
 * Feature flag that makes the navigation bar cover the system navigation buttons. Upstream turns
 * this on for the translucent style on devices where the window is drawn behind them.
 */
private const val NAVIGATION_BAR_COVERS_SYSTEM_BUTTONS_FLAG = 45632194L

/**
 * Feature flag of the translucent style itself. Only needed before Android 12, where it is what
 * lays the pages out behind the navigation bar.
 */
private const val TRANSLUCENT_NAVIGATION_BAR_STYLE_FLAG = 45633329L

/**
 * Xposed port of the Morphe "Glass navigation bar" feature (`YouTubeGlassNavigationBar`).
 *
 * The patcher version injects overrides into `PivotBar` and into the frosted background drawable.
 * In this module the same extension helpers are called from hooks on the framework methods that
 * the navigation bar uses. The hooks are always installed: the settings are read for every frame,
 * so the glass can be turned on and off in the settings without restarting the app.
 */
val NavigationBarGlass = patch(
    name = "Glass navigation bar",
    description = "Shows the navigation bar as a floating, rounded, translucent capsule "
            + "with a blurred background on Android 14 and later.",
    settings = {
        PreferenceScreen.NEW_PATCHES.addPreferences(
            PreferenceCategory(
                key = "morphe_navigation_bar_glass_category",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("morphe_navigation_bar_glass", summary = true),
                    NonInteractivePreference(
                        key = "morphe_navigation_bar_glass_opacity",
                        tag = SeekBarPreference::class.java,
                        selectable = true,
                    ),
                    NonInteractivePreference(
                        key = "morphe_navigation_bar_glass_height",
                        titleKey = "morphe_navigation_bar_glass_size_title",
                        summaryKey = "morphe_navigation_bar_glass_size_summary",
                        tag = YouTubeGlassNavigationBarPreference::class.java,
                        selectable = true,
                    ),
                    // Also here: the labels belong to the glass layout, but the setting is
                    // shared with the General navigation bar screen.
                    SwitchPreference("morphe_hide_navigation_button_labels", summary = true),
                ),
            ),
        )
    },
) {
    dependsOn(NavigationBar)

    Logger.printDebug {
        "Glass navigation bar hooks installed, enabled: ${Settings.NAVIGATION_BAR_GLASS.get()}"
    }

    val bottomBarContainerId = ResourceUtils.getIdentifier(
        ResourceType.ID, "bottom_bar_container"
    )
    val pivotBarId = ResourceUtils.getIdentifier(ResourceType.ID, "pivot_bar")

    if (bottomBarContainerId == 0 || pivotBarId == 0) {
        Logger.printDebug { "Glass navigation bar resources not found" }
        return@patch
    }

    // Keep the same feature flag decisions as the patcher's translucent navigation bar patch.
    // The blur itself is the app's own, and it is only drawn on Android 14 and later.
    // The overrides are installed unconditionally (the patch is enabled by default), but they
    // only answer when the glass is on, so a bar without the glass keeps the flags of the account.
    insertLiteralOverride(TRANSLUCENT_NAVIGATION_BAR_BACKGROUND_FLAG) { original ->
        if (!Settings.NAVIGATION_BAR_GLASS.get()) {
            original
        } else {
            NavigationBarPatch.useTranslucentNavigationBarBackground(original)
        }
    }
    insertLiteralOverride(NAVIGATION_BAR_COVERS_SYSTEM_BUTTONS_FLAG) { original ->
        if (!Settings.NAVIGATION_BAR_GLASS.get()) {
            original
        } else {
            NavigationBarPatch.useTranslucentNavigationBar(original)
        }
    }
    insertLiteralOverride(FEED_BEHIND_NAVIGATION_BAR_FLAG) { original ->
        if (!Settings.NAVIGATION_BAR_GLASS.get()) {
            original
        } else {
            NavigationBarPatch.useFeedBehindNavigationBar(original)
        }
    }
    insertLiteralOverride(TRANSLUCENT_NAVIGATION_BAR_STYLE_FLAG) { original ->
        if (!Settings.NAVIGATION_BAR_GLASS.get()) {
            original
        } else {
            NavigationBarPatch.useTranslucentNavigationBarStyle(original)
        }
    }

    fun isPivotBar(view: View) = view.id == pivotBarId

    /**
     * Hooks a method that [PIVOT_BAR_CLASS] inherits. `DexMethod.toMethod()` walks the superclass
     * chain, so the hook is installed on the framework implementation, and the callback filters
     * calls that do not belong to the navigation bar.
     */
    fun hookPivotBarMethod(
        name: String,
        parameters: String,
        onPivotBarCall: (MethodHookParam) -> Unit,
    ) {
        DexMethod("$PIVOT_BAR_CLASS->$name$parameters").hookMethod {
            before { param ->
                val view = param.thisObject as? View ?: return@before
                if (view.id == pivotBarId) {
                    onPivotBarCall(param)
                }
            }
        }
    }

    /**
     * The container of the navigation bar above [view], such as the bottom bar container the
     * patcher uses as its injection point.
     */
    fun findBottomBarContainer(view: View): View? {
        var parent: ViewParent? = view.parent
        while (parent is View) {
            if (parent.id == bottomBarContainerId) return parent
            parent = parent.parent
        }
        return view.parent as? View
    }

    // The patcher applies the glass when the bottom bar container is created. The navigation bar
    // is also caught when it is attached to the window, so the glass is applied even for the
    // layouts that never call `addOnLayoutChangeListener` for the container, and so turning the
    // glass on in the settings takes effect without restarting the app.
    hookPivotBarMethod("onAttachedToWindow", "()V") { param ->
        val view = param.thisObject as? View ?: return@hookPivotBarMethod
        try {
            val container = findBottomBarContainer(view) ?: return@hookPivotBarMethod
            Logger.printDebug { "Glass navigation bar attached to window" }
            NavigationBarPatch.setNavigationBarGlass(container)
        } catch (ex: Throwable) {
            Logger.printException { "Glass navigation bar attach failure" }
        }
    }

    // The bottom bar container is created once and holds the PivotBar. This is the same
    // injection point the patcher uses for `setNavigationBarGlass`.
    DexMethod(
        "Landroid/view/View;->addOnLayoutChangeListener(Landroid/view/View\$OnLayoutChangeListener;)V"
    ).hookMethod {
        after { param ->
            val container = param.thisObject as? View ?: return@after
            if (container.id != bottomBarContainerId) return@after

            try {
                val pivotBar = container.findViewById<View>(pivotBarId)
                    ?: return@after

                // Apply the glass after the first layout, when the bar has the padding and the
                // size the app gives it. Applying before that would calculate the capsule from
                // a bar without the room for the system navigation buttons.
                fun applyGlass() {
                    try {
                        NavigationBarPatch.getNavigationBarBottomPadding(
                            pivotBar, pivotBar.paddingBottom
                        )
                        NavigationBarPatch.setNavigationBarGlass(container)
                    } catch (ex: Throwable) {
                        Logger.printException { "Glass navigation bar apply failure" }
                    }
                }

                if (pivotBar.width > 0 && pivotBar.height > 0) {
                    applyGlass()
                } else {
                    val listener = object : View.OnLayoutChangeListener {
                        override fun onLayoutChange(
                            view: View,
                            left: Int,
                            top: Int,
                            right: Int,
                            bottom: Int,
                            oldLeft: Int,
                            oldTop: Int,
                            oldRight: Int,
                            oldBottom: Int,
                        ) {
                            if (view.width == 0 || view.height == 0) return
                            view.removeOnLayoutChangeListener(this)
                            applyGlass()
                        }
                    }
                    pivotBar.addOnLayoutChangeListener(listener)
                }
            } catch (ex: Throwable) {
                Logger.printException { "Glass navigation bar apply failure" }
            }
        }
    }

    // `PivotBar` does not override onMeasure, so hook the implementation it inherits.
    hookPivotBarMethod("onMeasure", "(II)V") { param ->
        val view = param.thisObject as? View ?: return@hookPivotBarMethod
        try {
            param.args[1] = NavigationBarPatch.getNavigationBarHeightMeasureSpec(
                view, param.args[1] as Int
            )
        } catch (ex: Throwable) {
            Logger.printException { "Glass navigation bar measure failure" }
        }
    }

    // The bar hides itself by translation while scrolling or dragging the player. The extra
    // room added to the glass bar has to move with it, otherwise a strip stays on screen.
    hookPivotBarMethod("setTranslationY", "(F)V") { param ->
        val view = param.thisObject as? View ?: return@hookPivotBarMethod
        try {
            param.args[0] = NavigationBarPatch.getNavigationBarTranslationY(
                view, param.args[0] as Float
            )
        } catch (ex: Throwable) {
            Logger.printException { "Glass navigation bar translation failure" }
        }
    }

    // Reset the bottom padding to the floating room the capsule needs. `PivotBar` inherits
    // setPadding/setPaddingRelative from View.
    listOf("setPadding", "setPaddingRelative").forEach { methodName ->
        hookPivotBarMethod(methodName, "(IIII)V") { param ->
            val view = param.thisObject as? View ?: return@hookPivotBarMethod
            try {
                param.args[3] = NavigationBarPatch.getNavigationBarPadding(
                    view, param.args[3] as Int
                )
            } catch (ex: Throwable) {
                Logger.printException { "Glass navigation bar padding failure" }
            }
        }
    }

    // Observe touches for the press animation of the glass: the capsule breathes slightly and a
    // soft highlight follows the finger. The events are not consumed, so tab clicks keep working.
    hookPivotBarMethod("dispatchTouchEvent", "(Landroid/view/MotionEvent;)Z") { param ->
        val event = param.args[0] as? MotionEvent ?: return@hookPivotBarMethod
        try {
            YouTubeGlassNavigationBar.onNavigationBarTouch(event)
        } catch (ex: Throwable) {
            Logger.printException { "Glass navigation bar touch failure" }
        }
    }

    // The app sometimes shows and hides the navigation bar without its own slide animation
    // (the second argument is false), which makes the glass capsule jump into place while only
    // the tabs animate. Force its animation while the glass is enabled, so the capsule follows
    // the bar instead of appearing and disappearing instantly.
    //
    // On 21.45+ an experiment (flag 45649805) adds a second show/hide controller, called right
    // after the first one with the same arguments. When the experiment is enabled, the first
    // controller returns immediately without animating, so every controller has to be forced.
    NavigationBarAnimationAllControllersFingerprint.dexMethodList.forEach { method ->
        method.hookMethod {
            before { param ->
                if (Settings.NAVIGATION_BAR_GLASS.get() && param.args[1] == false) {
                    param.args[1] = true
                    Logger.printDebug { "Glass navigation bar animation forced: ${method.methodSign}" }
                }
            }
        }
    }

    // The experiment flag above cannot be forced through: the second controller is entered even
    // with a forced animation argument, but its animation only runs from a known final state
    // (its visibility field is otherwise out of sync and it returns without doing anything).
    // Force the experiment off instead: both controllers then pass through the classic animated
    // controller, whose animation is unconditional. The checker class is obfuscated and its
    // name changes between versions, so it is discovered here from the fields of the hooked
    // controller: a field whose type has a method taking (int, long, boolean) and returning
    // boolean is the feature flag checker. Hooked once, only while the glass is enabled.
    val forcedExperimentCheckers = HashSet<String>()
    NavigationBarAnimationAllControllersFingerprint.dexMethodList.forEach { method ->
        method.hookMethod {
            before { param ->
                if (!Settings.NAVIGATION_BAR_GLASS.get()) return@before

                try {
                    for (field in param.thisObject.javaClass.declaredFields) {
                        val checkerType = field.type
                        for (checkerMethod in checkerType.declaredMethods) {
                            if (checkerMethod.returnType != java.lang.Boolean.TYPE) continue
                            val parameterTypes = checkerMethod.parameterTypes
                            if (parameterTypes.size != 3) continue
                            if (parameterTypes[0] != Integer.TYPE || parameterTypes[1] != java.lang.Long.TYPE
                                || parameterTypes[2] != java.lang.Boolean.TYPE
                            ) continue

                            // Nested classes keep the '$' of the binary name in the signature.
                            val signature = "L${checkerType.name};->${checkerMethod.name}(IJZ)Z"
                            if (!forcedExperimentCheckers.add(signature)) continue

                            DexMethod(signature).hookMethod {
                                before { checkerParam ->
                                    if (!Settings.NAVIGATION_BAR_GLASS.get()) return@before
                                    if (checkerParam.args[1] == 45649805L) {
                                        checkerParam.result = false
                                    }
                                }
                            }
                            Logger.printDebug { "Glass navigation bar experiment flag disabled: $signature" }
                        }
                    }
                } catch (ex: Throwable) {
                    Logger.printException { "Glass navigation bar experiment flag failure" }
                }
            }
        }
    }

    // The controller of the classic layout only (not matched by the experiment layout),
    // forced as well in case the fingerprint above does not match on a new app version.
    NavigationBarAnimationFingerprint.dexMethodList.forEach { method ->
        method.hookMethod {
            before { param ->
                if (Settings.NAVIGATION_BAR_GLASS.get() && param.args[1] == false) {
                    param.args[1] = true
                    Logger.printDebug { "Glass navigation bar animation forced" }
                }
            }
        }
    }

    // The app draws the tint of its blurred background with a Paint held by the background
    // drawable. Lower its alpha, so more of the blurred content shows through the capsule.
    // This replaces the patcher's injection into the tint method. The drawable class is not
    // stable between app versions, and the background can be replaced by another drawable,
    // so the Paint is looked up for each new background instead of caching one field.
    fun findBlurTint(background: Drawable): Paint? {
        var type: Class<*>? = background.javaClass
        while (type != null && type != Any::class.java) {
            for (field in type.declaredFields) {
                if (!Paint::class.java.isAssignableFrom(field.type)) continue

                try {
                    field.isAccessible = true
                    (field.get(background) as? Paint)?.let { return it }
                } catch (_: Throwable) {
                    // The field may belong to another instance of the same drawable class.
                }
            }
            type = type.superclass
        }
        return null
    }

    DexMethod("Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V")
        .hookMethod {
            after { param ->
                val view = param.thisObject as? View ?: return@after
                if (!isPivotBar(view)) return@after

                try {
                    val background = view.background as? Drawable ?: return@after
                    // The capsule has Paint fields of its own, which must keep their alpha.
                    if (background is GlassCapsuleDrawable) return@after

                    val paint = findBlurTint(background) ?: return@after
                    NavigationBarPatch.setNavigationBarBlurTint(paint)
                } catch (ex: Throwable) {
                    Logger.printException({ "Glass navigation bar tint failure" }, ex)
                }
            }
        }
}
