package io.github.nexalloy.morphe.music.misc.privacy

import io.github.nexalloy.patch
import io.github.nexalloy.morphe.music.misc.settings.PreferenceScreen
import io.github.nexalloy.morphe.shared.misc.privacy.SanitizeSharingLinks
import io.github.nexalloy.morphe.shared.misc.privacy.registerSanitizeSharingLinksPreferences

val SanitizeSharingLinks = patch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from shared links.",
    settings = {
        registerSanitizeSharingLinksPreferences(
            preferenceScreen = PreferenceScreen.MISC,
            replaceMusicLinksWithYouTube = true
        )
    },
) {
    SanitizeSharingLinks(
        preferenceScreen = PreferenceScreen.MISC,
        replaceMusicLinksWithYouTube = true
    )
}