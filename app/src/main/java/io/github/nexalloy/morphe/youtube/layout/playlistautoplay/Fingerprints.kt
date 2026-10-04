package io.github.nexalloy.morphe.youtube.layout.playlistautoplay

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodListDirect

/**
 * The (obfuscated) navigation intent enum with NEXT/PREVIOUS/AUTOPLAY/AUTONAV/JUMP constants.
 * Two enum classes may carry the same constant names; the real one also declares a
 * constructor with more than two parameters (mirrors the upstream custom condition).
 */
val playlistAutoplayEnumClass = findClassDirect {
    this.findMethod {
        matcher {
            addUsingString("NEXT")
            addUsingString("PREVIOUS")
            addUsingString("AUTOPLAY")
            addUsingString("AUTONAV")
            addUsingString("JUMP")
        }
    }.firstOrNull { clinit ->
        clinit.declaredClass?.methods?.any { it.isConstructor && it.paramCount > 2 } == true
    }?.declaredClass ?: error("Navigation intent enum class not found")
}

/**
 * Wrapper class holding the navigation intent enum (public constructor with
 * (enum, Object, Object) parameters).
 */
val playlistAutoplayWrapperClass = findClassDirect {
    // NOTE: the app DSL converts descriptor parameters to dotted names; an enumType here must be
    // the descriptor (e.g. "Larqw;"), since the raw dotted name would be converted incorrectly.
    val enumType = playlistAutoplayEnumClass(this).descriptor
    Fingerprint(
        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
        parameters = listOf(enumType, "L", "L"),
    ).run().declaredClass ?: error("Navigation intent wrapper class not found")
}

/**
 * The wrapper field holding the navigation intent enum instance.
 */
val playlistAutoplayNavigationIntentField = findFieldDirect {
    val enumClass = playlistAutoplayEnumClass(this)
    playlistAutoplayWrapperClass(this).fields.first {
        it.type.descriptor == enumClass.descriptor
    }
}

/**
 * Methods that receive the navigation intent wrapper and advance the playback queue.
 * Hooked at their start to skip playlist autoplay when the extension setting is enabled.
 */
val playlistAutoplayNavigationMethods = findMethodListDirect {
    val wrapper = playlistAutoplayWrapperClass(this)
    this.findMethod {
        matcher {
            returnType = "void"
            paramTypes(listOf(wrapper.name))
        }
    }
}