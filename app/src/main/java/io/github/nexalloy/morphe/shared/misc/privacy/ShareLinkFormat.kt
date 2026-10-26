package io.github.nexalloy.morphe.shared.misc.privacy

import android.content.Context
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.Utils

/**
 * Rewrites shared YouTube links into the format picked in Misc.
 *
 * This replaces the two conflicting toggles that used to exist: the extension's
 * "Change share links to youtu.be" and the app's "Use old share links". Only one
 * of them could ever win, so both are now a single three-state preference.
 */
internal object ShareLinkFormat {
    const val PREFERENCE_KEY = "morphe_share_link_format"

    const val FORMAT_DEFAULT = "default"
    const val FORMAT_SHORT = "short"
    const val FORMAT_FULL = "full"

    private const val PREFERENCES_NAME = "morphe_prefs"
    private const val LEGACY_OLD_LINKS_KEY = "morphe_use_old_share_links"
    private const val LEGACY_SHORTENER_KEY = "morphe_replace_links_with_shortener"
    private const val SHORT_URL = "https://youtu.be/"
    private const val FULL_URL = "https://www.youtube.com/watch?v="

    private val shortLinkRegex =
        Regex("""https?://(?:www\.|m\.)?youtu\.be/([A-Za-z0-9_-]{11})(?:\?([^\s]*))?""")
    private val shortsLinkRegex =
        Regex("""https?://(?:www\.|m\.)?youtube\.com/shorts/([A-Za-z0-9_-]{11})(?:\?([^\s]*))?""")
    private val watchLinkRegex =
        Regex("""https?://(?:www\.|m\.)?youtube\.com/watch\?([^\s]+)""")
    private val videoIdRegex =
        Regex("""(?:^|&)v=([A-Za-z0-9_-]{11})(?=[&#]|$)""")

    fun format(): String = runCatching {
        val preferences = Utils.getContext()
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.getString(PREFERENCE_KEY, null) ?: when {
            // Migrate the two old toggles that this setting replaces.
            preferences.getBoolean(LEGACY_OLD_LINKS_KEY, false) -> FORMAT_FULL
            preferences.getBoolean(LEGACY_SHORTENER_KEY, false) -> FORMAT_SHORT
            else -> FORMAT_DEFAULT
        }
    }.getOrDefault(FORMAT_DEFAULT)

    fun convert(text: String): String = convert(text, format())

    internal fun convert(text: String, format: String): String {
        if (format != FORMAT_SHORT && format != FORMAT_FULL) return text
        if (!text.contains("youtu.be/") && !text.contains("youtube.com/")) return text

        val converted = when (format) {
            FORMAT_SHORT -> text
                .replace(watchLinkRegex) { match -> shortLink(match.value, match.groupValues[1]) }
                .replace(shortsLinkRegex) { match ->
                    shortLink(match.value, match.groupValues[1], match.groupValues[2])
                }

            else -> text
                .replace(shortLinkRegex) { match ->
                    fullLink(match.groupValues[1], match.groupValues[2])
                }
                .replace(shortsLinkRegex) { match ->
                    fullLink(match.groupValues[1], match.groupValues[2])
                }
        }

        if (converted != text) {
            // Never let diagnostics break sharing.
            runCatching { Logger.printDebug { "ShareLinkFormat ($format): $text -> $converted" } }
        }
        return converted
    }

    private fun shortLink(original: String, query: String): String {
        val videoId = videoIdRegex.find(query)?.groupValues?.get(1) ?: return original
        return buildString {
            append(SHORT_URL).append(videoId)
            appendQuery(query)
        }
    }

    private fun shortLink(original: String, videoId: String, query: String): String {
        if (videoId.isEmpty()) return original
        return buildString {
            append(SHORT_URL).append(videoId)
            appendQuery(query)
        }
    }

    private fun fullLink(videoId: String, query: String): String =
        buildString {
            append(FULL_URL).append(videoId)
            appendQuery(query, '&')
        }

    private fun StringBuilder.appendQuery(query: String, firstSeparator: Char = '?') {
        query.split('&')
            .filter { it.isNotBlank() && !it.startsWith("v=") }
            .forEachIndexed { index, parameter ->
                append(if (index == 0) firstSeparator else '&').append(parameter)
            }
    }
}
