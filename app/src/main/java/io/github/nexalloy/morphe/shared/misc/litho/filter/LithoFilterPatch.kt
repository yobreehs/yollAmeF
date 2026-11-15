package io.github.nexalloy.morphe.shared.misc.litho.filter

import app.morphe.extension.shared.Logger
import app.morphe.extension.shared.patches.components.Filter
import app.morphe.extension.shared.patches.components.LithoFilterPatch
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.Patch
import io.github.nexalloy.PatchExecutor
import io.github.nexalloy.hookMethod
import io.github.nexalloy.morphe.shared.misc.litho.context.ConversionContext
import io.github.nexalloy.morphe.youtube.insertLiteralOverride
import io.github.nexalloy.new
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook
import java.nio.ByteBuffer

fun addLithoFilter(filter: Filter) {
    runCatching {
        LithoFilterPatch.addFilter(filter)
    }.onFailure { ex ->
        // A single bad filter must not take down the whole patch that registers it.
        Logger.printException({ "LithoFilter: failed to register ${filter.javaClass.simpleName}" }, ex)
    }
}


/**
 * Shared Litho component filter factory used by both YouTube and YT Music.
 *
 * The following patch inserts a hook into the method that parses the bytes into a ComponentContext.
 * This method contains a StringBuilder object that represents the pathBuilder of the component.
 * The pathBuilder is used to filter components by their path.
 *
 * Additionally, the method contains a reference to the component's identifier.
 * The identifier is used to filter components by their identifier.
 *
 * The protobuf buffer is passed along from a different injection point before the filtering occurs.
 * The buffer is a large byte array that represents the component tree.
 * This byte array is searched for strings that indicate the current component.
 *
 * All modifications done here must allow all the original code to still execute
 * even when filtering, otherwise memory leaks or poor app performance may occur.
 *
 * The following pseudocode shows how this patch works:
 *
 * class SomeOtherClass {
 *    // Called before ComponentContextParser.parseComponent() method.
 *    public void someOtherMethod(ByteBuffer byteBuffer) {
 *        ExtensionClass.setProtoBuffer(byteBuffer); // Inserted by this patch.
 *        ...
 *   }
 * }
 *
 * class ComponentContextParser {
 *    public Component parseComponent() {
 *        ...
 *
 *        if (extensionClass.shouldFilter()) {  // Inserted by this patch.
 *            return emptyComponent;
 *        }
 *        return originalUnpatchedComponent; // Original code.
 *    }
 * }
 *
 * @param hookNonNativeBuffer Whether to also insert the non-native ByteBuffer hook.
 *                            Older client versions push data through a non-native path; newer ones
 *                            (YouTube 20.22+, YT Music 9.x) always use the native Upb encode path.
 *                            Evaluated lazily inside execute so version flags are already set.
 * @param overrideUpbFeatureFlag Whether to override the A/B feature flag that enables Upb-native
 *                               protobuf parsing (only present on older YouTube; absent in Music).
 * @param block Callback to add app-specific dependencies (sharedExtensionPatch, conversionContextPatch,
 *              versionCheckPatch, and any app-specific fixes).
 */
internal fun sharedLithoFilterPatch(
    hookNonNativeBuffer: () -> Boolean,
    overrideUpbFeatureFlag: () -> Boolean,
    useLegacyLithoFiltering: () -> Boolean,
    block: PatchExecutor.() -> Unit,
): Patch = patch(
    description = "Hooks the method which parses the bytes into a ComponentContext to filter components."
) {
    block()


    // v2.2 removed useLegacyLithoFiltering(); keep the hook optional for older bases.
    runCatching {
        LithoFilterPatch::class.java.getDeclaredMethod("useLegacyLithoFiltering")
            .hookMethod(XC_MethodReplacement.returnConstant(useLegacyLithoFiltering()))
    }

    // region Hook the method that parses bytes into a ComponentContext.

    // Return an EmptyComponent instead of the original component if the filterState method returns true.

    val buttonViewModelThreadLocal = ThreadLocal<Any?>()
    ComponentCreateFingerprint.hookMethod(scopedHook(::buttonViewModelReceiver.method) {
        before {
            buttonViewModelThreadLocal.set(it.args[0])
        }
    })

    ComponentCreateFingerprint.hookMethod {
        val emptyComponentClazz = ::emptyComponentClass.clazz
        val protoBufferEncodeMethod = ProtobufBufferEncodeFingerprint.method
        val protoBufferEncodeClass = ProtobufBufferEncodeFingerprint.declaredClass
        val accessibilityIdMethod = ::AccessibilityIdMethod.method
        val accessibilityTextMethod = ::accessibilityTextMethod.method
        // FemAlloy: any failure here used to be thrown back into Litho's component creation on a
        // background thread, spamming the log with uncaught exceptions for every affected component.
        // Report it once and let the component be created unfiltered instead.
        val filterFailureReported = java.util.concurrent.atomic.AtomicBoolean()
        after { param ->
            try {
                val conversion = param.args.getOrNull(1)
                val bufferParent = param.args.getOrNull(2)
                // Verify it's the expected subclass just in case.
                val buffer = if (protoBufferEncodeClass.isInstance(bufferParent)) {
                    protoBufferEncodeMethod(bufferParent) as? ByteArray ?: byteArrayOf()
                } else byteArrayOf()
                val buttonViewModel = buttonViewModelThreadLocal.get()
                buttonViewModelThreadLocal.remove()
                val accessibilityId = buttonViewModel
                    ?.takeIf { accessibilityIdMethod.declaringClass.isInstance(it) }
                    ?.let { accessibilityIdMethod(it) as? String }
                val accessibilityText = buttonViewModel
                    ?.takeIf { accessibilityTextMethod.declaringClass.isInstance(it) }
                    ?.let { accessibilityTextMethod(it) as? String }

                if (conversion != null && LithoFilterPatch.isFiltered(
                        ConversionContext(conversion),
                        buffer,
                        accessibilityId,
                        accessibilityText
                    )
                ) {
                    param.result = emptyComponentClazz.new()
                }
            } catch (ex: Throwable) {
                if (filterFailureReported.compareAndSet(false, true)) {
                    Logger.printException(
                        { "LithoFilterPatch: component filtering failed, the component is shown unfiltered" },
                        ex
                    )
                }
            }
        }
    }

    //endregion

    // region Change Litho thread executor to 1 thread to fix layout issue in unpatched YouTube.

    ::lithoThreadExecutorFingerprint.hookMethod {
        before {
            it.args[0] = LithoFilterPatch.getExecutorCorePoolSize(it.args[0] as Int)
            it.args[1] = LithoFilterPatch.getExecutorMaxThreads(it.args[1] as Int)
        }
    }

    // endregion

    // region A/B test of new Litho native code.

    // Turn off a feature flag that enables native code of protobuf parsing (Upb protobuf).
    // If this is enabled, then the litho protobuffer hook will always show an empty buffer
    // since it's no longer handled by the hooked Java code.
    if (overrideUpbFeatureFlag()) {
        insertLiteralOverride(45419603L)
    }

    // endregion
}