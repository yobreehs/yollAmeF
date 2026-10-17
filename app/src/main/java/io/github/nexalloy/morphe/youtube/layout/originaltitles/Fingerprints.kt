package io.github.nexalloy.morphe.youtube.layout.originaltitles

import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.checkCast
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findFieldListDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.string

/**
 * Binds a video of the playlist panel (the queue of a playlist or mix on the watch page).
 * The panel has multiple layouts, each with its own bind method.
 */
@SkipTest
internal object PlaylistPanelVideoBindFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    filters = listOf(
        // Title view.
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/widget/TextView;"),
        string("PLAYLIST_CURRENT_VIDEO_MONITOR"),
        // Video id.
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/String;",
            location = MatchAfterWithin(10)
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Ljava/lang/String;",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * Binds the video information shown on the watch page while the video is loading,
 * which shows the title of the element that opened the video.
 */
@StrictQuery
internal object LoadingVideoInformationBindFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "channel_navigation_container"),
        methodCall(name = "findViewById", location = MatchAfterImmediately()),
        resourceLiteral(ResourceType.ID, "title", location = MatchAfterWithin(8)),
        methodCall(name = "findViewById", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/TextView;", location = MatchAfterWithin(2)),
        resourceLiteral(ResourceType.ID, "collapsed_subtitle", location = MatchAfterWithin(8))
    )
)

/**
 * Creates the fullscreen engagement overlay, shown by swiping up in fullscreen.
 */
@StrictQuery
internal object FullscreenEngagementOverlayFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "fullscreen_engagement_overlay"),
        resourceLiteral(ResourceType.ID, "engagement_title"),
        methodCall(name = "findViewById", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/TextView;", location = MatchAfterWithin(2)),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Landroid/widget/TextView;",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * Creates the title view of the next video of a playlist, shown when the playlist panel is collapsed.
 */
@SkipTest
internal object NextVideoTitleViewFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.ID, "next_video_title"),
        methodCall(name = "findViewById", location = MatchAfterImmediately()),
        checkCast("Landroid/widget/TextView;", location = MatchAfterWithin(2)),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            type = "Landroid/widget/TextView;",
            location = MatchAfterImmediately()
        )
    )
)

val playlistPanelVideoBindMethods = findMethodListDirect {
    PlaylistPanelVideoBindFingerprint.matchAll().map { it.method }
}

val playlistPanelTitleFields = findFieldListDirect {
    PlaylistPanelVideoBindFingerprint.matchAll().map { it.instructionMatches.first().instruction.fieldRef!! }
        .distinctBy { it.descriptor }
}

val playlistPanelVideoIdFields = findFieldListDirect {
    PlaylistPanelVideoBindFingerprint.matchAll().map { it.instructionMatches.last().instruction.fieldRef!! }
        .distinctBy { it.descriptor }
}

val nextVideoTitleViewMethods = findMethodListDirect {
    NextVideoTitleViewFingerprint.matchAll().map { it.method }
}
