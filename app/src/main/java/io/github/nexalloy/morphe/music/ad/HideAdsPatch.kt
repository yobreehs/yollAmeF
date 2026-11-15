package io.github.nexalloy.morphe.music.ad

import android.view.View
import app.morphe.extension.music.patches.HideAdsPatch
import app.morphe.extension.music.patches.components.AdsFilter
import io.github.nexalloy.morphe.music.misc.litho.filter.lithoFilterPatch
import io.github.nexalloy.morphe.music.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.shared.ad.HideFullscreenAds
import io.github.nexalloy.morphe.shared.ad.registerHideFullscreenAdsPreferences
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

val HideAds = patch(
    name = "Hide ads",
    description = "Adds options to hide fullscreen ads, Premium promotions and video ads.",
    settings = {
        registerHideFullscreenAdsPreferences(PreferenceScreen.ADS)

        PreferenceScreen.ADS.addPreferences(
            SwitchPreference("morphe_music_hide_get_premium_label"),
            SwitchPreference("morphe_music_hide_music_premium_promotions"),
            SwitchPreference("morphe_music_hide_video_ads"),
        )
    },
) {
    dependsOn(
        HideFullscreenAds(),
        lithoFilterPatch,
    )

    addLithoFilter(AdsFilter())

    // Hide 'Get Music Premium' label.
    val premiumView = ::getPremiumViewField.field
    HideGetPremiumFingerprint.hookMethod(scopedHook(::getPremiumSetVisibilityMethod.method) {
        before {
            if (it.thisObject === premiumView.get(outerParam.thisObject)) {
                it.args[0] = View.GONE
            }
        }
    })

    MembershipSettingsFingerprint.hookMethod {
        before {
            if (HideAdsPatch.hideGetPremiumLabel()) it.result = null
        }
    }

    ::showVideoAds.hookMethod {
        before { param ->
            param.args[0] = HideAdsPatch.hideVideoAds(param.args[0] as Boolean)
        }
    }

    FloatingLayoutFingerprint.hookMethod {
        after { HideAdsPatch.hidePremiumPromotionBottomSheet(it.result as View) }
    }
}
