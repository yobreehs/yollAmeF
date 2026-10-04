package io.github.nexalloy.morphe.shared.misc.debugging

import android.app.Activity
import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.StringRef.str
import app.morphe.extension.shared.Utils
import io.github.nexalloy.snapshotAliveActivities

/**
 * A debug preference that dumps the view hierarchy of every alive activity into the log.
 *
 * Used to identify renderers that bypass the Litho filters (e.g. single full-width Shorts on the
 * home feed on YouTube 21.39): scroll to the component, open Settings -> Debug and tap this entry.
 * The logged tree reveals the exact view class names/ids of the rendered component.
 */
@Suppress("unused", "deprecation")
class DumpViewHierarchyPreference : Preference {

    init {
        setOnPreferenceClickListener {
            dumpViewHierarchy()
            true
        }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    companion object {

        /** Dumps the view tree of every alive activity to the debug log. */
        @JvmStatic
        fun dumpViewHierarchy() {
            val activities = snapshotAliveActivities()
            if (activities.isEmpty()) {
                Logger.printDebug { "VD: no alive activities to dump" }
                Utils.showToastShort(str("morphe_debug_view_dump_empty"))
                return
            }
            activities.forEach { activity ->
                runCatching {
                    val sb = StringBuilder()
                    sb.append("\n===== VIEW DUMP: ").append(activity.javaClass.name).append(" =====")
                    val decor = activity.window?.decorView
                    if (decor == null) {
                        sb.append("\n  <no decorView>")
                    } else {
                        appendView(decor, 0, sb)
                    }
                    sb.append("\n===== END VIEW DUMP =====")
                    Logger.printDebug { sb.toString() }
                }.onFailure {
                    Logger.printException({ "View dump failed for $activity" }, it)
                }
            }
            Utils.showToastShort(
                str("morphe_debug_view_dump_done")
                    .replace("{count}", activities.size.toString())
            )
        }

        private fun appendView(view: View, depth: Int, sb: StringBuilder) {
            sb.append('\n')
            repeat(depth) { sb.append("  ") }
            sb.append(view.javaClass.simpleName)

            val vis = when (val v = view.visibility) {
                View.VISIBLE -> "V"
                View.INVISIBLE -> "I"
                else -> "G"
            }
            sb.append("|").append(vis)

            val id = view.id
            if (id != View.NO_ID) {
                val idName = runCatching { view.resources.getResourceEntryName(id) }.getOrNull()
                sb.append("|id=").append(idName ?: ("0x" + Integer.toHexString(id)))
            }

            if (view is TextView) {
                view.text?.let { sb.append("|t=").append(it) }
                view.contentDescription?.let { sb.append("|cd=").append(it) }
            } else {
                view.contentDescription?.let {
                    sb.append("|cd=").append(contextStringOf(it))
                }
            }

            if (view is ViewGroup && view.childCount > 0) {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i) ?: continue
                    appendView(child, depth + 1, sb)
                }
            }
        }

        private fun contextStringOf(cd: CharSequence): String = cd.toString()
    }
}