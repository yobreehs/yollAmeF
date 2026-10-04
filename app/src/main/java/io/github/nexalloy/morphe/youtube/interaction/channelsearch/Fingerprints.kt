package io.github.nexalloy.morphe.youtube.interaction.channelsearch

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.anyInstruction
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.indexOfFirstInstruction
import io.github.nexalloy.morphe.indexOfFirstInstructionReversed
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData

/**
 * Every browse page is shown by this one fragment, and the endpoint it is handed names the page.
 * The browse request cannot be used instead, because a page served from cache makes no request.
 */
internal object ChannelBrowseFragmentOnCreateViewFingerprint : Fingerprint(
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    filters = listOf(
        string("Browse Fragment was given a navigation endpoint without browse data."),
    ),
)

/**
 * The search feed is not a browse page, and returning to it from a channel sets no browse id.
 */
internal object ChannelSearchResultsFragmentOnCreateViewFingerprint : Fingerprint(
    returnType = "Landroid/view/View;",
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;",
    ),
    filters = listOf(
        string("search_cache_key"),
    ),
)

/**
 * Picks the hint of the search box. The default hint is loaded last, after the hints of
 * Shorts search and playlist search.
 */
internal object ChannelSearchBoxHintFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/String;",
    parameters = listOf("Landroid/content/Context;", "Z", "Z", "Z", "Z"),
    filters = listOf(
        resourceLiteral(ResourceType.STRING, "shorts_search_hint"),
        resourceLiteral(ResourceType.STRING, "playlists_search_hint"),
        resourceLiteral(ResourceType.STRING, "search_hint"),
        opcode(Opcode.MOVE_RESULT_OBJECT),
    ),
)

/**
 * Every search submit path funnels through this method, including suggestions and filter chips.
 */
internal object ChannelSearchSubmitFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(
        "Ljava/lang/String;",
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
    ),
    filters = listOf(
        anyInstruction(
            methodCall( // 21.31+
                parameters = listOf(
                    "Ljava/lang/String;",
                    "[B",
                    "Ljava/lang/String;",
                    "I",
                    "L",
                    "L",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Z",
                ),
                returnType = "V",
            ),
            methodCall( // 21.30 and older.
                parameters = listOf(
                    "Ljava/lang/String;",
                    "[B",
                    "Ljava/lang/String;",
                    "I",
                    "L",
                    "L",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                    "Ljava/lang/String;",
                ),
                returnType = "V",
            ),
        ),
    ),
)

/**
 * The index of the instruction reading the endpoint field of the fragment ("this"), right
 * before the endpoint is checked for browse data.
 */
private fun MethodData.channelBrowseEndpointIndex(browseDataIndex: Int): Int =
    indexOfFirstInstructionReversed(browseDataIndex) {
        opcode == Opcode.IGET_OBJECT.ordinal &&
            fieldRef?.declaredClass?.descriptor == this@channelBrowseEndpointIndex.declaredClass?.descriptor
    }

/**
 * Browse data is an extension of the endpoint, and reading it starts with this field.
 */
private fun MethodData.channelBrowseDataExtensionField(endpointIndex: Int): FieldData {
    val browseDataExtensionIndex = indexOfFirstInstruction(endpointIndex + 1) {
        opcode == Opcode.SGET_OBJECT.ordinal
    }
    return instructions[browseDataExtensionIndex].fieldRef
        ?: error("Browse data extension field not found")
}

/**
 * The static method that reads the browse id from an endpoint (obfuscated endpoint type).
 * Used by the browse-id setter hook to extract the channel id before scoping the search.
 *
 * Some app versions contain two semantically identical obfuscated getters reading the same
 * browse-data extension field; either works, so a deterministic first match is used.
 */
val channelBrowseIdMethod = findMethodDirect {
    val method = ChannelBrowseFragmentOnCreateViewFingerprint.run()
    val browseDataIndex = ChannelBrowseFragmentOnCreateViewFingerprint.instructionMatches.first().index
    val endpointIndex = method.channelBrowseEndpointIndex(browseDataIndex)
    val endpointField = method.instructions[endpointIndex].fieldRef
        ?: error("Browse fragment endpoint field not found")
    val browseDataExtensionField = method.channelBrowseDataExtensionField(endpointIndex)

    findMethod {
        matcher {
            returnType = "java.lang.String"
            paramTypes(endpointField.typeName)
        }
    }.firstOrNull { candidate ->
        candidate.instructions.any {
            it.opcode == Opcode.SGET_OBJECT.ordinal &&
                it.fieldRef?.descriptor == browseDataExtensionField.descriptor
        }
    } ?: error("Browse id getter method not found")
}

/**
 * The method that stores the endpoint into the fragment (IPUT_OBJECT of the endpoint field).
 * Hooked after the store to keep the channel browse id current.
 */
val channelBrowseIdSetterMethod = findMethodDirect {
    val method = ChannelBrowseFragmentOnCreateViewFingerprint.run()
    val browseDataIndex = ChannelBrowseFragmentOnCreateViewFingerprint.instructionMatches.first().index
    val endpointIndex = method.channelBrowseEndpointIndex(browseDataIndex)
    val endpointField = method.instructions[endpointIndex].fieldRef
        ?: error("Browse fragment endpoint field not found")

    Fingerprint(
        returnType = "V",
        parameters = listOf(endpointField.type.descriptor),
        filters = listOf(
            fieldAccess(reference = endpointField, opcode = Opcode.IPUT_OBJECT),
        ),
    ).run()
}