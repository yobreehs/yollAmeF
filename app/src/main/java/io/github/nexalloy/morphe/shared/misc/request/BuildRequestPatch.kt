package io.github.nexalloy.morphe.shared.misc.request

import io.github.nexalloy.patch

internal val buildRequestPatch = patch(description = "buildRequestPatch") {
    val urlMethod = ::requestUrlMethod.method
    val builder = BuildRequestFingerprint.dexMethod
    val mapIndex = builder.paramTypeNames.indexOf("java.util.Map")
    check(mapIndex >= 0)
    builder.hookMethod {
        before {
            @Suppress("UNCHECKED_CAST")
            var headers = it.args[mapIndex] as MutableMap<String, String>?
            val url = urlMethod(it.args[0]) as String?
            hooks.forEach { hook -> headers = hook(url, headers) }
            it.args[mapIndex] = headers
        }
    }
}

private val hooks = mutableListOf<(String?, MutableMap<String, String>?) -> MutableMap<String, String>?>()

internal fun hookBuildRequest(hook: (String?, MutableMap<String, String>?) -> MutableMap<String, String>?) {
    hooks.add(hook)
}
