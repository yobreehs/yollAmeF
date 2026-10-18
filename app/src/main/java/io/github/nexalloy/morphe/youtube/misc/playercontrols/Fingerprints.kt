package io.github.nexalloy.morphe.youtube.misc.playercontrols

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchFirst
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.OpcodesFilter
import io.github.nexalloy.RequireAppVersion
import io.github.nexalloy.morphe.StringComparisonType
import io.github.nexalloy.morphe.findFieldDirect
import io.github.nexalloy.morphe.findFieldListDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.resourceMappings
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContextToStringFingerprint
import io.github.nexalloy.morphe.string
import java.lang.reflect.Modifier

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

/**
 * Image component of the Elements templates.
 */
@RequireAppVersion("21.08.000")
internal object ElementsImageComponentFingerprint : Fingerprint(
    filters = listOf(string("Failed to find a valid source for the image", comparison = StringComparisonType.CONTAINS))
)

/**
 * Sets the tint of an image of the Elements templates, with the color given by the image.
 */
@RequireAppVersion("21.08.000")
internal object ElementsImageTintFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("Landroid/graphics/drawable/Drawable;", "L"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("L"),
            returnType = "I",
            location = MatchFirst()
        ),
        methodCall(smali = $$"Landroid/graphics/drawable/Drawable;->setColorFilter(ILandroid/graphics/PorterDuff$Mode;)V")
    )
)

@get:RequireAppVersion("21.08.000")
val imageTintColorMethod = findMethodDirect {
    ElementsImageTintFingerprint.instructionMatches.first().instruction.methodRef!!
}

@get:RequireAppVersion("21.08.000")
val imageComponentConversionContextField = findFieldDirect {
    val contextClass = ConversionContextToStringFingerprint().declaredClass!!
    ElementsImageComponentFingerprint().declaredClass!!.fields.single { it.type == contextClass }
}

@get:RequireAppVersion("21.08.000")
val imageComponentImageFields = findFieldListDirect {
    val imageType = imageTintColorMethod().paramTypes.single()
    ElementsImageComponentFingerprint().declaredClass!!.fields.filter { it.type == imageType }
        .also { require(it.isNotEmpty()) { "No image fields in image component" } }
}

@get:RequireAppVersion("21.08.000")
val imageComponentPrepareMethod = findMethodDirect {
    ElementsImageComponentFingerprint().declaredClass!!.methods.single { method ->
        method.returnType?.descriptor == "V" && method.paramCount == 3 &&
                !Modifier.isStatic(method.modifiers) && method.instructions.any { instruction ->
            instruction.opcode == Opcode.INVOKE_STATIC_RANGE.ordinal &&
                    instruction.methodRef?.paramTypes?.any { it.descriptor == "Landroid/content/Context;" } == true
        }
    }
}
