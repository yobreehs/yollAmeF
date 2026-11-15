package io.github.nexalloy

import io.github.nexalloy.hoodles.morphe.alltrails.AllTrailsPatches
import io.github.nexalloy.morphe.music.YTMusicPatches
import io.github.nexalloy.morphe.reddit.RedditPatches
import io.github.nexalloy.morphe.youtube.YouTubePatches
import io.github.nexalloy.revanced.googlephotos.GooglePhotosPatches
import io.github.nexalloy.revanced.meta.MetaPatches
import io.github.nexalloy.revanced.photomath.PhotomathPatches
import io.github.nexalloy.revanced.strava.StravaPatches

/**
 * A target app and the patches it supports.
 *
 * The patch array is created lazily, on the first access. Building every array eagerly (the old
 * behavior) initialized all top-level `patch {}` objects in every process the module is loaded
 * into. Each of those objects queues its settings registration, so a process such as Reddit
 * applied the settings lambdas of YouTube too. The YouTube-only version flags
 * (`is_20_31_or_greater` and the like) are `Delegates.notNull()` and threw there, which showed up
 * as `Settings registration failed for patch 'Navigation bar'` in the logs of apps that have
 * nothing to do with the YouTube navigation bar. It also meant every target app loaded the code of
 * every other target app.
 */
class AppPatchInfo(
    val appName: String,
    val packageName: String,
    private val patchFactory: () -> Array<Patch>,
) {
    val patches: Array<Patch> by lazy(LazyThreadSafetyMode.SYNCHRONIZED, patchFactory)
}

val appPatchConfigurations = listOf(
    AppPatchInfo("YouTube", "com.google.android.youtube") { YouTubePatches },
    AppPatchInfo("YT Music", "com.google.android.apps.youtube.music") { YTMusicPatches },
    AppPatchInfo("Reddit", "com.reddit.frontpage") { RedditPatches },
    AppPatchInfo("Google Photos", "com.google.android.apps.photos") { GooglePhotosPatches },
    AppPatchInfo("Photomath", "com.microblink.photomath") { PhotomathPatches },
    AppPatchInfo("Instagram", "com.instagram.android") { MetaPatches },
    AppPatchInfo("Threads", "com.instagram.barcelona") { MetaPatches },
    AppPatchInfo("Strava", "com.strava") { StravaPatches },
    AppPatchInfo("AllTrails", "com.alltrails.alltrails") { AllTrailsPatches },
)

val patchesByPackage = appPatchConfigurations.associateBy { it.packageName }
