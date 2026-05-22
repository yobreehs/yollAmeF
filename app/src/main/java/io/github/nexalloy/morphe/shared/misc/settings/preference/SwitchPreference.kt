@file:Suppress("DEPRECATION", "DiscouragedApi")

package io.github.nexalloy.morphe.shared.misc.settings.preference

import android.content.Context
import android.preference.Preference
import android.preference.PreferenceManager
import android.preference.SwitchPreference

@Suppress("MemberVisibilityCanBePrivate")
class SwitchPreference(
    key: String? = null,
    titleKey: String = "${key}_title",
    summaryKey: String? = "${key}_summary",
    tag: Class<out Preference> = SwitchPreference::class.java,
    icon: String? = null,
    iconBold: String? = null,
    layout: String? = null,
) : BasePreference(key, titleKey, summaryKey, icon, iconBold, layout, tag) {
    override fun build(ctx: Context, prefMgr: PreferenceManager): Preference {
        return SwitchPreference(ctx).apply {
            applyBaseAttrs(this)
        }
    }
}