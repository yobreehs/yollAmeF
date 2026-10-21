package io.github.nexalloy.morphe.youtube.misc.feedcards

import io.github.nexalloy.morphe.Fingerprint

/**
 * Litho is not obfuscated, and the method overrides the framework,
 * so this matches by name on every supported app version.
 */
internal object FeedCardComponentHostLayoutFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/litho/ComponentHost;",
    name = "onLayout",
    returnType = "V",
    parameters = listOf("Z", "I", "I", "I", "I")
)
