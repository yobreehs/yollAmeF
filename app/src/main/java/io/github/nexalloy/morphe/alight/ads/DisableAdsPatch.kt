package io.github.nexalloy.morphe.alight.ads

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

/**
 * Blocks ad loading through the bundled Google AdMob SDK (which is how Alight
 * Motion serves banners, interstitials and rewarded ads, including the
 * "watch ad to unlock" flows).
 *
 * Instead of touching the app's own (obfuscated) ad controller, the public
 * AdMob entry points are short-circuited so no ad ever loads and the UI simply
 * never receives a loaded ad. Each hook is guarded — a missing method on some
 * AdMob version won't fail the whole patch.
 */
val DisableAds = patch(
    name = "Disable ads",
    description = "Blocks all AdMob interstitial, rewarded and banner ad loads.",
) {
    fun noOp(smali: String) {
        runCatching {
            DexMethod(smali).hookMethod(XC_MethodReplacement.returnConstant(null))
        }.onFailure { XposedBridge.log(it) }
    }

    // Interstitial ads
    noOp("Lcom/google/android/gms/ads/interstitial/InterstitialAd;->load(Landroid/content/Context;Ljava/lang/String;Lcom/google/android/gms/ads/AdRequest;Lcom/google/android/gms/ads/interstitial/InterstitialAdLoadCallback;)V")
    noOp("Lcom/google/android/gms/ads/interstitial/InterstitialAd;->show(Landroid/app/Activity;)V")

    // Rewarded ads ("watch ad to unlock ...")
    noOp("Lcom/google/android/gms/ads/rewarded/RewardedAd;->load(Landroid/content/Context;Ljava/lang/String;Lcom/google/android/gms/ads/AdRequest;Lcom/google/android/gms/ads/rewarded/RewardedAdLoadCallback;)V")
    noOp("Lcom/google/android/gms/ads/rewarded/RewardedAd;->show(Landroid/app/Activity;Lcom/google/android/gms/ads/OnUserEarnedRewardListener;)V")
    noOp("Lcom/google/android/gms/ads/rewardedinterstitial/RewardedInterstitialAd;->load(Landroid/content/Context;Ljava/lang/String;Lcom/google/android/gms/ads/AdRequest;Lcom/google/android/gms/ads/rewardedinterstitial/RewardedInterstitialAdLoadCallback;)V")
    noOp("Lcom/google/android/gms/ads/rewardedinterstitial/RewardedInterstitialAd;->show(Landroid/app/Activity;Lcom/google/android/gms/ads/OnUserEarnedRewardListener;)V")

    // Banner ads
    noOp("Lcom/google/android/gms/ads/admanager/AdManagerAdView;->loadAd(Lcom/google/android/gms/ads/AdRequest;)V")
    noOp("Lcom/google/android/gms/ads/BaseAdView;->loadAd(Lcom/google/android/gms/ads/AdRequest;)V")
}