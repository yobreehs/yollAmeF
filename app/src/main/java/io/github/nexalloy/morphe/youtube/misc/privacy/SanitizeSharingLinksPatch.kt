package io.github.nexalloy.morphe.youtube.misc.privacy

import io.github.nexalloy.morphe.shared.misc.privacy.SanitizeSharingLinks
import io.github.nexalloy.morphe.shared.misc.privacy.registerSanitizeSharingLinksPreferences
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

val SanitizeSharingLinks = patch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from shared links.",
    settings = {
        registerSanitizeSharingLinksPreferences(
            preferenceScreen = PreferenceScreen.MISC,
            shareLinkFormat = true
        )
    },
) {
    SanitizeSharingLinks(
        preferenceScreen = PreferenceScreen.MISC,
        shareLinkFormat = true
    )
}