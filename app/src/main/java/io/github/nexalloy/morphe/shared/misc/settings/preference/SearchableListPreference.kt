@file:Suppress("DEPRECATION", "DiscouragedApi")

package io.github.nexalloy.morphe.shared.misc.settings.preference

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.preference.ListPreference
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import app.morphe.extension.shared.StringRef

/**
 * A [ListPreference] whose dialog has a search field above the list.
 *
 * Used for long lists such as the preferred subtitle language: the items can be filtered by
 * either the displayed name or the stored value (language code).
 */
class SearchableListPreference(context: Context, attrs: AttributeSet?) :
    ListPreference(context, attrs) {

    private var dialog: AlertDialog? = null

    override fun showDialog(state: Bundle?) {
        val entries = entries ?: return
        val values = entryValues ?: return
        if (entries.isEmpty() || entries.size != values.size) return

        val ctx = context
        val density = ctx.resources.displayMetrics.density

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (16 * density).toInt(),
                (8 * density).toInt(),
                (16 * density).toInt(),
                0
            )
        }

        val search = EditText(ctx).apply {
            hint = runCatching {
                StringRef.str("morphe_preferred_caption_language_search_hint")
            }.getOrDefault("Search")
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        val listView = ListView(ctx)

        root.addView(
            search,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            listView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        var filtered: List<Int> = entries.indices.toList()

        val adapter = ArrayAdapter<CharSequence>(
            ctx,
            android.R.layout.simple_list_item_1,
            filtered.map { entries[it] }
        )
        listView.adapter = adapter

        fun applyFilter(query: String) {
            val q = query.trim()
            filtered = entries.indices.filter { index ->
                q.isEmpty() ||
                    entries[index].contains(q, ignoreCase = true) ||
                    values[index].contains(q, ignoreCase = true)
            }
            adapter.clear()
            adapter.addAll(filtered.map { entries[it] })
            adapter.notifyDataSetChanged()
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        // Preselect the current value.
        runCatching {
            val current = value
            val index = filtered.indexOfFirst { values[it].toString() == current }
            if (index >= 0) {
                listView.setItemChecked(index, true)
                listView.setSelection(index)
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val index = filtered.getOrNull(position) ?: return@setOnItemClickListener
            val newValue = values[index].toString()
            if (callChangeListener(newValue)) {
                value = newValue
                summary = entries[index]
            }
            dismissDialog()
        }

        dialog = AlertDialog.Builder(ctx)
            .setTitle(title)
            .setView(root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .also { it.setOnDismissListener { dialog = null } }
        dialog?.show()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        dialog?.dismiss()
        dialog = null
        super.onDialogClosed(positiveResult)
    }

    private fun dismissDialog() {
        runCatching { dialog?.dismiss() }
        dialog = null
    }
}
