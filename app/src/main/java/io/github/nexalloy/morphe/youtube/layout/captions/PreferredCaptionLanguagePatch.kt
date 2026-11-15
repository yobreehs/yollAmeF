package io.github.nexalloy.morphe.youtube.layout.captions

import android.content.Context
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SearchableListPreference
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.video.information.VideoInformationPatch
import io.github.nexalloy.morphe.youtube.video.information.onCreateHook
import io.github.nexalloy.hookMethod
import io.github.nexalloy.patch
import java.lang.reflect.Field
import java.util.Locale

private const val SETTINGS_PREFERENCES_NAME = "morphe_prefs"
private const val PREFERRED_LANGUAGE_KEY = "morphe_preferred_caption_language"

private const val DISABLE_OPTION = "DISABLE_CAPTIONS_OPTION"
private const val AUTO_TRANSLATE_OPTION = "AUTO_TRANSLATE_CAPTIONS_OPTION"
private const val PREFERRED_TRACK_SELECT_TYPE = "PREFERRED_TRACK"

/** Time after a new video starts where explicit track selections are not user selections. */
private const val USER_SELECTION_GRACE_PERIOD_MILLISECONDS = 500L

private fun readPreferredLanguage(): String = runCatching {
    Utils.getContext()
        .getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getString(PREFERRED_LANGUAGE_KEY, "") ?: ""
}.getOrDefault("")

private fun savePreferredLanguage(languageCode: String) {
    runCatching {
        Utils.getContext()
            .getSharedPreferences(SETTINGS_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFERRED_LANGUAGE_KEY, languageCode)
            .apply()
    }
}

private fun normalizeLanguageCode(code: String?): String {
    if (code == null) return ""
    val s = code.trim().lowercase(Locale.ROOT).replace('_', '-')
    return when {
        s == "iw" || s.startsWith("iw-") -> "he" + s.substring(2)
        s == "in" || s.startsWith("in-") -> "id" + s.substring(2)
        s == "ji" || s.startsWith("ji-") -> "yi" + s.substring(2)
        else -> s
    }
}

private fun isTraditionalChinese(code: String) =
    code.contains("hant") || code.contains("tw") || code.contains("hk")

private fun isSimplifiedChinese(code: String) =
    code.contains("hans") || code.contains("cn")

private fun primaryLanguage(code: String): String {
    val dash = code.indexOf('-')
    return if (dash < 0) code else code.substring(0, dash)
}

private fun matchesLanguage(trackLanguage: String?, targetLanguage: String): Boolean {
    if (trackLanguage == null) return false
    val t1 = normalizeLanguageCode(trackLanguage)
    val t2 = normalizeLanguageCode(targetLanguage)
    if (t1.isEmpty() || t2.isEmpty()) return false
    if (t1 == t2 || t1.startsWith("$t2-") || t2.startsWith("$t1-")) return true

    // Prevent cross-matching between Simplified and Traditional Chinese.
    if ((isSimplifiedChinese(t1) && isTraditionalChinese(t2)) ||
        (isTraditionalChinese(t1) && isSimplifiedChinese(t2))
    ) return false

    return primaryLanguage(t1) == primaryLanguage(t2)
}

private fun appLanguage(): String = runCatching {
    val locale = Utils.getContext().resources.configuration.locales[0]
    val tag = locale.toLanguageTag()
    if (tag.isNotEmpty() && !tag.equals("und", true)) tag else locale.language
}.getOrDefault("en")

private fun readTrackString(track: Any?, field: Field): String? =
    runCatching { field.get(track) as? String }.getOrNull()

private fun isExplicitlyDisabled(track: Any?, languageField: Field, vssField: Field): Boolean {
    if (track == null) return false
    val language = readTrackString(track, languageField) ?: return false
    val vss = readTrackString(track, vssField).orEmpty()
    return language.isEmpty() || language == DISABLE_OPTION || vss == "-"
}

/**
 * Adds an option to automatically select captions in the preferred language: provider subtitles
 * first, then auto-translated captions.
 *
 * A language selected in the player's own captions menu is remembered as the preferred one, so it
 * does not have to be changed in the Morphe settings.
 */
