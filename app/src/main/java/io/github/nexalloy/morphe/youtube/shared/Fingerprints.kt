package io.github.nexalloy.morphe.youtube.shared

import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.InstructionLocation.MatchFirst
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.StringComparisonType
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.string

internal const val CLIENT_INFO_CLASS =
    $$"Lcom/google/protos/youtube/api/innertube/InnertubeContext$ClientInfo;"

internal const val YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE =
    "Lcom/google/android/apps/youtube/app/watchwhile/MainActivity;"

internal object VideoStreamingDataToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    filters = listOf(string("VideoStreamingData(itags="))
)


internal object BuildClientContextBodyConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    filters = listOf(
        string("Android Wear"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Z"
        ),
        opcode(Opcode.IF_EQZ),
        string("Android Automotive", location = MatchAfterImmediately()),
        string("Android"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, location = MatchAfterImmediately())
    )
)

internal object EngagementPanelControllerFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf("L", "L", "Z", "Z"),
    filters = listOf(
        string("EngagementPanelController: cannot show EngagementPanel before EngagementPanelController.init() has been called."),
        methodCall(smali = "Lj$/util/Optional;->orElse(Ljava/lang/Object;)Ljava/lang/Object;"),
        methodCall(smali = "Lj$/util/Optional;->orElse(Ljava/lang/Object;)Ljava/lang/Object;"),
        opcode(opcode = Opcode.CHECK_CAST, location = MatchAfterWithin(4)),
        opcode(opcode = Opcode.IF_EQZ, location = MatchAfterImmediately()),
        opcode(opcode = Opcode.IGET_OBJECT, location = MatchAfterImmediately()),
        literal(45615449L),
        methodCall(smali = "Ljava/util/ArrayDeque;->iterator()Ljava/util/Iterator;"),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/String;",
            location = MatchAfterWithin(10)
        )
    )
)


internal object YouTubeMainActivityConstructorFingerprint : Fingerprint(
    definingClass = YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf()
)

val mainActivityClass = findClassDirect {
    YouTubeMainActivityConstructorFingerprint().declaredClass!!
}

internal object YouTubeActivityOnCreateFingerprint : Fingerprint(
    definingClass = YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE,
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

internal object SeekbarFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("timed_markers_width"),
    )
)

internal object PlaybackSpeedOnItemClickParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("L", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(name = "getSupportFragmentManager", location = MatchFirst()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            returnType = "L",
            parameters = listOf("Ljava/lang/String;"),
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, location = MatchAfterImmediately()),
    ),
    custom = {
        declaredClass {
            methodCount(8)
        }
    }
)

internal object SpeedLimiterParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        string("Playback rate: %f"),
        literal(0.25f),
        literal(4.0f),
    )
)

internal object SpeedLimiterFingerprint : Fingerprint(
    classFingerprint = SpeedLimiterParentFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        literal(0.25f),
        literal(4.0f),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            parameters = listOf("F"),
            returnType = "V"
        )
    ),
//    custom = { method, _ ->
//        method.parameterTypes.firstOrNull() == "F"
//    }
)

@StrictQuery
internal object VideoQualityChangedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET,
            type = "I",
            location = MatchFirst()
        ),
        literal(2, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NE, location = MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, location = MatchAfterImmediately()), // Obfuscated VideoQuality

        opcode(Opcode.IGET_OBJECT, location = MatchAfterWithin(6)),
        opcode(Opcode.CHECK_CAST),
        fieldAccess( // Video resolution (human-readable).
            opcode = Opcode.IGET,
            type = "I",
            location = MatchAfterImmediately()
        ),
        methodCall( // VideoQuality constructor.
            name = "<init>",
            opcodes = listOf(Opcode.INVOKE_DIRECT, Opcode.INVOKE_DIRECT_RANGE)
        )
    )
)

val VideoQualityClass = findClassDirect {
    VideoQualityChangedFingerprint.instructionMatches.last().instruction.methodRef!!.declaredClass!!
}

val VideoQualityReceiver = findMethodDirect {
    val videoQualityClassName = VideoQualityClass().name
    VideoQualityChangedFingerprint().invokes.single { it.paramCount == 1 && it.paramTypeNames[0] == videoQualityClassName }
}

internal object WatchNextResponseParserFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    returnType = "Ljava/util/List;",
    filters = listOf(
        literal(49399797L),
        opcode(Opcode.SGET_OBJECT),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            location = MatchAfterImmediately()
        ),
        literal(51779735L),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/Object;",
            location = MatchAfterWithin(5)
        ),
        opcode(
            Opcode.CHECK_CAST,
            location = MatchAfterImmediately()
        ),
        literal(46659098L),
    )
)

internal object StartVideoInformerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID,
    ),
    strings = listOf("pc"),
)

internal object InitializePlaybackSpeedValuesFingerprint : Fingerprint(
    parameters = listOf("[L", "I"),
    filters = listOf(
        string("menu_item_playback_speed"),
    )
)

internal object VideoPlaybackIntentFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "Ljava/util/Map;"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = listOf(), returnType = "L"),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        string("PLAYBACK_START_DESCRIPTOR_MUTATOR", MatchAfterImmediately()),
        string("force_fullscreen"),
        string("VideoPresenterConstants.VIDEO_THUMBNAIL_BITMAP_KEY")
    )
)

@RequireAppVersion(minVersion = "21.20.000")
internal object ShortsPlaybackIntentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "L",
        "Ljava/util/Map;",
        "J",
        "Ljava/lang/String;"
    ),
    filters = listOf(
        // None of these strings are unique.
        string("PLAYBACK_START_DESCRIPTOR_MUTATOR"),
        string("com.google.android.apps.youtube.app.endpoint.flags"),
        string("com.google.android.libraries.youtube.rendering.elements.sender_view")
    )
)

@RequireAppVersion(maxVersion = "21.20.000")
internal object ShortsPlaybackIntentFingerprintLegacy : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "L",
        "Ljava/util/Map;",
        "J",
        "Ljava/lang/String;"
    ),
    filters = listOf(
        // None of these strings are unique.
        string("com.google.android.apps.youtube.app.endpoint.flags"),
        string("ReelWatchFragmentArgs"),
        string("reels_fragment_descriptor")
    )
)

internal object PlaybackStartDescriptorToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        methodCall(smali = "Ljava/util/Locale;->getDefault()Ljava/util/Locale;"),
        // First method call after Locale is the video ID.
        methodCall(returnType = "Ljava/lang/String;", parameters = listOf()),
        string("PlaybackStartDescriptor:", comparison = StringComparisonType.STARTS_WITH)
    )
)

val playbackStartVideoIdMethod = findMethodDirect {
    PlaybackStartDescriptorToStringFingerprint.instructionMatches[1].instruction.methodRef!!
}

val regularVideoPlaybackStartDescriptorGetter = findMethodDirect {
    VideoPlaybackIntentFingerprint.instructionMatches.first().instruction.methodRef!!
}
