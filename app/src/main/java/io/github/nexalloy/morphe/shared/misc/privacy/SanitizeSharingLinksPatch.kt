package io.github.nexalloy.morphe.shared.misc.privacy

import android.content.ClipData
import android.content.Intent
import app.morphe.extension.shared.patches.SanitizeSharingLinksPatch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.BasePreferenceScreen
import io.github.nexalloy.morphe.shared.misc.settings.preference.ListPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.shared.misc.settings.preference.noTitleUnsortedPreferenceCategory

/**
 * Preference part of [SanitizeSharingLinks]. Kept separate, because the preferences have to be
 * registered even when the patch is disabled.
 */
fun registerSanitizeSharingLinksPreferences(
    preferenceScreen: BasePreferenceScreen.Screen,
    replaceMusicLinksWithYouTube: Boolean = false,
    shareLinkFormat: Boolean = false
) {
    val sanitizePreference = SwitchPreference("morphe_sanitize_sharing_links", summary = true)

    preferenceScreen.addPreferences(
        if (replaceMusicLinksWithYouTube || shareLinkFormat) {
            val preferences = mutableSetOf<BasePreference>(sanitizePreference)
            if (replaceMusicLinksWithYouTube) preferences += SwitchPreference("morphe_replace_music_with_youtube", summary = true)
            if (shareLinkFormat) preferences += ListPreference(
                key = ShareLinkFormat.PREFERENCE_KEY,
                entriesKey = "morphe_share_link_format_entries",
                entryValuesKey = "morphe_share_link_format_entry_values",
                summaryKey = "morphe_share_link_format_summary"
            )

            noTitleUnsortedPreferenceCategory(preferences)
        } else {
            sanitizePreference
        }
    )
}

fun PatchExecutor.SanitizeSharingLinks(
    preferenceScreen: BasePreferenceScreen.Screen,
    replaceMusicLinksWithYouTube: Boolean = false,
    shareLinkFormat: Boolean = false
) {

    val sanitizeArg1 = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val url = param.args[1] as? String ?: return
            if (!url.startsWith("https://")) return
            val sanitizedUrl = SanitizeSharingLinksPatch.sanitize(url)
            param.args[1] = ShareLinkFormat.convert(sanitizedUrl)
        }
    }

    XposedHelpers.findAndHookMethod(
        ClipData::class.java.name,
        lpparam.classLoader,
        "newPlainText",
        CharSequence::class.java,
        CharSequence::class.java,
        sanitizeArg1
    )

    XposedHelpers.findAndHookMethod(
        Intent::class.java.name,
        lpparam.classLoader,
        "putExtra",
        String::class.java,
        String::class.java,
        sanitizeArg1
    )
}