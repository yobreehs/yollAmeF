package io.github.nexalloy.morphe.youtube.layout.hide.shelves

import app.morphe.extension.youtube.patches.components.HorizontalShelvesFilter
import io.github.nexalloy.morphe.shared.misc.litho.filter.addLithoFilter
import io.github.nexalloy.morphe.youtube.layout.buttons.navigation.NavigationBar
import io.github.nexalloy.morphe.youtube.misc.engagement.EngagementPanelHook
import io.github.nexalloy.morphe.youtube.misc.litho.filter.LithoFilter
import io.github.nexalloy.morphe.youtube.misc.litho.observer.LayoutReloadObserver
import io.github.nexalloy.morphe.youtube.misc.playertype.PlayerTypeHook
import io.github.nexalloy.patch

val HideHorizontalShelves = patch {
    dependsOn(
        LithoFilter,
        PlayerTypeHook,
        NavigationBar,
        EngagementPanelHook,
        LayoutReloadObserver
    )

    addLithoFilter(HorizontalShelvesFilter())
}