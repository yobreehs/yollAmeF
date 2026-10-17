package io.github.nexalloy.morphe.youtube.layout.sharesheet

import app.morphe.extension.youtube.patches.OpenSystemShareSheetPatch
import app.morphe.extension.youtube.patches.OpenSystemShareSheetPatch.ActionSheetControllerInterface
import io.github.nexalloy.createProxy
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.layout.flyout.flyoutPatch
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val openSystemShareSheetPatch = patch(
    name = "Open system share sheet",
    description = "Adds an option to always open the system share sheet instead of the in-app share sheet."
) {
    dependsOn(flyoutPatch, PlayerTypeHook)

    PreferenceScreen.GENERAL.addPreferences(
        SwitchPreference("morphe_open_system_share_sheet", summary = true)
    )

    // Add interface method to dismiss the action sheet.
    val dismissActionSheet = ::dismissActionSheetMethod.method
    val emptyOptional = dismissActionSheet.parameterTypes.single().getMethod("empty").invoke(null)
    ShowActionSheetCommandFingerprint.hookMethod {
        before {
            OpenSystemShareSheetPatch.setActionSheetController(it.thisObject.createProxy { reference ->
                ActionSheetControllerInterface {
                    reference.get()?.let { controller -> dismissActionSheet(controller, emptyOptional) }
                }
            })
        }
    }

    // Open the system share sheet and skip the share panel request,
    // so the in-app share sheet is never opened.
    ShareEndpointCommandFingerprint.hookMethod {
        before {
            if (OpenSystemShareSheetPatch.openSystemShareSheet()) {
                it.result = null
            }
        }
    }
}
