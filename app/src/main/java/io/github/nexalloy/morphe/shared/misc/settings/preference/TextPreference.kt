@file:Suppress("DEPRECATION", "DiscouragedApi")

package io.github.nexalloy.morphe.shared.misc.settings.preference

import android.content.Context
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceManager
import app.morphe.extension.shared.settings.preference.ResettableEditTextPreference
import android.text.InputType as AndroidInputType

class TextPreference(
    key: String? = null,
    titleKey: String = "${key}_title",
    summaryKey: String? = "${key}_summary",
    icon: String? = null,
    iconBold: String? = null,
    layout: String? = null,
    tag: Class<out Preference> = ResettableEditTextPreference::class.java,
    val inputType: InputType = InputType.TEXT
) : BasePreference(key, titleKey, summaryKey, icon, iconBold, layout, tag) {
    override fun build(ctx: Context, prefMgr: PreferenceManager): Preference =
        super.build(ctx, prefMgr).apply {
            if (this is EditTextPreference && this@TextPreference.inputType != InputType.TEXT) {
                editText.inputType = when (this@TextPreference.inputType) {
                    InputType.TEXT -> AndroidInputType.TYPE_CLASS_TEXT
                    InputType.TEXT_CAP_CHARACTERS ->
                        AndroidInputType.TYPE_CLASS_TEXT or AndroidInputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                    InputType.TEXT_MULTI_LINE ->
                        AndroidInputType.TYPE_CLASS_TEXT or AndroidInputType.TYPE_TEXT_FLAG_MULTI_LINE
                    InputType.TEXT_PASSWORD ->
                        AndroidInputType.TYPE_CLASS_TEXT or AndroidInputType.TYPE_TEXT_VARIATION_PASSWORD
                    InputType.NUMBER -> AndroidInputType.TYPE_CLASS_NUMBER
                    InputType.NUMBER_DECIMAL ->
                        AndroidInputType.TYPE_CLASS_NUMBER or AndroidInputType.TYPE_NUMBER_FLAG_DECIMAL
                }
            }
        }
}
