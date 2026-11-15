package io.github.nexalloy.morphe.youtube.interaction.swipecontrols

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import app.morphe.extension.shared.settings.preference.ColorPickerWithOpacitySliderPreference
import app.morphe.extension.shared.settings.preference.SeekBarPreference
import app.morphe.extension.youtube.settings.preference.SwipeVolumeStepsPreference
import app.morphe.extension.youtube.settings.preference.SwipeZonePreference
import app.morphe.extension.youtube.swipecontrols.SwipeControlsHostActivity
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.settings.preference.InputType
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.NonInteractivePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_34_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.mainActivityClass
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch

val SwipeControls = patch(
    name = "Swipe controls",
    description = "Adds options to enable and configure volume and brightness swipe controls.",
    settings = {
        PreferenceScreen.SWIPE_CONTROLS.addPreferences(
            ListPreference(
                "morphe_swipe_left_zone",
                entriesKey = "morphe_swipe_zone_action_entries",
                entryValuesKey = "morphe_swipe_zone_action_entry_values"
            ),
            ListPreference(
                "morphe_swipe_right_zone",
                entriesKey = "morphe_swipe_zone_action_entries",
                entryValuesKey = "morphe_swipe_zone_action_entry_values"
            ),
            ListPreference(
                "morphe_swipe_top_zone",
                entriesKey = "morphe_swipe_zone_action_entries",
                entryValuesKey = "morphe_swipe_zone_action_entry_values"
            ),
            NonInteractivePreference(
                key = "morphe_swipe_zone_width",
                tag = SeekBarPreference::class.java,
                // Upstream inflates this from XML where AttributeSet handles 'selectable'.
                // FemAlloy instantiates via reflection, applying 'selectable' AFTER the constructor.
                // Aligning with SeekBar's init() default to avoid patching extension code.
                selectable = true,
            ),
            NonInteractivePreference(
                key = "morphe_swipe_speed_zone_height",
                tag = SeekBarPreference::class.java,
                selectable = true,
            ),
            NonInteractivePreference(
                key = "morphe_swipe_zone_preview",
                summaryKey = null,
                tag = SwipeZonePreference::class.java,
            ),
            NonInteractivePreference(
                key = "morphe_swipe_brightness_sensitivity",
                tag = SeekBarPreference::class.java,
                selectable = true,
            ),
            NonInteractivePreference(
                key = "morphe_swipe_brightness_steps",
                tag = SeekBarPreference::class.java,
                selectable = true,
            ),
            NonInteractivePreference(
                key = "morphe_swipe_brightness_step_percent",
                tag = SeekBarPreference::class.java,
                selectable = true,
            ),
            NonInteractivePreference(
                key = "morphe_swipe_volume_distance",
                tag = SeekBarPreference::class.java,
                selectable = true,
            ),
            ListPreference(
                "morphe_swipe_volume_steps",
                tag = SwipeVolumeStepsPreference::class.java
            ),
            NonInteractivePreference(
                key = "morphe_volume_key_steps",
                tag = SeekBarPreference::class.java,
                selectable = true,
            ),
            NonInteractivePreference(
                key = "morphe_swipe_speed_sensitivity",
                tag = SeekBarPreference::class.java,
                selectable = true,
            ),
            ListPreference("morphe_swipe_speed_step"),
            SwitchPreference("morphe_swipe_ignore_when_locked", summary = true),
            SwitchPreference("morphe_swipe_press_to_engage", summary = true),
            SwitchPreference("morphe_swipe_haptic_feedback"),
            SwitchPreference("morphe_swipe_save_and_restore_brightness", summary = true),
            SwitchPreference("morphe_swipe_lowest_value_enable_auto_brightness", summary = true),
            ListPreference("morphe_swipe_overlay_style"),
            NonInteractivePreference(
                key = "morphe_swipe_overlay_background_opacity",
                tag = SeekBarPreference::class.java,
                selectable = true,
            ),
            TextPreference("morphe_swipe_overlay_progress_brightness_color",
                tag = ColorPickerWithOpacitySliderPreference::class.java,
                inputType = InputType.TEXT_CAP_CHARACTERS),
            TextPreference("morphe_swipe_overlay_progress_volume_color",
                tag = ColorPickerWithOpacitySliderPreference::class.java,
                inputType = InputType.TEXT_CAP_CHARACTERS),
            TextPreference("morphe_swipe_overlay_progress_speed_color",
                tag = ColorPickerWithOpacitySliderPreference::class.java,
                inputType = InputType.TEXT_CAP_CHARACTERS),
            NonInteractivePreference(
                key = "morphe_swipe_text_overlay_size",
                tag = SeekBarPreference::class.java,
                selectable = true,
            ),
            TextPreference("morphe_swipe_overlay_timeout", inputType = InputType.NUMBER),
            TextPreference("morphe_swipe_threshold", inputType = InputType.NUMBER),
        )
    },
) {
    dependsOn(
        PlayerTypeHook,
        VideoInformationPatch
    )

//    if (!is_20_34_or_greater) {
//        PreferenceScreen.SWIPE_CONTROLS.addPreferences(
//            SwitchPreference("morphe_swipe_change_video", summary = true)
//        )
//    }


    hookSwipeControlsActivity(::mainActivityClass.clazz)

    if (!is_20_34_or_greater) {
        insertLiteralOverride(45631116L, SwipeControlsHostActivity::allowSwipeChangeVideo)
    }

    PlayerOverlayContainerFingerprint.hookMethod {
        val overlayNameField = ::PlayerOverlayNameField.field
        before {
            val overlayName = overlayNameField.get(it.thisObject) as String?
            SwipeControlsHostActivity.setPlayerOverlay(it.thisObject as View, overlayName)
        }
    }
}

