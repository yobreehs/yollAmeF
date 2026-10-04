package io.github.nexalloy.morphe.youtube.layout.miniplayer

import android.graphics.Rect
import android.view.MotionEvent
import app.morphe.extension.youtube.patches.MiniplayerPatch as ExtensionMiniplayerPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.newfeatures.addNewFeaturesPlayerPreferences
import io.github.nexalloy.patch

/**
 * Adds options to change the in-app minimized player.
 *
 * Ported from Morphe v1.45.0 `miniplayerPatch`. The Xposed port wires what is expressible
 * without bytecode injection: the modern miniplayer feature flags, the horizontal drag /
 * offscreen behavior and the button-listener hooks. The minimal bar types and the resuming
 * start-up hook need synthetic bytecode / register access and are not portable here.
 */
val Miniplayer = patch(
    name = "Miniplayer",
    description = "Adds options to change the in-app minimized player.",
) {
    addNewFeaturesPlayerPreferences(
        PreferenceScreenPreference(
            key = "morphe_miniplayer_screen",
            sorting = Sorting.UNSORTED,
            preferences = setOf(
                ListPreference(
                    "morphe_miniplayer_type",
                    entriesKey = "morphe_miniplayer_type_21_29_entries",
                    entryValuesKey = "morphe_miniplayer_type_21_29_entry_values",
                ),
                SwitchPreference("morphe_miniplayer_hide_title", summary = true),
                SwitchPreference("morphe_miniplayer_disable_rounded_corners", summary = true),
                SwitchPreference("morphe_miniplayer_disable_drag_and_drop", summary = true),
                SwitchPreference("morphe_miniplayer_disable_horizontal_drag", summary = true),
                SwitchPreference("morphe_miniplayer_disable_horizontal_reposition", summary = true),
            ),
        ),
    )

    // << Modern miniplayer feature flag overrides (via the standard flag getter). >>
    insertLiteralOverride(45622882L) { ExtensionMiniplayerPatch.getModernFeatureFlagsActiveOverride(it) }
    insertLiteralOverride(45628823L) { ExtensionMiniplayerPatch.getMiniplayerDoubleTapAction(it) }
    insertLiteralOverride(45657015L) { ExtensionMiniplayerPatch.getMiniplayerOnCloseHandler(it) }
    insertLiteralOverride(45630429L) { ExtensionMiniplayerPatch.getModernMiniplayerOverride(it) }
    insertLiteralOverride(45628752L) { ExtensionMiniplayerPatch.getMiniplayerDragAndDrop(it) }
    insertLiteralOverride(45652224L) { ExtensionMiniplayerPatch.getRoundedCorners(it) }

    // << Horizontal drag and offscreen handling. >>
    // Skip the offscreen rect validation when horizontal drag is disabled.
    MiniplayerOffscreenRectValidatorFingerprint.hookMethod {
        before { param ->
            if (ExtensionMiniplayerPatch.getHorizontalDrag()) param.result = 0
        }
    }
    // Skip the offscreen handler when horizontal drag is disabled.
    MiniplayerOffscreenHandlerFingerprint.hookMethod {
        before { param ->
            if (ExtensionMiniplayerPatch.getHorizontalDrag()) param.result = null
        }
    }
    // Track the offscreen miniplayer button press (blocks repositioning taps).
    NextGenWatchLayoutOnInterceptTouchEventFingerprint.hookMethod {
        before { param ->
            (param.args.firstOrNull() as? MotionEvent)
                ?.let { ExtensionMiniplayerPatch.enableOffScreenMiniplayerButtonPressed(it) }
        }
    }
    // Block offscreen repositioning when the miniplayer is dragged offscreen.
    MiniplayerHorizontalRepositionFingerprint.hookMethod {
        before { param ->
            val current = param.args.firstOrNull() as? Rect
            val previous = runCatching {
                ::miniplayerPreviousRectField.field.get(param.thisObject) as? Rect
            }.getOrNull()
            param.args[0] = ExtensionMiniplayerPatch.blockOffscreenMiniplayerHorizontalReposition(current, previous)
        }
    }
}