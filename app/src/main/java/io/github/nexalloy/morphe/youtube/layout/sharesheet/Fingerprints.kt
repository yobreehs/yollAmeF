package io.github.nexalloy.morphe.youtube.layout.sharesheet

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

/**
 * Command resolver of the share endpoint.
 * Builds and sends the `share/get_share_panel` request,
 * whose response opens the in-app (Litho) share sheet.
 */
@StrictQuery
internal object ShareEndpointCommandFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            smali = "Ljava/util/Iterator;->next()Ljava/lang/Object;"
        ),
        opcode(opcode = Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        checkCast(type = "Landroid/content/pm/ResolveInfo;", location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            smali = "Landroid/content/pm/ResolveInfo;->activityInfo:Landroid/content/pm/ActivityInfo;",
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            smali = "Landroid/content/pm/ActivityInfo;->applicationInfo:Landroid/content/pm/ApplicationInfo;",
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            smali = "Landroid/content/pm/ApplicationInfo;->packageName:Ljava/lang/String;",
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            smali = "Landroid/content/pm/ResolveInfo;->activityInfo:Landroid/content/pm/ActivityInfo;",
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            smali = "Landroid/content/pm/ActivityInfo;->name:Ljava/lang/String;",
            location = MatchAfterImmediately()
        ),
        opcode(opcode = Opcode.IF_EQZ, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            smali = "Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * Action sheet controller method that handles `ShowActionSheetCommand`.
 * The share button in flyout menus opens an empty action sheet (loading spinner)
 * and then resolves the share endpoint to fill it.
 */
internal object ShowActionSheetCommandFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "L"),
    filters = listOf(string("ShowActionSheetCommand needs to provided."))
)

val dismissActionSheetMethod = findMethodDirect {
    // The only method of the class that takes an optional sheet id.
    ShowActionSheetCommandFingerprint().declaredClass!!.methods.single {
        it.returnTypeName == "void" && it.paramCount == 1 &&
                it.paramTypes.single().descriptor.endsWith("/util/Optional;")
    }
}
