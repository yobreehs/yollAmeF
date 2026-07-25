package io.github.nexalloy.morphe.shared.misc.litho.context

import app.morphe.extension.shared.patches.components.ContextInterface
import io.github.nexalloy.patch
import java.lang.reflect.Field

lateinit var identifierField: Field
lateinit var pathBuilderField: Field

data class ConversionContext(val conversion: Any) : ContextInterface {
    override fun patch_getPathBuilder() =
        pathBuilderField.get(conversion) as StringBuilder

    override fun patch_getIdentifier() =
        identifierField.get(conversion) as? String ?: ""

    override fun toString() = conversion.toString()
}

/**
 * Shared factory for the ConversionContext patch used by both YouTube and YT Music.
 */
internal val conversionContextPatch = patch(
    description = "Hooks the method to use the conversion context in an extension."
) {

    identifierField = ::identifierFieldData.field
    pathBuilderField = ::pathBuilderFieldData.field
}