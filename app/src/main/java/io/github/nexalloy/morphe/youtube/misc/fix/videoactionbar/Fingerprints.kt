package io.github.nexalloy.morphe.youtube.misc.fix.videoactionbar

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findFieldListDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.youtube.shared.CLIENT_INFO_CLASS
import org.luckypray.dexkit.DexKitBridge

context(dexkit: DexKitBridge)
private fun requestUriMatches() = Fingerprint(
    returnType = "Landroid/net/Uri\$Builder;",
    parameters = listOf(),
    filters = listOf(
        string("key"),
        string("asig"),
        checkCast("Ljava/lang/String;"),
        methodCall("Landroid/net/Uri\$Builder;->appendQueryParameter(Ljava/lang/String;Ljava/lang/String;)Landroid/net/Uri\$Builder;"),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterWithin(5))
    )
).matchAllOrNull() ?: Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string("key"),
        string("asig"),
        checkCast("Ljava/lang/String;"),
        methodCall("Landroid/net/Uri\$Builder;->appendQueryParameter(Ljava/lang/String;Ljava/lang/String;)Landroid/net/Uri\$Builder;"),
        methodCall("Landroid/net/Uri\$Builder;->build()Landroid/net/Uri;")
    )
).matchAll()

@get:RequireAppVersion("20.30.000")
val innerTubeRequestBodyMethods = findMethodListDirect {
    requestUriMatches().flatMap { uri ->
        Fingerprint(
            definingClass = uri.method.declaredClass!!.descriptor,
            parameters = listOf("L"),
            returnType = "Lcom/google/protobuf/MessageLite;",
            filters = listOf(fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = CLIENT_INFO_CLASS))
        ).matchAll().map { it.method }
    }.distinct()
}

context(dexkit: DexKitBridge)
private fun requestFields() = requestUriMatches().map { uri ->
    val instructions = uri.method.instructions
    val appendEndpoint = instructions.indexOfLast { it.methodRef?.name == "appendEncodedPath" }
    check(appendEndpoint >= 0)
    val endpointRegister = instructions[appendEndpoint].register(1)
    val endpointIndex = instructions.take(appendEndpoint).indexOfLast {
        it.opcode == Opcode.IGET_OBJECT.ordinal && it.registerOrNull(0) == endpointRegister
    }
    check(endpointIndex >= 0)
    val requestRegister = instructions[endpointIndex].register(1)
    val request = instructions.take(endpointIndex).last {
        it.opcode == Opcode.IGET_OBJECT.ordinal && it.registerOrNull(0) == requestRegister
    }.fieldRef!!
    request to instructions[endpointIndex].fieldRef!!
}

@get:RequireAppVersion("20.30.000")
val innerTubeRequestFields = findFieldListDirect { requestFields().map { it.first }.distinct() }

@get:RequireAppVersion("20.30.000")
val innerTubeEndpointFields = findFieldListDirect { requestFields().map { it.second }.distinct() }

@get:RequireAppVersion("20.30.000")
val configInfoField = findFieldDirect {
    Fingerprint(
        definingClass = requestUriMatches().first().method.declaredClass!!.descriptor,
        parameters = listOf("L"),
        returnType = "Lcom/google/protobuf/MessageLite;",
        filters = listOf(fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = CLIENT_INFO_CLASS))
    ).instructionMatches.first().instruction.fieldRef!!
}

context(dexkit: DexKitBridge)
private fun configInfoFields() = Fingerprint(
    definingClass = configInfoField(dexkit).typeSign,
    name = "<init>",
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this", type = "Ljava/lang/String;"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = "this", type = "Ljava/lang/String;")
    )
).instructionMatches.map { it.instruction.fieldRef!! }

@get:RequireAppVersion("20.30.000")
val coldConfigDataField = findFieldDirect { configInfoFields()[0] }

@get:RequireAppVersion("20.30.000")
val coldHashDataField = findFieldDirect { configInfoFields()[1] }

@get:RequireAppVersion("20.30.000")
val configInfoBuildMethod = findMethodDirect {
    val method = innerTubeRequestBodyMethods().first()
    val assignment = method.instructions.indexOfFirst {
        it.opcode == Opcode.IPUT_OBJECT.ordinal && it.fieldRef == configInfoField()
    }
    method.instructions.take(assignment).last { it.methodRef?.name == "build" }.methodRef!!
}
