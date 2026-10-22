/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2100
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.misc.chapters

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterLast
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findFieldFromToString
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral

internal object TimelineMarkerFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("TimelineMarker[title=",  ", startMillis=", ", endMillis=")
)

internal fun getTimelineMarkersArrayFingerprint(timelineMarkerClassName: String) = object : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "[$timelineMarkerClassName",
    parameters = listOf("L"),
    filters = listOf(
        checkCast("[$timelineMarkerClassName"),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterLast())
    )
) {}

@StrictQuery
internal object HeatMapPeakPointFingerprint : Fingerprint (
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Lj$/util/Optional;->isPresent()Z"
        ),
        opcode(
            opcode = Opcode.MOVE_RESULT,
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Lj$/util/Optional;",
            location = MatchAfterWithin(2)
        ),
        resourceLiteral(
            ResourceType.DIMEN,
            "ic_marker_decoration_size",
            location = MatchAfterWithin(37)
        )
    )
)

val timelineMarkersArrayMethod = findMethodDirect {
    getTimelineMarkersArrayFingerprint(TimelineMarkerFingerprint().declaredClass!!.descriptor)()
}

val timelineMarkersToArrayMethod = findMethodDirect {
    timelineMarkersArrayMethod().instructions.last { it.methodRef?.name == "toArray" }.methodRef!!
}

val timelineMarkerStartMillisField = findFieldDirect {
    TimelineMarkerFingerprint().findFieldFromToString("startMillis=")
}

val timelineMarkerEndMillisField = findFieldDirect {
    TimelineMarkerFingerprint().findFieldFromToString("endMillis=")
}

val timelineMarkerTitleField = findFieldDirect {
    TimelineMarkerFingerprint().findFieldFromToString("title=")
}

val heatMapPeakPointIsPresentMethod = findMethodDirect {
    HeatMapPeakPointFingerprint.instructionMatches.first().instruction.methodRef!!
}
