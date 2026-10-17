package io.github.nexalloy.morphe.youtube.video.codecs

import android.view.Display
import app.morphe.extension.youtube.patches.DisableVideoCodecsPatch
import app.morphe.extension.youtube.settings.preference.VideoCodecListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

val DisableVideoCodecs = patch(
    name = "Disable video codecs",
    description = "Adds options to disable or force HDR, and to disable VP9 or AV1 codecs.",
) {
    PreferenceScreen.VIDEO.addPreferences(
        noTitleUnsortedPreferenceCategory(
            SwitchPreference("morphe_disable_hdr_video"),
            SwitchPreference("morphe_force_hdr_video", summary = true),
        ),
        ListPreference(
            key = "morphe_video_codec",
            tag = VideoCodecListPreference::class.java
        )
    )

    DexMethod($$"Landroid/view/Display$HdrCapabilities;->getSupportedHdrTypes()[I").hookMethod {
        val guard = ThreadLocal<Boolean>()
        after {
            if (guard.get() == true) {
                return@after
            }

            guard.set(true)
            try {
                it.result =
                    DisableVideoCodecsPatch.overrideSupportedHdrTypes(it.thisObject as Display.HdrCapabilities)
            } finally {
                guard.remove()
            }
        }
    }

    Vp9CapabilityFingerprint.hookMethod {
        before {
            if (!DisableVideoCodecsPatch.allowVP9()) {
                it.result = false
            }
        }
    }
    CodecSupportCheckFingerprint.hookMethod {
        before {
            if (!DisableVideoCodecsPatch.allowCodec(it.args[1] as String)) {
                it.result = false
            }
        }
    }
}
