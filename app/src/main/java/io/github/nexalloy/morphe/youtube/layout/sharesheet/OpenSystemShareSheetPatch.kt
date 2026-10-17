package io.github.nexalloy.morphe.youtube.layout.sharesheet

import app.morphe.extension.youtube.patches.OpenSystemShareSheetPatch
import app.morphe.extension.youtube.patches.OpenSystemShareSheetPatch.ActionSheetControllerInterface
import com.google.protobuf.CodedInputStream
import com.google.protobuf.WireFormat
import io.github.nexalloy.callMethod
import io.github.nexalloy.createProxy
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val openSystemShareSheetPatch = patch(
    name = "Open system share sheet",
    description = "Adds an option to always open the system share sheet instead of the in-app share sheet."
) {
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
    val extensionFields = ::shareEndpointExtensionFields.fieldList
    val getExtensionNumber = ::shareEndpointExtensionNumberMethod.method
    val extensionNumbers = extensionFields.associate { field ->
        field.declaringClass to getExtensionNumber(field.get(null)) as Int
    }
    listOf(
        ShareEndpointCommandFingerprint to extensionFields.first().declaringClass,
        ShareEntityEndpointCommandFingerprint to ::shareEntityEndpointType.clazz
    ).forEach { (fingerprint, endpointType) ->
        fingerprint.hookMethod {
            before {
                val command = it.args[0]
                val bytes = command.callMethod("toByteArray") as ByteArray
                // The host inlines extension access; unwrap its serialized field before entering the command.
                val endpoint = if (endpointType.isInstance(command)) bytes else {
                    extractEndpoint(bytes, extensionNumbers.getValue(endpointType)) ?: return@before
                }
                if (OpenSystemShareSheetPatch.openSystemShareSheet(endpoint)) {
                    it.result = null
                }
            }
        }
    }
}

private fun extractEndpoint(bytes: ByteArray, fieldNumber: Int): ByteArray? {
    val input = CodedInputStream.newInstance(bytes)
    while (!input.isAtEnd) {
        val tag = input.readTag()
        if (WireFormat.getTagFieldNumber(tag) == fieldNumber &&
            WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED
        ) {
            return input.readByteArray()
        }
        if (!input.skipField(tag)) break
    }
    return null
}
