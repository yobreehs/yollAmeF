package io.github.nexalloy.revanced.meta.ads

import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.strings

val adInjectorFingerprint = findMethodDirect {
    val current = runCatching {
        findMethod {
            matcher { strings("Is ad pod") }
        }.firstOrNull { it.returnTypeName == "boolean" }
    }.getOrNull()
    if (current != null) return@findMethodDirect current

    val legacyMarkers = listOf(
        "SponsoredContentController.insertItem",
        "SponsoredContentController.processValidatedContent",
    )
    legacyMarkers.firstNotNullOfOrNull { marker ->
        runCatching {
            findMethod { matcher { strings(marker) } }.firstOrNull()
        }.getOrNull()
    } ?: error("Instagram ad injector fingerprint not found")
}