val PreferredCaptionLanguage = patch(
    name = "Preferred caption language",
    description = "Automatically selects captions in the preferred language " +
        "(provider subtitles first, then auto-translated). A language picked in the player menu is remembered.",
    settings = {
        PreferenceScreen.PLAYER.addPreferences(
            ListPreference(
                key = PREFERRED_LANGUAGE_KEY,
                summaryKey = "${PREFERRED_LANGUAGE_KEY}_summary",
                tag = SearchableListPreference::class.java,
            )
        )
    },
) {
    dependsOn(VersionCheck, VideoInformationPatch)

    val languageField = ::captionTrackLanguageField.field
    val vssField = ::captionTrackVssIdField.field
    val tracksManagerField = ::captionTracksManagerField.field
    val directTracksMethod = ::captionTracksManagerDirectMethod.method
    val autoTranslateTracksMethod = ::captionTracksManagerAutoTranslateMethod.method
    val trackClass = ::captionTrackClass.clazz

    // The obfuscated fields/methods are not public, so make them callable from the module.
    listOf(languageField, vssField, tracksManagerField).forEach { field ->
        runCatching { field.isAccessible = true }
    }
    listOf(directTracksMethod, autoTranslateTracksMethod).forEach { method ->
        runCatching { method.isAccessible = true }
    }

    Logger.printDebug {
        "PreferredCaptionLanguage: installed, trackClass=${trackClass.name}, " +
            "languageField=${languageField.name}, vssField=${vssField.name}, " +
            "tracksManagerField=${tracksManagerField.name}, " +
            "directTracks=${directTracksMethod.name}, autoTracks=${autoTranslateTracksMethod.name}"
    }

    var videoStartTime: Long = System.currentTimeMillis()

    var userSelectedTrack: Boolean = false

    var lastProgrammaticLanguage: String? = null

    var lastProgrammaticVssId: String? = null

    /** Only used to rate limit the diagnostics, so logcat does not flood. */
    var debugCallCount: Int = 0

    fun recordProgrammaticSelection(track: Any?): Any? {
        lastProgrammaticLanguage = track?.let { readTrackString(it, languageField) }
        lastProgrammaticVssId = track?.let { readTrackString(it, vssField) }
        return track
    }

    fun isSameAsProgrammatic(track: Any?): Boolean {
        if (track == null) return false
        val language = readTrackString(track, languageField)
        val vss = readTrackString(track, vssField)
        val targetVss = lastProgrammaticVssId
        val targetLanguage = lastProgrammaticLanguage
        val hasTargetVss = !targetVss.isNullOrEmpty()

        if (hasTargetVss && targetVss == vss) return true

        return !targetLanguage.isNullOrEmpty() &&
            targetLanguage.equals(language, ignoreCase = true) &&
            (!hasTargetVss || vss.isNullOrEmpty())
    }

    /** @return the preferred track for the current video, or null to keep the original one. */
    fun findPreferredTrack(manager: Any, debug: Boolean): Any? {
        val preferred = readPreferredLanguage()

        fun log(message: String) {
            if (debug && debugCallCount < 10) {
                debugCallCount++
                Logger.printDebug { "PreferredCaptionLanguage: $message" }
            }
        }

        if (preferred.isEmpty()) {
            log("no preferred language set, keeping the original track")
            return null
        }

        if (preferred.equals("off", ignoreCase = true)) {
            log("preferred language is off, keeping the original track")
            return null
        }

        val target = normalizeLanguageCode(
            if (preferred.equals("default", ignoreCase = true)) appLanguage() else preferred
        )
        if (target.isEmpty()) {
            log("preferred language '$preferred' normalized to an empty code, keeping the original track")
            return null
        }

        val tracksManager = runCatching { tracksManagerField.get(manager) }.getOrNull()
        if (tracksManager == null) {
            log("caption tracks manager field is null, keeping the original track")
            return null
        }

        val directTracks = runCatching { directTracksMethod.invoke(tracksManager) as? List<*> }
            .getOrNull()
        if (directTracks == null) {
            log("direct caption tracks are null, keeping the original track")
            return null
        }

        val directSummary = directTracks.joinToString { track ->
            val language = readTrackString(track, languageField).orEmpty()
            val vss = readTrackString(track, vssField).orEmpty()
            "$language/$vss"
        }
        log("preferred=$preferred target=$target direct tracks=$directSummary")

        var nativeTrack: Any? = null
        var autoTranslateAvailable = false

        for (track in directTracks) {
            if (track == null) continue
            val language = readTrackString(track, languageField) ?: continue
            if (language == AUTO_TRANSLATE_OPTION) {
                autoTranslateAvailable = true
                continue
            }
            if (language == DISABLE_OPTION || !matchesLanguage(language, target)) continue

            if (readTrackString(track, vssField).orEmpty().startsWith(".")) {
                // Creator / provider subtitle in the target language has the highest priority.
                log("selected provider subtitle ${readTrackString(track, vssField)}")
                return track
            }
            if (nativeTrack == null) nativeTrack = track
        }

        if (nativeTrack != null) {
            log("selected native track ${readTrackString(nativeTrack, vssField)}")
            return nativeTrack
        }

        if (autoTranslateAvailable) {
            val autoTracks = runCatching { autoTranslateTracksMethod.invoke(tracksManager) as? List<*> }
                .getOrNull()
            if (autoTracks != null) {
                log(
                    "no direct match, auto translated tracks=" +
                        autoTracks.joinToString { track ->
                            readTrackString(track, languageField).orEmpty()
                        }
                )
                for (track in autoTracks) {
                    if (track == null) continue
                    if (matchesLanguage(readTrackString(track, languageField), target)) {
                        log("selected auto translated track ${readTrackString(track, languageField)}")
                        return track
                    }
                }
            }
        } else {
            log("no direct match and auto translation is not available")
        }

        log("no preferred track found, keeping the original track")
        return null
    }

    // Reset the per-video state.
    onCreateHook.add {
        userSelectedTrack = false
        recordProgrammaticSelection(null)
        videoStartTime = System.currentTimeMillis()
        debugCallCount = 0
        Logger.printDebug { "PreferredCaptionLanguage: new video started" }
    }

    // Replace the track that YouTube is about to select.
    //
    // This is the path that actually changes the captions. The default track getter is not hooked:
    // its call sites are ambiguous (the same call appears in more than one method), and the setter
    // is the authoritative place where the selected track is decided.
    ::setSubtitleTrackMethod.memberOrNull?.hookMethod {
        before {
            try {
                val trackIndex = it.args.indexOfFirst { arg -> arg != null && trackClass.isInstance(arg) }
                if (trackIndex < 0) {
                    Logger.printDebug {
                        "PreferredCaptionLanguage: setSubtitleTrack without a caption track, " +
                            "args=${it.args.joinToString { arg -> arg?.javaClass?.simpleName ?: "null" }}"
                    }
                    return@before
                }

                val originalTrack = it.args[trackIndex]
                val selectType = it.args.firstOrNull { arg -> arg is Enum<*> } as? Enum<*>
                val selectReason = it.args.filterIsInstance<Int>().firstOrNull()
                val language = readTrackString(originalTrack, languageField)
                val vssId = readTrackString(originalTrack, vssField)
                val preferred = readPreferredLanguage()
                val featureEnabled = preferred.isNotEmpty() && !preferred.equals("off", ignoreCase = true)
                val elapsed = System.currentTimeMillis() - videoStartTime

                // The select type enum and its constants are obfuscated (a/b/c on 21.42), so its
                // name cannot be used. Any real track selection after the start grace period that
                // is not our own programmatic choice is treated as a selection in the player menu.
                // Selection reason 4 is the only path that comes from the player UI
                // (Lasim.aI -> SubtitleManager.m(track, selectType, 4)); internal restores use 0/1/3/5.
                val isUserSelection = featureEnabled &&
                    selectReason == 4 &&
                    elapsed > USER_SELECTION_GRACE_PERIOD_MILLISECONDS &&
                    !isSameAsProgrammatic(originalTrack)

                Logger.printDebug {
                    "PreferredCaptionLanguage: setSubtitleTrack language=$language vssId=$vssId " +
                        "selectType=${selectType?.javaClass?.simpleName}#${selectType?.ordinal} " +
                        "reason=$selectReason elapsed=$elapsed preferred=$preferred " +
                        "userSelected=$userSelectedTrack userSelection=$isUserSelection"
                }

                if (isUserSelection) {
                    // The user picked a track in the player menu: remember it for the next videos.
                    userSelectedTrack = true
                    if (!language.isNullOrEmpty() &&
                        language != DISABLE_OPTION &&
                        language != AUTO_TRANSLATE_OPTION &&
                        !language.equals(preferred, ignoreCase = true)
                    ) {
                        savePreferredLanguage(language)
                        Logger.printDebug {
                            "PreferredCaptionLanguage: remembered player selection: $language"
                        }
                    }
                    return@before
                }

                if (userSelectedTrack) return@before
                if (isExplicitlyDisabled(originalTrack, languageField, vssField)) return@before

                val preferredTrack = runCatching { findPreferredTrack(it.thisObject, true) }
                    .getOrNull() ?: return@before

                it.args[trackIndex] = preferredTrack
                recordProgrammaticSelection(preferredTrack)
                Logger.printDebug {
                    "PreferredCaptionLanguage: replaced track with " +
                        "${readTrackString(preferredTrack, languageField)}"
                }
            } catch (ex: Throwable) {
                Logger.printException({ "PreferredCaptionLanguage: setSubtitleTrack hook failed" }, ex)
            }
        }
    }
}
