package io.github.nexalloy.morphe.youtube.layout.hide.shorts

import app.morphe.extension.shared.Utils
import app.morphe.extension.shared.patches.components.BufferAsciiStrings
import app.morphe.extension.shared.patches.components.ContextInterface
import app.morphe.extension.shared.patches.components.Filter
import app.morphe.extension.shared.patches.components.StringFilterGroup
import app.morphe.extension.youtube.shared.NavigationBar
import de.robv.android.xposed.XSharedPreferences
import io.github.nexalloy.BuildConfig

/**
 * Hides Shorts from the home feed.
 *
 * The module's settings UI stores every preference key in the module's `<targetPackage>.xml`
 * file, while the extension settings read `Setting.preferences` ("morphe_prefs") which is never
 * written. The toggle value therefore has to be read from the module's `<targetPackage>.xml`
 * (the same file PatchExecutor reads patch states from), once at startup.
 */
class HomeShortsFilter : Filter() {

    private val hideShortsHomeEnabled: Boolean by lazy {
        runCatching {
            XSharedPreferences(BuildConfig.APPLICATION_ID, Utils.getContext().packageName)
                .getBoolean("morphe_hide_shorts_home", false)
        }.getOrDefault(false)
    }

    init {
        addIdentifierCallbacks(
            StringFilterGroup(null, "shorts_shelf", "shorts_video_cell")
        )
    }

    override fun isFiltered(
        contextInterface: ContextInterface,
        identifier: String,
        accessibility: String,
        path: String,
        buffer: ByteArray,
        asciiStrings: BufferAsciiStrings,
        matchedGroup: StringFilterGroup,
        contentType: Filter.FilterContentType,
        contentIndex: Int,
    ): Boolean {
        if (!hideShortsHomeEnabled) return false

        // Same fallback as ShortsFilter.shouldHideShortsFeedItems:
        // an unknown navigation tab is treated as the home feed.
        val selectedNavButton = NavigationBar.NavigationButton.getSelectedNavigationButton()
        return selectedNavButton == null ||
                selectedNavButton == NavigationBar.NavigationButton.HOME
    }
}
