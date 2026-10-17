/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.shared.misc.auth

import app.morphe.extension.shared.innertube.utils.AuthUtils
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.morphe.shared.misc.request.buildRequestPatch
import io.github.nexalloy.morphe.shared.misc.request.hookBuildRequest
import io.github.nexalloy.patch

internal fun authHookPatch(
    emptyPageIdHook: () -> Boolean,
    block: PatchExecutor.() -> Unit,
) = patch(
    description = "Hook to get the parameters required for account authentication."
) {
    block()
    dependsOn(buildRequestPatch)

    ::accountPageIdMethod.hookMethod {
        after { AuthUtils.setPageId(it.result as String?) }
    }

    if (emptyPageIdHook()) {
        val pageIdField = ::accountPageIdField.field
        ::accountIsEmptyPageIdMethod.hookMethod {
            before { AuthUtils.setPageId(pageIdField.get(it.thisObject) as String?) }
        }
    }

    ::accountIncognitoMethods.dexMethodList.forEach { method ->
        method.hookMethod {
            after { AuthUtils.setIncognitoStatus(it.result as Boolean) }
        }
    }

    hookBuildRequest { url, headers ->
        AuthUtils.setRequestHeaders(url, headers)
        headers
    }
}
