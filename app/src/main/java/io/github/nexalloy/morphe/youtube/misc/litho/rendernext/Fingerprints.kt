package io.github.nexalloy.morphe.youtube.misc.litho.rendernext

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.string

/**
 * RenderNext (ElementsView) is a Google UI library alternative to Litho, enabled by some A/B
 * tests. Items rendered with it are not seen by the Litho filters and hooks (so e.g. Shorts on
 * the home feed are never filtered).
 *
 * Ported from Morphe v1.45.0:
 * `patches/src/main/kotlin/app/morphe/patches/youtube/misc/litho/rendernext/Fingerprints.kt`
 */
const val RenderNextFlagLiteral = 45661418L

/**
 * The no-argument getter of the RenderNext flag (contains the flag literal).
 */
internal object RenderNextFeatureFlagFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(RenderNextFlagLiteral)
    )
)

/**
 * Reads the enable_rendernext field of an Element proto, set by the server.
 * If true, the element is presented with an ElementsView instead of Litho.
 */
internal object RenderNextEnablementCheckFingerprint : Fingerprint(
    returnType = "Z",
    filters = listOf(
        string("Failed to read Element proto passed in to RenderNextEnablementCheck: ")
    )
)

/**
 * Checks if a component identifier is in the comma separated list of the flag 45661551
 * ("template" or "parentTemplate:template"). If true, the Litho component is converted to RenderNext.
 */
internal object RenderNextTemplateCheckFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L", "Ljava/lang/String;", "Ljava/lang/String;"),
    filters = listOf(
        literal(58),
        methodCall(smali = "Ljava/lang/String;->indexOf(I)I"),
        methodCall(smali = "Ljava/lang/String;->substring(II)Ljava/lang/String;"),
    )
)