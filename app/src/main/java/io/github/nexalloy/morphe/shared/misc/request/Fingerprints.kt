package io.github.nexalloy.morphe.shared.misc.request

import io.github.nexalloy.TargetApp
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall

@StrictQuery
@TargetApp("youtube")
internal object BuildRequestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Lorg/chromium/net/UrlRequest", // UrlRequest; or UrlRequest$Builder;
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "newUrlRequestBuilder",
            parameters = listOf(
                "Ljava/lang/String;",
                "Lorg/chromium/net/UrlRequest\$Callback;",
                "Ljava/util/concurrent/Executor;"
            )
        ),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            smali = "Ljava/util/Map;->entrySet()Ljava/util/Set;",
            location = MatchAfterWithin(15)
        ),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, name = "ordinal"),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, name = "setPriority")
    )
)

@get:TargetApp("youtube")
val requestUrlMethod = findMethodDirect {
    val method = BuildRequestFingerprint()
    val builder = method.instructions.indexOfFirst { it.methodRef?.name == "newUrlRequestBuilder" }
    method.instructions.take(builder).last {
        it.methodRef?.returnType?.descriptor == "Ljava/lang/String;" &&
                it.methodRef?.declaredClass == method.paramTypes.first()
    }.methodRef!!
}
