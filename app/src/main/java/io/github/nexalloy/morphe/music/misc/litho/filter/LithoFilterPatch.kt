package io.github.nexalloy.morphe.music.misc.litho.filter

import io.github.nexalloy.morphe.shared.misc.litho.context.conversionContextPatch
import io.github.nexalloy.morphe.shared.misc.litho.filter.sharedLithoFilterPatch

/**
 * Litho filter hook for YT Music.
 *
 * YT Music does not ship the Upb feature flag and always uses the native buffer path,
 * so the legacy branches are disabled.
 */
val lithoFilterPatch = sharedLithoFilterPatch(
    hookNonNativeBuffer = { false },
    overrideUpbFeatureFlag = { false },
    useLegacyLithoFiltering = { false }
) {
    dependsOn(conversionContextPatch)
}
