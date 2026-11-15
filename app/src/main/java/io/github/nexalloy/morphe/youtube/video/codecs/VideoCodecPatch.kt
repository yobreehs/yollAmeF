package io.github.nexalloy.morphe.youtube.video.codecs

import android.content.Context
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.youtube.interaction.reload.dismissPlayerMethod
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.youtube.shared.VideoStreamingDataConstructorFingerprint
import io.github.nexalloy.patch
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.util.WeakHashMap

private const val CODEC_PREFERENCE_KEY = "morphe_video_codec"
internal const val CODEC_PREFERENCES_NAME = "morphe_prefs"
internal const val CODEC_AUTO = "DEFAULT"
internal const val CODEC_AV1 = "AV1"
internal const val CODEC_VP9 = "VP9"
internal const val CODEC_AVC = "AVC"

internal fun selectedVideoCodec(): String = runCatching {
    Utils.getContext()
        .getSharedPreferences(CODEC_PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getString(CODEC_PREFERENCE_KEY, CODEC_AUTO)
        ?: CODEC_AUTO
}.getOrDefault(CODEC_AUTO)

internal fun setSelectedVideoCodec(codec: String) {
    runCatching {
        Utils.getContext()
            .getSharedPreferences(CODEC_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(CODEC_PREFERENCE_KEY, codec)
            .apply()
    }.onFailure {
        Logger.printException({ "VideoCodec: failed to save codec=$codec" }, it)
    }
}

/**
 * Adds a video codec preference and removes the adaptive formats that use other codecs.
 *
 * The player then has no choice but to use the selected codec, exactly like forcing
 * the codec in SmartTube. If the selected codec is not available for the video,
 * the original format list is kept so playback still works.
 */
val VideoCodec = patch(
    name = "Video codec",
    description = "Adds an option to prefer a video codec (AV1, VP9 or H.264/AVC) and switch it during playback.",
    settings = {
        PreferenceScreen.VIDEO.addPreferences(
            ListPreference(
                key = CODEC_PREFERENCE_KEY,
                entriesKey = "morphe_video_codec_entries",
                entryValuesKey = "morphe_video_codec_entry_values",
                summaryKey = "morphe_video_codec_summary"
            )
        )
    },
) {
    VideoStreamingDataConstructorFingerprint.hookMethod {
        after { param ->
            try {
                VideoCodecFormatFilter.filter(param.thisObject)
            } catch (ex: Throwable) {
                Logger.printException({ "VideoCodec: failed to filter video formats" }, ex)
            }
        }
    }

    // Resolve and hook the obfuscated player dismiss method used for the on-the-fly reload.
    val dismissMethod = runCatching { ::dismissPlayerMethod.method }.getOrNull()
    VideoCodecReloader.initialize(dismissMethod)
    dismissMethod?.declaringClass?.declaredConstructors?.forEach { constructor ->
        runCatching {
            constructor.hookMethod {
                after { param ->
                    VideoCodecReloader.capturePlayerInstance(param.thisObject)
                }
            }
        }.onFailure {
            Logger.printDebug { "VideoCodec: could not hook player constructor: $it" }
        }
    }
}

internal object VideoCodecFormatFilter {

    /**
     * Unfiltered format lists of every seen VideoStreamingData instance. Keeping them
     * allows switching the codec back and forth without losing the other formats.
     */
    private val originalLists = WeakHashMap<Any, Map<Field, List<*>>>()

    private var latestVideoStreamingData: WeakReference<Any>? = null
    private var preferenceListenerRegistered = false

    fun filter(videoStreamingData: Any) {
        ensurePreferenceListener()
        latestVideoStreamingData = WeakReference(videoStreamingData)

        val originals = originalLists.getOrPut(videoStreamingData) {
            captureLists(videoStreamingData)
        }
        apply(videoStreamingData, originals, selectedVideoCodec())
    }

    /**
     * Re-applies the current preference to the last parsed streaming data. Used when
     * the user changes the codec while the player is already open.
     */
    fun reapply() {
        val instance = latestVideoStreamingData?.get() ?: return
        val originals = originalLists[instance] ?: return
        apply(instance, originals, selectedVideoCodec())
    }

    private fun captureLists(videoStreamingData: Any): Map<Field, List<*>> {
        val lists = mutableMapOf<Field, List<*>>()

        for (field in videoStreamingData.javaClass.declaredFields) {
            if (!List::class.java.isAssignableFrom(field.type)) continue

            field.isAccessible = true
            val formats = field.get(videoStreamingData) as? List<*> ?: continue
            if (formats.isEmpty()) continue

            lists[field] = ArrayList(formats)
        }

        return lists
    }

    private fun apply(
        videoStreamingData: Any,
        originals: Map<Field, List<*>>,
        selectedCodec: String,
    ) {
        val codecMarker = codecMarker(selectedCodec)

        var changedLists = 0
        var removedFormats = 0

        for ((field, originalFormats) in originals) {
            val targetFormats = if (codecMarker == null) {
                originalFormats
            } else {
                filteredFormats(originalFormats, codecMarker)
            }

            if (field.get(videoStreamingData) === targetFormats) continue

            field.set(videoStreamingData, targetFormats)
            changedLists++
            removedFormats += originalFormats.size - targetFormats.size

            Logger.printDebug {
                "VideoCodec: field=${field.name}, codec=$selectedCodec, " +
                    "formats=${originalFormats.size}->${targetFormats.size}"
            }
        }

        if (changedLists > 0) {
            Logger.printDebug {
                "VideoCodec: selected=$selectedCodec, lists=$changedLists, removed=$removedFormats"
            }
        } else {
            Logger.printDebug {
                "VideoCodec: selected=$selectedCodec, lists already up to date"
            }
        }
    }

    private fun filteredFormats(formats: List<*>, codecMarker: String): List<*> {
        val mimeTypes = formats.map(::mimeTypeOf)
        if (mimeTypes.none { it?.startsWith("video/") == true }) return formats

        val filteredFormats = formats.filterIndexed { index, _ ->
            val mimeType = mimeTypes[index]
            mimeType == null || !mimeType.startsWith("video/") || mimeType.contains(codecMarker)
        }

        // Never replace the list if it would remove every video format.
        if (filteredFormats.size == formats.size ||
            filteredFormats.none { mimeTypeOf(it)?.startsWith("video/") == true }
        ) {
            return formats
        }

        return ArrayList(filteredFormats)
    }

    private fun codecMarker(codec: String): String? = when (codec) {
        CODEC_AV1 -> "av01"
        CODEC_VP9 -> "vp9"
        CODEC_AVC -> "avc"
        else -> null
    }

    private fun ensurePreferenceListener() {
        if (preferenceListenerRegistered) return

        val context = runCatching { Utils.getContext() }.getOrNull() ?: return
        context.getSharedPreferences(CODEC_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener { _, key ->
                if (key != CODEC_PREFERENCE_KEY) return@registerOnSharedPreferenceChangeListener

                runCatching { reapply() }
                    .onFailure { Logger.printException({ "VideoCodec: re-apply failed" }, it) }
                runCatching { VideoCodecButtonUi.updateText() }
                runCatching { VideoCodecReloader.reload() }
                    .onFailure { Logger.printException({ "VideoCodec: reload failed" }, it) }
            }
        preferenceListenerRegistered = true
    }

    /**
     * The format model is obfuscated, but its toString() always contains the mime type.
     * Extracting it avoids depending on obfuscated getter or field names.
     */
    private fun mimeTypeOf(format: Any?): String? {
        if (format == null) return null
        val text = runCatching { format.toString() }.getOrNull() ?: return null
        val start = text.indexOf("mimeType=")
        if (start < 0) return null

        val rest = text.substring(start + "mimeType=".length)
        val end = rest.indexOf(" drmFamilies=").let { if (it >= 0) it else rest.length }
        return rest.substring(0, end).trim()
    }
}
