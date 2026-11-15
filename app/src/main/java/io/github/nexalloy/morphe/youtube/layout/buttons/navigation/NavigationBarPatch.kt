package io.github.nexalloy.morphe.youtube.layout.buttons.navigation

import android.widget.TextView
import app.morphe.extension.shared.Logger
import app.morphe.extension.youtube.patches.ClassicSubscriptionsLayoutPatch
import app.morphe.extension.youtube.patches.NavigationBarPatch
import app.morphe.extension.youtube.settings.Settings
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.morphe.youtube.misc.contexthook.Endpoint
import io.github.nexalloy.morphe.youtube.misc.contexthook.addOSNameHook
import io.github.nexalloy.morphe.youtube.misc.contexthook.clientContextHookPatch
import io.github.nexalloy.morphe.youtube.misc.navigation.NavigationBarHook
import io.github.nexalloy.morphe.youtube.misc.navigation.PivotBarRendererFingerprint
import io.github.nexalloy.morphe.youtube.misc.navigation.hookNavigationButtonCreated
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_31_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import org.luckypray.dexkit.wrap.DexMethod

val NavigationBar = patch(
    name = "Navigation bar",
    description = "Adds options to hide and change the bottom navigation bar (such as the Shorts button) "
            + "and the upper navigation toolbar.",
    settings = {
        val navPreferences = mutableSetOf(
            SwitchPreference("morphe_hide_home_button"),
            SwitchPreference("morphe_hide_shorts_button"),
            SwitchPreference("morphe_hide_create_button"),
            SwitchPreference("morphe_hide_subscriptions_button"),
            SwitchPreference("morphe_hide_notifications_button"),
//        SwitchPreference("morphe_show_search_button"),
//        ListPreference("morphe_show_search_button_index"),
//        SwitchPreference("morphe_show_settings_button"),
//        ListPreference("morphe_show_settings_button_index"),
//        SwitchPreference("morphe_show_settings_button_type", summary = true),
            SwitchPreference("morphe_swap_create_with_notifications_button", summary = true),
//        SwitchPreference("morphe_hide_navigation_bar"),
//        SwitchPreference("morphe_narrow_navigation_buttons", summary = true),
            SwitchPreference("morphe_hide_navigation_button_labels"),
            SwitchPreference("morphe_disable_icon_only_navigation_buttons", summary = true),
            SwitchPreference("morphe_hide_navigation_new_content_dot"),
            SwitchPreference("morphe_navigation_bar_animations", summary = true),
//        SwitchPreference("morphe_disable_translucent_navigation", summary = true)
        )

        if (is_20_31_or_greater) {
            navPreferences += SwitchPreference("morphe_disable_auto_hide_navigation_bar", summary = true)
        }

        PreferenceScreen.GENERAL.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_navigation_bar_screen",
                sorting = Sorting.UNSORTED,
                preferences = navPreferences
            )
        )
    },
) {
    dependsOn(
        NavigationBarHook,
        VersionCheck,
        clientContextHookPatch,
    )

    // Swap create with notifications button.
    addOSNameHook(
        Endpoint.GUIDE,
        NavigationBarPatch::swapCreateWithNotificationButton
    )
    setExtensionIsPatchIncluded(NavigationBarPatch::class.java)

    // YouTube can send the navigation buttons in an icon-only layout, which the other
    // navigation bar patches ignore. Convert those items back to the labelled layout first.
    PivotBarRendererFingerprint.hookMethod {
        before { param ->
            val item = param.args[0] ?: return@before
            val enabled = Settings.DISABLE_ICON_ONLY_NAVIGATION_BUTTONS.get()
            Logger.printDebug {
                "PivotBarRenderer item: ${item.javaClass.name}, icon-only conversion: $enabled"
            }
            if (!enabled) return@before

            try {
                // The module and YouTube each bundle their own protobuf runtime, so the item
                // can not be cast to the module's MessageLite. Use the object's own methods.
                val itemBytes = item.javaClass
                    .getMethod("toByteArray")
                    .invoke(item) as? ByteArray ?: return@before

                // The 'You' tab loses its navigation command when its proto is rebuilt, so it
                // keeps the original icon only renderer and the app's own click handling.
                if (ClassicSubscriptionsLayoutPatch.isIconOnlyLibraryItem(itemBytes)) {
                    Logger.printDebug { "Keeping the icon-only navigation button: FElibrary" }
                    return@before
                }

                val converted = ClassicSubscriptionsLayoutPatch
                    .convertIconOnlyPivotBarItemBytes(itemBytes) ?: return@before

                val parser = item.javaClass
                    .getMethod("getParserForType")
                    .invoke(item) ?: return@before
                // Parser method names are obfuscated in YouTube, so find parseFrom(byte[]).
                val parseFrom = parser.javaClass.methods.firstOrNull {
                    it.parameterTypes.size == 1 &&
                        it.parameterTypes[0] == ByteArray::class.java &&
                        it.returnType != Void.TYPE
                } ?: return@before

                param.args[0] = parseFrom.invoke(parser, converted)
                Logger.printDebug { "Converted an icon-only navigation button" }
            } catch (ex: Throwable) {
                Logger.printException(
                    { "Failed to convert an icon-only navigation button" }, ex
                )
            }
        }
    }

    // Hide navigation button labels.
    CreatePivotBarFingerprint.hookMethod(scopedHook(DexMethod("Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V").toMethod()) {
        before { param ->
            Logger.printDebug { "CreatePivotBar label text set: ${param.args[0]}" }
            NavigationBarPatch.hideNavigationButtonLabels(param.thisObject as TextView)
        }
    })

    // Hook navigation button created, in order to hide them.
    hookNavigationButtonCreated.add { button, view ->
        NavigationBarPatch.navigationTabCreated(button, view)
    }

    // TODO Hide navigation bar

    // TODO Paint over the translucent status bar and navigation bar

    // Animated navigation tabs.
    insertLiteralOverride(45680008L, NavigationBarPatch::useAnimatedNavigationButtons)

    // TODO Narrow navigation buttons

    // disableAutoHidingNavigationBar

    if (is_20_31_or_greater) {
        listOf(
            AutoHideNavigationBarOnFeedScrollingFingerprint,
            AutoHideNavigationBarOnDismissMiniplayerFingerprint,
        ).forEach {
            it.hookMethod {
                before { param ->
                    if (NavigationBarPatch.disableAutoHidingNavigationBar()) {
                        param.result = null
                    }
                }
            }
        }
    }

    // TODO upper navigation toolbar
}
