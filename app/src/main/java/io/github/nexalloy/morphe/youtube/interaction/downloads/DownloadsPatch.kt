package io.github.nexalloy.morphe.youtube.interaction.downloads

import app.morphe.extension.shared.settings.preference.ExternalDownloaderPreference
import app.morphe.extension.youtube.patches.DownloadsPatch
import app.morphe.extension.youtube.patches.InAppDownloadsPatch
import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch
import app.morphe.extension.shared.spoof.requests.StreamingDataRequest
import app.morphe.extension.youtube.videoplayer.ExternalDownloadButton
import android.net.Uri
import io.github.nexalloy.callMethod
import io.github.nexalloy.R
import io.github.nexalloy.HookDsl
import io.github.nexalloy.IScopedHookCallback
import io.github.nexalloy.morphe.shared.misc.request.buildRequestPatch
import io.github.nexalloy.morphe.shared.misc.request.hookBuildRequest
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceScreenPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.TextPreference
import io.github.nexalloy.morphe.youtube.misc.playercontrols.ControlInitializer
import io.github.nexalloy.morphe.youtube.misc.playercontrols.LegacyPlayerControls
import io.github.nexalloy.morphe.youtube.misc.playercontrols.addTopControl
import io.github.nexalloy.morphe.youtube.misc.playercontrols.initializeTopControl
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val Downloads = patch(
    name = "Downloads",
    description = "Adds support to download videos with an external downloader app " +
            "using the in-app download button or a video player action button, " +
            "and adds an option to download videos in-app.",
) {
    dependsOn(
        LegacyPlayerControls,
        VideoInformationPatch,
        buildRequestPatch,
    )

    PreferenceScreen.PLAYER.addPreferences(
        PreferenceScreenPreference(
            key = "morphe_external_downloader_screen",
            sorting = PreferenceScreenPreference.Sorting.UNSORTED,
            preferences = setOf(
                ListPreference("morphe_download_mode"),
                TextPreference(
                    "morphe_external_downloader_name",
                    tag = ExternalDownloaderPreference::class.java
                ),
                SwitchPreference("morphe_external_downloader_action_button", summary = true),
            ),
        ),
    )

    addTopControl(
        R.layout.morphe_external_download_button,
        R.id.morphe_external_download_button,
        R.id.morphe_external_download_button
    )

    initializeTopControl(
        ControlInitializer(
            R.id.morphe_external_download_button,
            ExternalDownloadButton::initializeLegacyButton
        )
    )

    OfflineVideoEndpointFingerprint.hookMethod {
        before {
            if (DownloadsPatch.inAppDownloadButtonOnClick(it.args[2] as String)) it.result = null
        }
    }

    // region In-app downloads.

    // The module and host use separate protobuf implementations.
    InAppDownloadsPatch.setMessageSerializer { it.callMethod("toByteArray") as ByteArray }
    val parseFrom = ProtobufClassParseByteArrayWithRegistryFingerprint.method
    val registry = GeneratedRegistryFingerprint.method.invoke(null)
    fun parse(defaultInstance: Any, bytes: ByteArray): Any =
        parseFrom.invoke(null, defaultInstance, bytes, registry)!!

    // Save the video instead of asking the server for the download action, which answers with the Premium upsell.
    val commandDefault = ::offlineCommandDefaultInstance.field.get(null)!!
    ResolveCommandFingerprint.hookMethod {
        before {
            InAppDownloadsPatch.replaceDownloadActionCommand(it.args[0])?.let { bytes ->
                it.args[0] = parse(commandDefault, bytes)
            }
        }
    }

    // Unlock the qualities of the video, and save the video instead of showing the Premium upsell.
    val offlineabilityDefault = ::offlineabilityDefaultInstance.field.get(null)!!
    ::saveOfflineVideoMethod.hookMethod {
        before {
            InAppDownloadsPatch.unlockOfflineability(it.args[1])?.let { bytes ->
                it.args[1] = parse(offlineabilityDefault, bytes)
            }
        }
    }

    // Save the video instead of showing the Premium upsell of the offline upsell endpoint.
    val commandField = ::offlineUpsellCommandField.field
    val upsellDefault = ::offlineUpsellDefaultInstance.field.get(null)!!
    OfflineUpsellResponseFingerprint.hookMethod {
        before {
            InAppDownloadsPatch.replaceOfflineUpsell(commandField.get(it.thisObject), it.args[0])?.let { bytes ->
                it.args[0] = parse(upsellDefault, bytes)
            }
        }
    }

    // Keep download authentication headers without starting playback spoof requests.
    hookBuildRequest { url, headers ->
        if (url != null && headers != null && Uri.parse(url).path?.endsWith("/player") == true) {
            StreamingDataRequest.setPlayerHeaders(headers)
        }
        headers
    }

    // Save the video with streams of another client, if the server declines to save the video.
    val getter = ::playerResponseGetterMethod.method
    val fromBytes = ::playerResponseModelFromBytesMethod.method
    OfflinePlayerResponseFingerprint.hookMethod(scopedHook(::offlinePlayerResponseFactoryMethod.method) {
        after {
            if (it.hasThrowable()) return@after
            val model = it.result ?: return@after
            val bytes = InAppDownloadsPatch.replaceDeclinedOfflinePlayerResponse(getter.invoke(model))
                ?: return@after
            // The playback stream override would replace the streams that can be saved.
            SpoofVideoStreamsPatch.setKeepStreamsOnThread(true)
            val replacement = try {
                // The response time, used for the expiration of the stream urls.
                fromBytes.invoke(null, bytes, System.currentTimeMillis())
            } finally {
                SpoofVideoStreamsPatch.setKeepStreamsOnThread(false)
            }
            InAppDownloadsPatch.onOfflinePlayerResponseReplaced(replacement)
            if (replacement != null) it.result = replacement
        }
    })

    // Streams of other clients are plain urls, which the server rejects or sends slowly if they are
    // requested like the streams of the app, with a POST body. Send them as plain GET requests instead.
    val urlField = ::cronetUrlField.field
    val httpMethodField = ::cronetHttpMethodField.field
    val uploadField = ::cronetUploadField.field
    CronetUrlRequestBuildFingerprint.hookMethod {
        before {
            if (InAppDownloadsPatch.isPlainStreamRequest(urlField.get(it.thisObject) as String?)) {
                httpMethodField.set(it.thisObject, "GET")
                uploadField.set(it.thisObject, null)
            }
        }
    }
    // Plain streams are sent slowly if a large range is requested. Request a smaller range instead.
    val limitRange: HookDsl<IScopedHookCallback>.() -> Unit = {
        before {
            if (innerDepth == 0) {
                it.args[0] = InAppDownloadsPatch.limitPlainStreamRange(it.args[0] as String?)
            }
        }
    }
    val createRequestHooks = ::cronetCreateRequestMethods.dexMethodList.map { method ->
        method.toMethod() to limitRange
    }.toTypedArray()
    CronetUrlRequestBuildFingerprint.hookMethod(scopedHook(*createRequestHooks))

    // endregion
}
