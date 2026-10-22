package io.github.nexalloy.morphe.shared.misc.spoof

import app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch
import io.github.nexalloy.HookDsl
import io.github.nexalloy.IScopedHookCallback
import io.github.nexalloy.patch
import io.github.nexalloy.scopedHook

// The Cronet hooks are shared with stream spoofing upstream; downloads use them independently.
val plainStreamRequestsPatch = patch {
    // Streams of other clients are plain urls, which the server rejects or sends slowly if they are
    // requested like the streams of the app, with a POST body. Send them as plain GET requests instead.
    val urlField = ::cronetUrlField.field
    val httpMethodField = ::cronetHttpMethodField.field
    val uploadField = ::cronetUploadField.field
    CronetUrlRequestBuildFingerprint.hookMethod {
        before {
            if (SpoofVideoStreamsPatch.isPlainStreamRequest(urlField.get(it.thisObject) as String?)) {
                httpMethodField.set(it.thisObject, "GET")
                uploadField.set(it.thisObject, null)
            }
        }
    }
    // Plain streams are sent slowly if a large range is requested. Request a smaller range instead.
    val limitRange: HookDsl<IScopedHookCallback>.() -> Unit = {
        before {
            if (innerDepth == 0) {
                it.args[0] = SpoofVideoStreamsPatch.limitPlainStreamRange(it.args[0] as String?)
            }
        }
    }
    val createRequestHooks = ::cronetCreateRequestMethods.dexMethodList.map { method ->
        method.toMethod() to limitRange
    }.toTypedArray()
    CronetUrlRequestBuildFingerprint.hookMethod(scopedHook(*createRequestHooks))

    // A shortened response must pause the transfer so saving continues from the last byte read.
    val remainingBytes = ::cronetRemainingBytesField.field
    val pauseTransfer = PriorityTooLowExceptionFingerprint.constructor
    listOf(CronetDataSourceReadFingerprint, CronetDataSourceReadBufferFingerprint).forEach { fingerprint ->
        fingerprint.hookMethod {
            before {
                if (SpoofVideoStreamsPatch.isLimitedStreamRangeFinished(remainingBytes.getLong(it.thisObject))) {
                    it.throwable = pauseTransfer.newInstance(0, 0) as Throwable
                }
            }
            after {
                // A successful read still delivers its bytes; the next read pauses at entry.
                if (!it.hasThrowable() && it.result == -1 &&
                    SpoofVideoStreamsPatch.isLimitedStreamRangeFinished(remainingBytes.getLong(it.thisObject))
                ) {
                    it.throwable = pauseTransfer.newInstance(0, 0) as Throwable
                }
            }
        }
    }
}
