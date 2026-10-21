package io.github.nexalloy.morphe.shared.misc.spoof

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import java.lang.reflect.Modifier

/**
 * Builds a Cronet url request, which every network request of the app is made with.
 */
internal object CronetUrlRequestBuildFingerprint : Fingerprint(
    name = "build",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lorg/chromium/net/ExperimentalUrlRequest;",
    parameters = listOf(),
    filters = listOf(
        // Creates the request with the url and the other values of the builder.
        methodCall(returnType = "Lorg/chromium/net/ExperimentalUrlRequest;")
    ),
    custom = {
        declaredClass { superClass { descriptor = "Lorg/chromium/net/ExperimentalUrlRequest\$Builder;" } }
    }
)

internal object CronetUrlRequestSetHttpMethodFingerprint : Fingerprint(
    classFingerprint = CronetUrlRequestBuildFingerprint,
    name = "setHttpMethod",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Lorg/chromium/net/ExperimentalUrlRequest\$Builder;",
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        )
    )
)

internal val cronetCreateRequestMethods = findMethodListDirect {
    val dispatch = CronetUrlRequestBuildFingerprint.instructionMatches.last().instruction.methodRef!!
    val engineType = dispatch.declaredClass!!.descriptor
    // The call can reference an abstract engine method. Xposed needs its concrete overrides.
    Fingerprint(
        name = dispatch.name,
        returnType = dispatch.returnType!!.descriptor,
        parameters = dispatch.paramTypes.map { it.descriptor },
    ).matchAll().map { it.method }.filter { method ->
        !Modifier.isAbstract(method.modifiers) && !Modifier.isStatic(method.modifiers) &&
                generateSequence(method.declaredClass) { it.superClass }.any { it.descriptor == engineType }
    }.also { check(it.isNotEmpty()) { "No concrete Cronet request creation methods found" } }
}

internal val cronetUrlField = findFieldDirect {
    CronetUrlRequestBuildFingerprint().declaredClass!!.fields.single {
        it.type.descriptor == "Ljava/lang/String;" && Modifier.isFinal(it.modifiers)
    }
}

internal val cronetUploadField = findFieldDirect {
    CronetUrlRequestBuildFingerprint().declaredClass!!.fields.single {
        it.type.descriptor == "Lorg/chromium/net/UploadDataProvider;"
    }
}

internal val cronetHttpMethodField = findFieldDirect {
    CronetUrlRequestSetHttpMethodFingerprint.instructionMatches.first().instruction.fieldRef!!
}
