package io.github.nexalloy.morphe.music.misc.debugging

import io.github.nexalloy.morphe.music.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.shared.misc.debugging.enableDebuggingPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference

val EnableDebugging = enableDebuggingPatch(
    // String feature flag does not appear to be present with YT Music.
    hookStringFeatureFlag = { false },
    hookLongFeatureFlag = { true },
    hookDoubleFeatureFlag = { true },
    preferenceScreen = PreferenceScreen.MISC,
    additionalDebugPreferences = listOf(
        SwitchPreference(
            "morphe_debug_protobuffer",
            summary = true
        )
    )
)