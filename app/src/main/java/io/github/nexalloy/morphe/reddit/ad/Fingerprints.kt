package io.github.nexalloy.morphe.reddit.ad

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.string


internal object ListingFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/domain/model/listing/Listing;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            smali = "Lcom/reddit/domain/model/listing/Listing;->children:Ljava/util/List;"
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            smali = "Lcom/reddit/domain/model/listing/Listing;->after:Ljava/lang/String;"
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            smali = "Lcom/reddit/domain/model/listing/Listing;->before:Ljava/lang/String;"
        )
    )
)

// Class appears to be removed in 2026.16.0+
@RequireAppVersion(maxVersion = "2026.16.0")
internal object SubmittedListingFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/domain/model/listing/SubmittedListing;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            smali = "Lcom/reddit/domain/model/listing/SubmittedListing;->children:Ljava/util/List;"
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            smali = "Lcom/reddit/domain/model/listing/SubmittedListing;->videoUploads:Ljava/util/List;"
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            smali = "Lcom/reddit/domain/model/listing/SubmittedListing;->after:Ljava/lang/String;"
        )
    )
)

private object AdPostSectionToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string("AdPostSection(linkId=")
    )
)

internal object AdPostSectionConstructorFingerprint : Fingerprint(
    classFingerprint = AdPostSectionToStringFingerprint,
    name = "<init>",
    returnType = "V"
)

/**
 * 2026.04+
 */
@RequireAppVersion("2026.04.0")
internal object CommentsAdStateToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("CommentsAdState(conversationAdViewState="),
        string(", adsLoadCompleted="),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN)
    )
)

@RequireAppVersion("2026.04.0")
internal object CommentsAdStateConstructorFingerprint : Fingerprint(
    classFingerprint = CommentsAdStateToStringFingerprint,
    name = "<init>",
    returnType = "V"
)

@get:RequireAppVersion("2026.04.0")
val adsLoadCompletedField = findFieldDirect {
    CommentsAdStateToStringFingerprint.instructionMatches.last().instruction.fieldRef!!
}

internal object CommentsViewModelAdLoaderFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/comments/presentation/CommentsViewModel;",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "L", "I"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            name = "<init>",
            parameters = listOf("Z", "I"),
            returnType = "V"
        )
    )
)

internal object ImmutableListBuilderFingerprint : Fingerprint(
    name = "<clinit>",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            definingClass = "Lcom/reddit/accessibility/AutoplayVideoPreviewsOption;",
            name = "getEntries"
        ),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Ljava/lang/Iterable;")
        )
    )
)

val ImmutableListBuilderReference = findMethodDirect {
    ImmutableListBuilderFingerprint.instructionMatches
        .last().instruction.methodRef!!
}