private fun PatchExecutor.hookSwipeControlsActivity(activityClass: Class<*>) {
    val downstreamTouch = getInvoker(Activity::class.java.getDeclaredMethod("dispatchTouchEvent", MotionEvent::class.java))
    val downstreamKey = getInvoker(Activity::class.java.getDeclaredMethod("dispatchKeyEvent", KeyEvent::class.java))
    activityClass.declaredConstructors.forEach { constructor ->
        constructor.hookMethod {
            after {
                val activity = it.thisObject as Activity
                if (XposedHelpers.getAdditionalInstanceField(activity, "swipeControlsHost") == null) {
                    val host = SwipeControlsHostActivity(activity, object : SwipeControlsHostActivity.Downstream {
                        override fun dispatchTouchEvent(event: MotionEvent): Boolean =
                            downstreamTouch.invokeSpecial(activity, event) as Boolean
                        override fun dispatchKeyEvent(event: KeyEvent): Boolean =
                            downstreamKey.invokeSpecial(activity, event) as Boolean
                    })
                    XposedHelpers.setAdditionalInstanceField(activity, "swipeControlsHost", host)
                }
            }
        }
    }
    fun host(activity: Any) =
        XposedHelpers.getAdditionalInstanceField(activity, "swipeControlsHost") as SwipeControlsHostActivity

    activityClass.getDeclaredMethod("onCreate", Bundle::class.java).hookMethod {
        after { host(it.thisObject).patch_initialize() }
    }
    activityClass.getDeclaredMethod("onStart").hookMethod {
        after { host(it.thisObject).patch_reAttachOverlays() }
    }
    activityClass.getDeclaredMethod("onDestroy").hookMethod {
        after { host(it.thisObject).patch_onDestroy() }
    }
    activityClass.getDeclaredMethod("dispatchTouchEvent", MotionEvent::class.java).hookMethod {
        before {
            val event = it.args[0] as MotionEvent?
            if (event != null && host(it.thisObject).dispatchTouchEvent(event)) it.result = true
        }
    }
    activityClass.getDeclaredMethod("dispatchKeyEvent", KeyEvent::class.java).hookMethod {
        before {
            val event = it.args[0] as KeyEvent?
            if (event != null && host(it.thisObject).dispatchKeyEvent(event)) it.result = true
        }
    }
}
