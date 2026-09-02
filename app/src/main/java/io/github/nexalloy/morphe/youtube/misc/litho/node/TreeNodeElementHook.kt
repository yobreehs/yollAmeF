package io.github.nexalloy.morphe.youtube.misc.litho.node

import io.github.nexalloy.morphe.shared.misc.litho.context.conversionContextPatch
import io.github.nexalloy.morphe.shared.misc.litho.node.createTreeNodeElementHookPatch
import io.github.nexalloy.patch

val TreeNodeElementHook = createTreeNodeElementHookPatch(
    patch {},
    conversionContextPatch,
    false,
    useLegacyContextRegister = { false }
)