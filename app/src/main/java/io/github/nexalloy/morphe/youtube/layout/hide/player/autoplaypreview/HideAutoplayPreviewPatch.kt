package io.github.nexalloy.morphe.youtube.layout.hide.player.autoplaypreview

import android.view.View
import android.view.ViewStub
import app.morphe.extension.shared.Utils
import app.morphe.extension.youtube.patches.HideAutoplayPreviewPatch as ExtensionHideAutoplayPreviewPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

/**
 * Adds an option to hide the autoplay preview ("up next") at the end of videos.
 *
 * The upstream patcher skips inflating the autonav preview ViewStub in the player layout.
 * In the Xposed port the same effect is achieved by intercepting ViewStub.setVisibility:
 * when YouTube tries to show the preview stub, the visibility change is suppressed, so the
 * stub never inflates.
 */
val HideAutoplayPreview = patch(
    name = "Hide autoplay preview",
    description = "Adds an option to hide the autoplay preview at the end of videos.",
) {
    PreferenceScreen.PLAYER.addPreferences(
        SwitchPreference("morphe_hide_autoplay_preview", summary = true),
    )

    val previewStubId by lazy {
        runCatching {
            Utils.getContext().resources.getIdentifier(
                "autonav_preview_stub",
                "id",
                Utils.getContext().packageName,
            )
        }.getOrDefault(0)
    }

    DexMethod("Landroid/view/ViewStub;->setVisibility(I)V").hookMethod {
        before { param ->
            if (param.thisObject is ViewStub) {
                val stub = param.thisObject as ViewStub
                val visibility = param.args.firstOrNull() as? Int
                if (visibility == View.VISIBLE &&
                    previewStubId != 0 &&
                    stub.id == previewStubId &&
                    ExtensionHideAutoplayPreviewPatch.hideAutoplayPreview()
                ) {
                    param.result = null
                }
            }
        }
    }
}