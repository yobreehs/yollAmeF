package io.github.nexalloy.morphe.alight.license

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.enumValueOf
import io.github.nexalloy.patch
import io.github.nexalloy.setStaticObjectField
import org.luckypray.dexkit.wrap.DexMethod

/**
 * Neutralizes the PairIP license gate (`com.pairip.licensecheck.LicenseClient`).
 *
 * Alight Motion ships Google's PairIP / Play Licensing V2 protection: at startup it
 * binds `com.android.vending`'s licensing service and verifies the app was installed
 * from the Play Store. When that check fails (no GApps, tampered install, offline)
 * the app launches [LicenseActivity] with a paywall/error dialog and closes itself.
 *
 * This patch:
 * - always reports the license response as LICENSED (code 0),
 * - disables the local installer check (installed-from-Play verification),
 * - no-ops the paywall / error-dialog activities that would otherwise close the app,
 * - pins the license check state to FULL_CHECK_OK.
 */
val DisableLicenseCheck = patch(
    name = "Disable license check",
    description = "Bypasses the PairIP/Play licensing gate so the app never shows the license paywall or closes itself.",
) {
    runCatching {
        DexMethod("Lcom/pairip/licensecheck/LicenseClient;->processResponse(ILandroid/os/Bundle;)V").hookMethod {
            before { param ->
                // Response code 0 == LICENSED, 2 == NOT_LICENSED, 3 == invalid package.
                if (param.args[0] != 0) param.args[0] = 0
            }
        }
    }.onFailure { XposedBridge.log(it) }

    runCatching {
        DexMethod("Lcom/pairip/licensecheck/LicenseClient;->startPaywallActivity(Landroid/app/PendingIntent;)V")
            .hookMethod(XC_MethodReplacement.returnConstant(null))
    }.onFailure { XposedBridge.log(it) }

    runCatching {
        DexMethod("Lcom/pairip/licensecheck/LicenseClient;->startErrorDialogActivity()V")
            .hookMethod(XC_MethodReplacement.returnConstant(null))
    }.onFailure { XposedBridge.log(it) }

    runCatching {
        DexMethod("Lcom/pairip/licensecheck/LicenseClient;->performLocalInstallerCheck()Z")
            .hookMethod(XC_MethodReplacement.returnConstant(true))
    }.onFailure { XposedBridge.log(it) }

    // Pin the singleton state so repeated/re-scheduled checks also pass.
    runCatching {
        val licenseClientClass =
            "com.pairip.licensecheck.LicenseClient".let { cn -> classLoader.loadClass(cn) }
        val stateClass =
            classLoader.loadClass("com.pairip.licensecheck.LicenseClient\$LicenseCheckState")
        val fullCheckOk = stateClass.enumValueOf("FULL_CHECK_OK")
        licenseClientClass.setStaticObjectField("licenseCheckState", fullCheckOk)
        licenseClientClass.setStaticObjectField("localCheckEnabled", true)
    }.onFailure { XposedBridge.log(it) }
}