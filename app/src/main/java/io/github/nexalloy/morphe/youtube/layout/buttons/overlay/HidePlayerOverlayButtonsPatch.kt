package io.github.nexalloy.morphe.youtube.layout.buttons.overlay

import android.view.View
import android.view.ViewStub
import android.widget.ImageView
import app.morphe.extension.shared.Utils
import app.morphe.extension.youtube.patches.HidePlayerOverlayButtonsPatch as ExtensionHidePlayerOverlayButtonsPatch
import app.morphe.extension.youtube.settings.Settings
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

private fun resourceId(name: String): Int = runCatching {
    Utils.getContext().resources.getIdentifier(name, "id", Utils.getContext().packageName)
}.getOrDefault(0)

/**
 * Adds options to hide buttons in the video player overlay (cast, captions, collapse,
 * fullscreen, autoplay toggle, previous/next, settings) and to customize the background
 * of the control buttons.
 *
 * Ported from Morphe v1.45.0 `hidePlayerOverlayButtonsPatch`. Instead of the upstream
 * instruction injections, the forks Xposed port intercepts the resolved button views
 * (`View.findViewById`) and the autoplay toggle / controls layout stubs.
 */
val HidePlayerOverlayButtons = patch(
    name = "Hide player overlay buttons",
    description = "Adds options to hide buttons in the video player overlay.",
) {
    addPlayerOverlayPreferences(
        noTitleUnsortedPreferenceCategory(
            SwitchPreference("morphe_hide_autoplay_button"),
            SwitchPreference("morphe_hide_captions_button"),
            SwitchPreference("morphe_hide_cast_button"),
            SwitchPreference("morphe_hide_collapse_button"),
            SwitchPreference("morphe_hide_fullscreen_button"),
            SwitchPreference("morphe_hide_player_control_buttons_background", summary = true),
            SwitchPreference("morphe_hide_player_previous_next_buttons"),
            SwitchPreference("morphe_hide_settings_button"),
        ),
        NonInteractivePreference(
            key = "morphe_player_control_buttons_background_opacity",
            tag = app.morphe.extension.shared.settings.preference.SeekBarPreference::class.java,
            selectable = true,
        ),
    )

    val mediaRouteButtonId = resourceId("media_route_button")
    val previousButtonTouchAreaId = resourceId("player_control_previous_button_touch_area")
    val nextButtonTouchAreaId = resourceId("player_control_next_button_touch_area")
    val overflowButtonId = resourceId("player_overflow_button")
    val fullscreenButtonId = resourceId("fullscreen_button")
    val collapseButtonId = resourceId("player_collapse_button")
    val titleAnchorId = resourceId("title_anchor")
    val autonavToggleId = resourceId("autonav_toggle")
    val controlsGroupStubId = resourceId("youtube_controls_button_group_layout_stub")

    // Hide cast / previous-next / settings / fullscreen / collapse buttons right when the
    // matching child view is resolved by the player layouts.
    DexMethod("Landroid/view/View;->findViewById(I)Landroid/view/View;").hookMethod {
        after { param ->
            val id = param.args[0] as? Int ?: return@after
            val view = param.result as? View ?: return@after
            when (id) {
                mediaRouteButtonId ->
                    if (Settings.HIDE_CAST_BUTTON.get()) view.visibility = View.GONE
                previousButtonTouchAreaId, nextButtonTouchAreaId ->
                    if (Settings.HIDE_PLAYER_PREVIOUS_NEXT_BUTTONS.get()) view.visibility = View.GONE
                overflowButtonId ->
                    if (Settings.HIDE_SETTINGS_BUTTON.get()) view.visibility = View.GONE
                fullscreenButtonId ->
                    if (Settings.HIDE_FULLSCREEN_BUTTON.get()) view.visibility = View.GONE
                collapseButtonId ->
                    if (Settings.HIDE_COLLAPSE_BUTTON.get()) {
                        ExtensionHidePlayerOverlayButtonsPatch.hideCollapseButton(view as? ImageView)
                    }
                titleAnchorId ->
                    if (Settings.HIDE_COLLAPSE_BUTTON.get()) {
                        ExtensionHidePlayerOverlayButtonsPatch.setTitleAnchorStartMargin(view)
                    }
            }
        }
    }

    // Hide the autoplay ("up next") toggle stub.
    DexMethod("Landroid/view/ViewStub;->setVisibility(I)V").hookMethod {
        before { param ->
            if (param.thisObject is ViewStub) {
                val stub = param.thisObject as ViewStub
                if ((param.args[0] as? Int) == View.VISIBLE &&
                    autonavToggleId != 0 &&
                    stub.id == autonavToggleId &&
                    Settings.HIDE_AUTOPLAY_BUTTON.get()
                ) {
                    param.result = null
                }
            }
        }
    }

    // Style the control buttons background right after the controls group layout is inflated.
    DexMethod("Landroid/view/ViewStub;->inflate()Landroid/view/View;").hookMethod {
        after { param ->
            if (param.thisObject is ViewStub) {
                val stub = param.thisObject as ViewStub
                if (controlsGroupStubId != 0 &&
                    (stub.id == controlsGroupStubId || stub.inflatedId == controlsGroupStubId) &&
                    param.result is View
                ) {
                    param.result = ExtensionHidePlayerOverlayButtonsPatch.styleControlButtonsBackground(param.result as View)
                }
            }
        }
    }

    // Hide the captions button via its controller (no stable resource id on 21.39).
    SubtitleButtonControllerFingerprint.hookMethod {
        after { param ->
            val imageView = runCatching {
                ::captionsButtonImageField.field.get(param.thisObject) as? ImageView
            }.getOrNull()
            imageView?.let { ExtensionHidePlayerOverlayButtonsPatch.hideCaptionsButton(it) }
        }
    }

    // Force the cast button feature flags off while the setting is enabled.
    insertLiteralOverride(45690090L) { original ->
        ExtensionHidePlayerOverlayButtonsPatch.hideCastButton(original)
    }
    insertLiteralOverride(45690091L) { original ->
        ExtensionHidePlayerOverlayButtonsPatch.hideCastButton(original)
    }
}