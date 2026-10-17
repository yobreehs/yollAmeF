package io.github.nexalloy.morphe.youtube.misc.playercontrols

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.resourceMappings

val fullscreen_button_id get() = resourceMappings["id", "fullscreen_button"]

@get:RequireAppVersion("21.36.000", maxVersion = "21.41.000")
val autonavEndscreenFlagCallers = findMethodListDirect {
    val flagMethod = NewPlayerOverlaysFeatureFlagFingerprint()
    AutonavEndscreenOverlayFingerprint().declaredClass!!.methods.filter { method ->
        method.invokes.any { it.descriptor == flagMethod.descriptor }
    }.also {
        require(it.isNotEmpty()) { "Did not apply autonav endscreen changes" }
    }
}

@RequireAppVersion("21.04.000", maxVersion = "21.41.000")
internal object NewPlayerOverlaysFeatureFlagFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(literal(45752335L))
)

@RequireAppVersion("21.36.000", maxVersion = "21.41.000")
internal object AutonavEndscreenOverlayFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("player_overlay_player_autonav_endscreen")
)

internal object PlayerControlsVisibilityEntityModelInit : Fingerprint(
    classFingerprint = PlayerControlsVisibilityEntityModelFingerprint,
    name = "<init>"
)

internal object PlayerControlsVisibilityEntityModelFingerprint : Fingerprint(
    name = "getPlayerControlsVisibility",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "L",
    parameters = listOf(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET,
        Opcode.INVOKE_STATIC
    )
)
