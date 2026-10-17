/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2214
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package io.github.nexalloy.morphe.youtube.misc.chapters

import app.morphe.extension.youtube.patches.ChaptersHookPatch
import app.morphe.extension.youtube.patches.ChaptersHookPatch.TimelineMarker
import io.github.nexalloy.morphe.youtube.video.videoid.VideoId
import io.github.nexalloy.morphe.youtube.video.videoid.videoIdHooks
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

private data class TimelineMarkerAdapter(
    val startMillis: Long,
    val endMillis: Long,
    val title: CharSequence?
) : TimelineMarker {
    override fun patch_getStartMillis() = startMillis
    override fun patch_getEndMillis() = endMillis
    override fun patch_getTitle() = title
}

val chaptersHookPatch = patch {
    dependsOn(VideoId)

    videoIdHooks.add(ChaptersHookPatch::newVideoLoaded)

    val startMillis = ::timelineMarkerStartMillisField.field
    val endMillis = ::timelineMarkerEndMillisField.field
    val title = ::timelineMarkerTitleField.field
    ::timelineMarkersArrayMethod.hookMethod(scopedHook(::timelineMarkersToArrayMethod.method) {
        after {
            val markers = it.result as? Array<*> ?: return@after
            ChaptersHookPatch.setTimelineMarkers(markers.map { marker ->
                TimelineMarkerAdapter(
                    startMillis.getLong(marker), endMillis.getLong(marker), title.get(marker) as? CharSequence
                )
            }.toTypedArray())
        }
    })

    HeatMapPeakPointFingerprint.hookMethod(scopedHook(::heatMapPeakPointIsPresentMethod.method) {
        after {
            if (it.thisObject === outerParam.args[0]) {
                ChaptersHookPatch.setHeatMapPeakPoint(it.result as Boolean)
            }
        }
    })
}
