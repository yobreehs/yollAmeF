/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/1837
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.layout.flyout

import app.morphe.extension.youtube.patches.AddToQueuePatch
import app.morphe.extension.youtube.patches.LocalQueuePatch
import io.github.nexalloy.morphe.setExtensionIsPatchIncluded
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.morphe.youtube.layout.hide.general.ContextualMenuItemBuilderOnClickFingerprint
import io.github.nexalloy.morphe.youtube.layout.hide.general.contextualMenuItemMessageField
import io.github.nexalloy.morphe.youtube.layout.playlistautoplay.navigationIntentHook
import io.github.nexalloy.morphe.youtube.misc.auth.authHookPatch
import io.github.nexalloy.morphe.youtube.misc.loadvideo.loadVideoHookPatch
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_05_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.playerStatusOverrideHook
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val addToQueuePatch = patch(
    name = "Add to queue",
    description = "Overrides the feed flyout 'Play next in queue' with the Morphe video queue."
) {
    dependsOn(flyoutPatch, authHookPatch, loadVideoHookPatch, VideoInformationPatch)

    PreferenceScreen.FEED.addPreferences(
        noTitleUnsortedPreferenceCategory(
            SwitchPreference("morphe_local_queue", summary = true),
            SwitchPreference("morphe_queue_override_flyout_menu", summary = true),
            SwitchPreference("morphe_queue_add_flyout_menu", summary = true)
        )
    )

    val runnableField = ::feedFlyoutButtonRunnableField.field
    FeedFlyoutButtonsInitializerFingerprint.hookMethod(scopedHook(::feedFlyoutMenuItemOptionalMethod.method) {
        before {
            val menuItem = it.args[0]
            if (runnableField.declaringClass.isInstance(menuItem)) {
                runnableField.set(menuItem, AddToQueuePatch.replaceButtonRunnable(runnableField.get(menuItem) as Runnable?))
            }
        }
    })

    val messageField = ::contextualMenuItemMessageField.field
    val getSubMessage = ::getSubMessageReference.method
    val enumIntField = ::enumIntField.field
    val enumMethodCall = ::enumMethodCall.method
    ContextualMenuItemBuilderOnClickFingerprint.hookMethod {
        before {
            val message = messageField.get(it.thisObject)
            if (!getSubMessage.parameterTypes.single().isInstance(message)) return@before
            val buttonInfo = getSubMessage(null, message)
            val buttonName = (enumMethodCall(null, enumIntField.get(buttonInfo)) as Enum<*>).name
            if (AddToQueuePatch.replaceOnItemClick(buttonName)) {
                it.result = null
            }
        }
    }

    if (!is_21_05_or_greater) {
        FeedFlyoutButtonsInitializerOnItemClickFingerprint.hookMethod {
            before {
                if (AddToQueuePatch.replaceOnItemClick(it.args[2] as Int)) {
                    it.result = null
                }
            }
        }
    }

    navigationIntentHook(LocalQueuePatch::shouldCancelNavigation)

    playerStatusOverrideHook(LocalQueuePatch::shouldCancelEndOfVideo)

    setExtensionIsPatchIncluded(AddToQueuePatch::class.java)
}
