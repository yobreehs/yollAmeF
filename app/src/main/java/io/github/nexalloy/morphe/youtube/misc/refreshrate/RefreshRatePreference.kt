package io.github.nexalloy.morphe.youtube.misc.refreshrate

import android.app.AlertDialog
import android.content.Context
import android.preference.Preference
import android.preference.PreferenceManager
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar

/**
 * Refresh rate preference: a seekbar from 30 to 999 Hz plus a manual numeric input.
 * The value is stored as an int (0 = use the device default) in the same preference file
 * the module's settings UI writes to, so [AppRefreshRateController] can read it via XSharedPreferences.
 */
class RefreshRatePreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager) {
        super.onAttachedToHierarchy(preferenceManager)
        updateSummary()
    }

    override fun onClick() {
        val pending = intArrayOf(savedValue().coerceIn(MIN, MAX))

        val input = EditText(context).apply {
            setText(pending[0].toString())
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
        }

        val seekBar = SeekBar(context).apply {
            max = MAX - MIN
            progress = pending[0] - MIN
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    pending[0] = progress + MIN
                    input.setText(pending[0].toString())
                }
                override fun onStartTrackingTouch(bar: SeekBar) {}
                override fun onStopTrackingTouch(bar: SeekBar) {}
            })
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            addView(seekBar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ))
            val inputParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputParams.topMargin = dp(12)
            addView(input, inputParams)
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(content)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val manual = input.text.toString().trim().toIntOrNull()
                val value = (manual ?: pending[0]).coerceIn(MIN, MAX)
                saveValue(value)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun savedValue(): Int =
        runCatching { sharedPreferences.getInt(key, 0) }.getOrDefault(0)

    private fun saveValue(value: Int) {
        runCatching {
            sharedPreferences.edit().putInt(key, value).commit()
        }
        updateSummary()
        notifyChanged()
    }

    private fun updateSummary() {
        val value = savedValue()
        summary = if (value <= 0) "Default" else "$value Hz"
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val MIN = 30
        const val MAX = 999
    }
}
