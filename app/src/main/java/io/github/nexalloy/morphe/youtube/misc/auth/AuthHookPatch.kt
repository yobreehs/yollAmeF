/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.youtube.misc.auth

import io.github.nexalloy.morphe.shared.misc.auth.authHookPatch
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_21_02_or_greater

internal val authHookPatch = authHookPatch(
    emptyPageIdHook = { is_21_02_or_greater },
    block = { dependsOn(VersionCheck) }
)
