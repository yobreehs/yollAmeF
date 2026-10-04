package io.github.nexalloy.morphe.youtube.interaction.channelsearch

import app.morphe.extension.youtube.patches.ChannelSearchPatch as ExtensionChannelSearchPatch
import io.github.nexalloy.morphe.shared.misc.settings.preference.PreferenceCategory
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

/**
 * Adds an option to search inside the channel that is currently open instead of
 * searching all of YouTube. All hooks condition on the extension setting internally,
 * so they are safe to register unconditionally.
 */
internal val channelSearch = patch(
    name = "Channel search",
    description = "Adds an option to search inside the channel that is currently open " +
        "instead of searching all of YouTube.",
) {
    PreferenceScreen.NEW_PATCHES.addPreferences(
        PreferenceCategory(
            key = "morphe_new_patches_chapter_4",
            titleKey = "morphe_new_patches_chapter_4_title",
            preferences = setOf(
                SwitchPreference("morphe_channel_search", summary = true),
            ),
        ),
    )

    // Leaving the channel for the search feed shows no browse page, so nothing would otherwise
    // replace the browse id of the channel.
    ChannelSearchResultsFragmentOnCreateViewFingerprint.hookMethod {
        before {
            ExtensionChannelSearchPatch.clearBrowseId()
        }
    }

    // The search box otherwise still reads as a search of all of YouTube.
    // Only the default hint is replaced, not the hint of Shorts or playlist search.
    ChannelSearchBoxHintFingerprint.hookMethod {
        after { param ->
            param.result = ExtensionChannelSearchPatch.getSearchHint(
                param.result as? String ?: "",
            )
        }
    }

    // Every search submit path funnels through this method. If it is a search in the channel,
    // the extension handles it with a dialog and the original search is suppressed.
    ChannelSearchSubmitFingerprint.hookMethod {
        before { param ->
            val query = param.args.firstOrNull() as? String
            if (ExtensionChannelSearchPatch.searchInChannel(query)) {
                param.result = null
            }
        }
    }

    // Keep the browse id current while browsing a channel: the fragment is handed the endpoint
    // of every page it shows, including pages served from cache, which make no browse request.
    // (The endpoint is param 1; the browse fragment instance is the receiver.)
    ::channelBrowseIdSetterMethod.hookMethod {
        after { param ->
            val endpoint = param.args.firstOrNull()
            val browseId = try {
                ::channelBrowseIdMethod.method.invoke(null, endpoint) as? String
            } catch (_: Throwable) {
                null
            }
            ExtensionChannelSearchPatch.setBrowseId(browseId)
        }
    }
}