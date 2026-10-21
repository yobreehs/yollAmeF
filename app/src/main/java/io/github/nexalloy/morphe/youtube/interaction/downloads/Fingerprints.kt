package io.github.nexalloy.morphe.youtube.interaction.downloads

import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import java.lang.reflect.Modifier
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassData
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.anyInstruction
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Opcode

internal object OfflineVideoEndpointFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Ljava/util/Map;",
        "L",
        "Ljava/lang/String", // Video ID
        "L",
    ),
    filters = listOf(
        anyInstruction(
            string("Unsupported Offline Video Action: "), // 21.14 and lower
            string("Unsupported Offline Video Action: %s") // 21.15+
        )
    ),
    custom = {
        addAnyOf { addEqString("Unsupported Offline Video Action: ") }
        addAnyOf { addEqString("Unsupported Offline Video Action: %s") }
    }
)

/**
 * Matches the offline video endpoint method, to find the call that saves a video with its offlineability renderer.
 * The quality picker and the Premium upsell of the video are both shown by the called method.
 */
@StrictQuery
internal object SaveOfflineVideoCallFingerprint : Fingerprint(
    classFingerprint = OfflineVideoEndpointFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Ljava/util/Map;",
        "L",
        "Ljava/lang/String",
        "L",
    ),
    filters = listOf(
        methodCall(
            parameters = listOf(
                "Ljava/lang/String;", // Video ID
                "L", // Offlineability renderer
                "L",
                "L",
                "L",
                "I"
            ),
            returnType = "V"
        )
    )
)

/**
 * Resolves a command with the first command mapping that supports it.
 */
internal object ResolveCommandFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        anyInstruction(
            string("Unknown command not resolved"), // 20.x
            string("Unknown command not resolved %s") // 21.x
        )
    ),
    custom = {
        addAnyOf { addEqString("Unknown command not resolved") }
        addAnyOf { addEqString("Unknown command not resolved %s") }
    }
)

/**
 * Response callback of the 'ypc/get_offline_upsell' endpoint.
 */
internal object OfflineUpsellResponseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        checkCast("L"), // Response.
        literal(131899791), // Upsell renderer of the response.
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this"), // Endpoint command.
        string("YpcGetOfflineUpsellResponse_videoIdKey")
    )
)

/**
 * Fetches the player response a video is saved with, and puts it into the cache.
 */
internal object OfflinePlayerResponseFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        string("Some error occurred when reading from the cache player response"),
        methodCall(
            parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
            returnType = "Lcom/google/common/util/concurrent/ListenableFuture;"
        )
    )
)

/**
 * Creates a player response model from the bytes of a player response.
 */
internal fun playerResponseModelFromBytesFingerprint(playerResponseModel: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = playerResponseModel,
    parameters = listOf("[B", "J"),
    filters = listOf(
        methodCall(
            parameters = listOf("[B", "Lcom/google/protobuf/MessageLite;"),
            returnType = "Lcom/google/protobuf/MessageLite;"
        ),
        opcode(Opcode.CHECK_CAST) // Player response.
    )
)

internal fun playerResponseGetterFingerprint(playerResponseModelClass: String, playerResponse: String) = Fingerprint(
    definingClass = playerResponseModelClass,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = playerResponse,
    parameters = listOf()
)

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

internal val saveOfflineVideoMethod = findMethodDirect {
    SaveOfflineVideoCallFingerprint.instructionMatches.first().instruction.methodRef!!
}

internal val offlineUpsellCommandField = findFieldDirect {
    OfflineUpsellResponseFingerprint.instructionMatches[2].instruction.fieldRef!!
}

internal val playerResponseModelFromBytesMethod = findMethodDirect {
    playerResponseModelFromBytesFingerprint(OfflinePlayerResponseFingerprint().returnType!!.descriptor)
        .match().method
}

internal val playerResponseGetterMethod = findMethodDirect {
    val factory = playerResponseModelFromBytesMethod()
    val responseType = playerResponseModelFromBytesFingerprint(factory.returnType!!.descriptor)
        .instructionMatches.last().instruction.classRef!!.descriptor
    playerResponseGetterFingerprint(factory.declaredClass!!.descriptor, responseType).match().method
}

internal val offlinePlayerResponseFactoryMethod = findMethodDirect {
    val outer = OfflinePlayerResponseFingerprint.match()
    val modelType = outer.method.returnType!!.descriptor
    // Transform the freshly created model before the cache put, so both cache and caller receive it.
    outer.method.instructions.take(outer.instructionMatches.last().index).filter {
        (it.opcode == Opcode.INVOKE_VIRTUAL.opCode || it.opcode == Opcode.INVOKE_VIRTUAL_RANGE.opCode)
                && it.methodRef?.returnType?.descriptor == modelType
    }.map { it.methodRef!! }.distinctBy { it.descriptor }.single()
}

@SkipTest
internal object ProtobufClassParseByteArrayFingerprint : Fingerprint(
    name = "parseFrom",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("L", "[B")
)

internal object ProtobufClassParseByteArrayWithRegistryFingerprint : Fingerprint(
    classFingerprint = ProtobufClassParseByteArrayFingerprint,
    name = "parseFrom",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("L", "[B", "Lcom/google/protobuf/ExtensionRegistryLite;")
)

internal object GeneratedRegistryFingerprint : Fingerprint(
    definingClass = "Lcom/google/protobuf/ExtensionRegistryLite;",
    name = "getGeneratedRegistry",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Lcom/google/protobuf/ExtensionRegistryLite;",
    parameters = emptyList()
)

context(bridge: DexKitBridge)
private fun defaultInstance(type: ClassData) = type.fields.single {
    Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers) && it.type.descriptor == type.descriptor
}

internal val offlineCommandDefaultInstance = findFieldDirect {
    defaultInstance(ResolveCommandFingerprint().paramTypes.first())
}

internal val offlineabilityDefaultInstance = findFieldDirect {
    defaultInstance(saveOfflineVideoMethod().paramTypes[1])
}

internal val offlineUpsellDefaultInstance = findFieldDirect {
    defaultInstance(OfflineUpsellResponseFingerprint.instructionMatches.first().instruction.classRef!!)
}

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
