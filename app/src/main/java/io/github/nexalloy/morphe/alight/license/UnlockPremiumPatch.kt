package io.github.nexalloy.morphe.alight.license

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

/**
 * Unlocks every Alight Motion Pro feature:
 * - removes the export watermark ("RemoveWatermark" benefit),
 * - enables premium effects / member effects / advanced easing / layer parenting /
 *   camera objects / cloud storage tiers / project sharing / infinite sharing,
 * - un-gates high-resolution & high-frame-rate export (PremiumFeatures),
 * - removes the "watch ad to unlock" gates.
 *
 * Works by forcing `PurchaseState.getActiveBenefits()` to return every feature
 * that the app knows about.
 */
val UnlockPremium = patch(
    name = "Unlock premium",
    description = "Unlocks all Alight Motion Pro features and removes the export watermark.",
) {
    val enumClass = PremiumFeaturesEnumClinitFingerprint.declaredClass
    val values = enumClass.getMethod("values").invoke(null) as Array<*>
    val allFeatures = HashSet(values.asList())

    ActiveBenefitsFingerprint.hookMethod(XC_MethodReplacement.returnConstant(allFeatures))
}