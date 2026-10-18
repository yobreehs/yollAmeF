package io.github.nexalloy.morphe.youtube.video.videoid

import io.github.nexalloy.morphe.findMethodDirect

val videoIdFingerprint = findMethodDirect {
    findMethod {
        matcher {
            addEqString("Null initialPlayabilityStatus")
        }
    }.single()
}

val PlayerResponseModel_getVideoId = findMethodDirect {
    videoIdFingerprint().let { method ->
        method.invokes.distinct().single {
            it.returnTypeName == "java.lang.String" && it.declaredClass == method.paramTypes[0] // PlayerResponseModel, interface
        }
    }
}
