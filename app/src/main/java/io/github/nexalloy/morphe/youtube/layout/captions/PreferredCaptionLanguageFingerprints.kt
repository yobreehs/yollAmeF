package io.github.nexalloy.morphe.youtube.layout.captions

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.RestrictQuery
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.anyInstruction
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.newInstance
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

/*
 * Preferred caption language (ported from the upstream PreferredCaptionLanguagePatch).
 *
 * The upstream patcher injects helper methods and interfaces into obfuscated classes; this
 * Xposed port resolves the same anchors with DexKit and calls them through reflection.
 */

private object SubtitleManagerClassFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf("L", "Landroid/view/accessibility/CaptioningManager;"),
    filters = listOf(
        fieldAccess(smali = "Ljava/util/concurrent/TimeUnit;->SECONDS:Ljava/util/concurrent/TimeUnit;"),
        methodCall(smali = "Landroid/view/accessibility/CaptioningManager;->isEnabled()Z"),
    ),
    custom = { modifiers(AccessFlags.STATIC.modifier) }
)

/**
 * String field of the enclosing class, or (older app targets) a call to an abstract String getter.
 */
private fun stringFieldOrGetter(
    location: InstructionLocation = InstructionLocation.MatchAfterAnywhere()
) = anyInstruction(
    fieldAccess(
        opcode = Opcode.IGET_OBJECT,
        definingClass = "this",
        type = "Ljava/lang/String;"
    ),
    methodCall(
        definingClass = "this",
        parameters = listOf(),
        returnType = "Ljava/lang/String;"
    ),
    location = location
)

/**
 * Resolves to the caption track class.
 */
internal object CaptionTrackIsDisableOptionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        string("DISABLE_CAPTIONS_OPTION"),
        // Language code.
        stringFieldOrGetter(location = MatchAfterImmediately()),
        methodCall(
            smali = "Ljava/lang/String;->equals(Ljava/lang/Object;)Z",
            location = MatchAfterWithin(1)
        )
    )
)

internal object CaptionTrackIsAutoTranslatedFingerprint : Fingerprint(
    classFingerprint = CaptionTrackIsDisableOptionFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        // Vss id.
        stringFieldOrGetter(),
        string("t", location = MatchAfterWithin(1)),
        methodCall(
            smali = "Ljava/lang/String;->startsWith(Ljava/lang/String;)Z",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * Resolves to the caption tracks manager class.
 */
internal object CaptionTracksManagerDirectTracksFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(
        string("AUTO_TRANSLATE_CAPTIONS_OPTION")
    )
)

internal object CaptionTracksManagerAutoTranslateTracksFingerprint : Fingerprint(
    classFingerprint = CaptionTracksManagerDirectTracksFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    parameters = listOf(),
    filters = listOf(
        string("&tlang=")
    )
)

internal object DefaultCaptionTrackFingerprint : Fingerprint(
    classFingerprint = SubtitleManagerClassFingerprint,
    returnType = "L",
    parameters = listOf(),
    filters = listOf(
        methodCall(smali = "Landroid/view/accessibility/CaptioningManager;->isEnabled()Z"),
    )
)

@RestrictQuery
internal object SetSubtitleTrackFingerprint : Fingerprint(
    classFingerprint = SubtitleManagerClassFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        // 21.40+ removed the "setSubtitleTrack name:%s languageCode:%s ..." log string,
        // but the Throwable used for the log statement remains.
        newInstance("Ljava/lang/Throwable;")
    )
)

/** Class of the caption track objects (obfuscated). */
internal val captionTrackClass = findClassDirect {
    CaptionTrackIsDisableOptionFingerprint.run().declaredClass!!
}

internal val defaultCaptionTrackMethod = findMethodDirect {
    DefaultCaptionTrackFingerprint.run()
}

internal val setSubtitleTrackMethod = findMethodDirect {
    SetSubtitleTrackFingerprint.run()
}

/** Language code of a caption track (e.g. "ko", "en"), or a pseudo option value. */
internal val captionTrackLanguageField = findFieldDirect {
    CaptionTrackIsDisableOptionFingerprint.instructionMatches[1].instruction.fieldRef!!
}

/** Caption track vss id (e.g. ".ko", "a.en", "t.ko"). */
internal val captionTrackVssIdField = findFieldDirect {
    CaptionTrackIsAutoTranslatedFingerprint.instructionMatches[0].instruction.fieldRef!!
}

internal val captionTracksManagerDirectMethod = findMethodDirect {
    CaptionTracksManagerDirectTracksFingerprint.run()
}

internal val captionTracksManagerAutoTranslateMethod = findMethodDirect {
    CaptionTracksManagerAutoTranslateTracksFingerprint.run()
}

/** Field of the subtitle manager that holds the caption tracks manager. */
internal val captionTracksManagerField = findFieldDirect {
    val managerClass = SubtitleManagerClassFingerprint.run().declaredClass!!
    val tracksManagerType = CaptionTracksManagerDirectTracksFingerprint.run()
        .declaredClass!!.descriptor
    managerClass.fields.single { it.type.descriptor == tracksManagerType }
}
