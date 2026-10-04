package io.github.nexalloy.morphe.youtube.layout.hide.endscreen

import android.view.View
import android.view.ViewStub
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.patches.components.BufferAsciiStrings
import app.morphe.extension.shared.patches.components.ContextInterface
import app.morphe.extension.shared.patches.components.Filter
import app.morphe.extension.shared.patches.components.StringFilterGroup
import app.morphe.extension.shared.settings.BooleanSetting
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.newfeatures.addNewFeaturesPlayerPreferences
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

/**
 * Registered lazily (app-side) so the in-app settings screen resolves the key
 * (no "Preference key has no setting" debug log) and the settings UI keeps the
 * value live via the normal Setting sync. No extension-side setting is needed.
 *
 * MUST NOT be a plain top-level val: the Setting constructor requires
 * Utils.getContext(), which is only set after the first YouTube activity is
 * created. Eager initialization at class load kills the whole module.
 */
val hideEndScreenSetting by lazy { BooleanSetting("morphe_hide_end_screen", false) }

/**
 * Adds an option to hide the end screen (video suggestions with channel icons) that YouTube
 * shows at the end of a video.
 *
 * The end screen ("full_screen_end_screen" / "related_endscreen_results") is created by the
 * player when the video ends. All creation paths are intercepted so no variant escapes.
 */
val HideEndScreen = patch(
    name = "Hide end screen",
    description = "Hides the video suggestions end screen (and its channel icons) shown at the end of a video.",
) {
    dependsOn(LithoFilter)

    addNewFeaturesPlayerPreferences(
        SwitchPreference("morphe_hide_end_screen", summary = true),
    )

    // Since 21.39 the end-of-video suggestion strip is delivered as an elements (eml-js-fe)
    // component ("teaser_carousel_with_controller.eml-js-fe|<hash>"), so it goes through the
    // Litho filter machinery (see "Searching ID:" debug lines). Filter it by identifier;
    // the native view hooks below remain as a fallback for other versions.
    addLithoFilter(EndScreenTeaserFilter())

    val endScreenId by lazy {
        runCatching {
            Utils.getContext().resources
                .getIdentifier("full_screen_end_screen", "id", Utils.getContext().packageName)
        }.getOrDefault(0)
    }
    val endScreenResultsId by lazy {
        runCatching {
            Utils.getContext().resources
                .getIdentifier("related_endscreen_results", "id", Utils.getContext().packageName)
        }.getOrDefault(0)
    }

    var idsLogged = false
    fun logIdsIfNeeded() {
        if (!idsLogged) {
            idsLogged = true
            if (endScreenId == 0 && endScreenResultsId == 0) {
                Logger.printDebug { "HideEndScreen: no end screen ids found in this version" }
            }
            Logger.printDebug {
                "HideEndScreen: full_screen_end_screen id=$endScreenId, " +
                    "related_endscreen_results id=$endScreenResultsId, " +
                    "enabled=${hideEndScreenSetting.get()}"
            }
        }
    }

    fun isHideEndScreenEnabled(): Boolean {
        logIdsIfNeeded()
        return hideEndScreenSetting.get()
    }

    fun isEndScreenView(view: View): Boolean =
        view.id == endScreenId || view.id == endScreenResultsId

    fun hideEndScreenView(view: View) {
        view.visibility = View.GONE
        Logger.printDebug { "Hiding end screen view: id=${view.id} class=${view.javaClass.name}" }
    }

    val isEndScreenStub: (ViewStub) -> Boolean = { stub ->
        stub.id == endScreenId || stub.id == endScreenResultsId ||
            stub.inflatedId == endScreenId || stub.inflatedId == endScreenResultsId
    }

    // 1) The end screen is inflated from a stub when the video ends;
    //    suppress showing the stub (same mechanism as the autoplay preview).
    DexMethod("Landroid/view/ViewStub;->setVisibility(I)V").hookMethod {
        before { param ->
            if (param.thisObject is ViewStub) {
                val stub = param.thisObject as ViewStub
                if ((param.args[0] as? Int) == View.VISIBLE &&
                    isEndScreenStub(stub) && isHideEndScreenEnabled()
                ) {
                    Logger.printDebug { "HideEndScreen: suppressing end screen stub (id=${stub.id})" }
                    param.result = null
                }
            }
        }
    }

    // 2) Some versions call stub.inflate() directly; hide the inflated view.
    DexMethod("Landroid/view/ViewStub;->inflate()Landroid/view/View;").hookMethod {
        after { param ->
            if (param.thisObject is ViewStub) {
                val stub = param.thisObject as ViewStub
                if (isEndScreenStub(stub) && isHideEndScreenEnabled()) {
                    (param.result as? View)?.let { hideEndScreenView(it) }
                }
            }
        }
    }

    // 3) Native layouts resolved by id (not stubs) — hide right when they are found.
    DexMethod("Landroid/view/View;->findViewById(I)Landroid/view/View;").hookMethod {
        after { param ->
            val id = param.args[0] as? Int ?: return@after
            if ((id == endScreenId || id == endScreenResultsId) && isHideEndScreenEnabled()) {
                (param.result as? View)?.let { hideEndScreenView(it) }
            }
        }
    }

    // 4) The end screen root is often shown with plain View.setVisibility(VISIBLE);
    //    suppress the show (the view is left GONE).
    DexMethod("Landroid/view/View;->setVisibility(I)V").hookMethod {
        before { param ->
            if ((param.args[0] as? Int) != View.VISIBLE) return@before
            val view = param.thisObject as? View ?: return@before
            if (isEndScreenView(view) && isHideEndScreenEnabled()) {
                param.result = null
            }
        }
    }
}

/**
 * Filters the end-of-video "teaser" (suggested video + channel icon) that appears in the last
 * seconds of a video. Since 21.39 it is an elements component:
 * "teaser_carousel_with_controller.eml-js-fe|<hash>" (seen in the Debug protobuffer log).
 */
class EndScreenTeaserFilter : Filter() {
    init {
        addIdentifierCallbacks(
            StringFilterGroup(null, "teaser_carousel_with_controller")
        )
    }

    override fun isFiltered(
        contextInterface: ContextInterface,
        identifier: String,
        accessibility: String,
        path: CharSequence,
        buffer: ByteArray,
        asciiStrings: BufferAsciiStrings,
        matchedGroup: StringFilterGroup,
        contentType: Filter.FilterContentType,
        contentIndex: Int,
    ): Boolean = hideEndScreenSetting.get()
}