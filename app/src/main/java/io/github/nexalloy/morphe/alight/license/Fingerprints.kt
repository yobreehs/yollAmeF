package io.github.nexalloy.morphe.alight.license

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint

/**
 * `com.alightcreative.account.o#nr()` — PurchaseState.getActiveBenefits().
 *
 * Returns the set of premium features (`kgE.K` enum) the user currently owns.
 * The IAP manager (`com.alightcreative.account.n.Uo()`) and every other feature gate
 * (watermark removal, export quality, effects, easing, layering, camera objects,
 * cloud storage, project sharing, ...) reads this set, so forcing it to contain
 * every feature unlocks the whole app.
 *
 * Fingerprint: the only public instance method of `com.alightcreative.account.o`
 * that takes no arguments and returns a `Set`. The `com.alightcreative.account`
 * classes are not obfuscated across releases (they're @Keep'ed / Firebase-bound),
 * and within that class the `nt()` (activeBenefits) getter is unique.
 */
internal object ActiveBenefitsFingerprint : Fingerprint(
    definingClass = "Lcom/alightcreative/account/o;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/util/Set;",
    parameters = listOf(),
)

/**
 * Locates the premium features enum (`kgE.K` on this build).
 *
 * Matches the enum's `<clinit>`, which materializes the feature constants
 * ("RemoveWatermark", "CloudStorageHighTier", ...). The class that declares this
 * method is the enum itself, from which we take `values()` to build the
 * "everything unlocked" feature set at runtime without hard-coding the
 * obfuscated class name.
 */
internal object PremiumFeaturesEnumClinitFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf("RemoveWatermark", "CloudStorageHighTier"),
)