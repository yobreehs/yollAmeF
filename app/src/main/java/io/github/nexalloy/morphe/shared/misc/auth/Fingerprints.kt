/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package io.github.nexalloy.morphe.shared.misc.auth

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.TargetApp
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchFirst
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findFieldFromToString
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string
import org.luckypray.dexkit.result.FieldData

internal const val GET_PAGE_ID_STRING = ", getPageId="
internal const val IS_INCOGNITO_STRING = ", isIncognito="

@TargetApp("youtube")
internal object AccountIdentityToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf(GET_PAGE_ID_STRING, IS_INCOGNITO_STRING)
)

internal fun getIncognitoStatusFingerprint(incognitoField: FieldData) = object : Fingerprint(
    classFingerprint = AccountIdentityToStringFingerprint,
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            location = MatchFirst(),
            reference = incognitoField
        ),
        opcode(Opcode.RETURN, location = MatchAfterImmediately())
    )
) {}

@StrictQuery
internal class GetPageIdFingerprint(pageIdField: FieldData) : Fingerprint(
    classFingerprint = AccountIdentityToStringFingerprint,
    returnType = "Ljava/lang/String;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            location = MatchFirst(),
            reference = pageIdField
        ),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterImmediately())
    )
)

internal fun isEmptyPageIdFingerprint(pageIdField: FieldData) = object : Fingerprint(
    classFingerprint = AccountIdentityToStringFingerprint,
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            location = MatchFirst(),
            reference = pageIdField
        ),
        string("", location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Ljava/lang/String;->equals(Ljava/lang/Object;)Z",
            location = MatchAfterImmediately()
        )
    )
) {}

@get:TargetApp("youtube")
val accountPageIdField = findFieldDirect {
    AccountIdentityToStringFingerprint().findFieldFromToString(GET_PAGE_ID_STRING)
}

@get:TargetApp("youtube")
val accountIncognitoField = findFieldDirect {
    AccountIdentityToStringFingerprint().findFieldFromToString(IS_INCOGNITO_STRING)
}

@get:TargetApp("youtube")
val accountPageIdMethod = findMethodDirect {
    GetPageIdFingerprint(accountPageIdField())()
}

@get:TargetApp("youtube")
@get:RequireAppVersion(minVersion = "21.02.000")
val accountIsEmptyPageIdMethod = findMethodDirect {
    isEmptyPageIdFingerprint(accountPageIdField())()
}

@get:TargetApp("youtube")
val accountIncognitoMethods = findMethodListDirect {
    getIncognitoStatusFingerprint(accountIncognitoField()).matchAll().map { it.method }
}
