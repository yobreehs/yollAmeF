package io.github.nexalloy.morphe.reddit.ad

import app.morphe.extension.shared.Logger
import io.github.nexalloy.getObjectField
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.reddit.misc.version.VersionCheck
import io.github.nexalloy.morphe.reddit.misc.version.is_2026_04_0_or_greater
import io.github.nexalloy.morphe.reddit.misc.version.is_2026_16_0_or_greater
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

val HideAds = patch(
    name = "Hide ads",
    description = "Adds options to hide ads."
) {
    dependsOn(VersionCheck)

    // region Filter promoted ads (does not work in popular or latest feed)

    fun hideOldAds(fingerprint: Fingerprint) {
        fingerprint.hookMethod {
            val iLink = classLoader.loadClass("com.reddit.domain.model.ILink")
            val getPromoted = iLink.methods.single { it.name == "getPromoted" }
            after { param ->
                val arrayList = param.thisObject.getObjectField("children") as Iterable<Any?>
                val result = mutableListOf<Any?>()
                var filtered = 0
                for (item in arrayList) {
                    try {
                        if (item != null && iLink.isAssignableFrom(item.javaClass) && getPromoted(item) == true) {
                            filtered++
                            continue
                        }
                    } catch (_: Throwable) {
                        Logger.printDebug { "not iLink, keep it" }
                        // not iLink, keep it
                    }
                    result.add(item)
                }
                Logger.printDebug { "Filtered $filtered ads in ${arrayList.count()} posts" }
                param.thisObject.setObjectField("children", result)
            }
        }
    }

    hideOldAds(ListingFingerprint)

    if (!is_2026_16_0_or_greater) {
        hideOldAds(SubmittedListingFingerprint)
    }

    AdPostSectionConstructorFingerprint.hookMethod {
        val immutableListBuilder = ::ImmutableListBuilderReference.method
        before {
            Logger.printDebug { "Emptied AdPostSection" }
            it.args[4] = immutableListBuilder(null, arrayListOf<Any>())
        }
    }

    // endregion

    // region Filter comment ads

    CommentsViewModelAdLoaderFingerprint.hookMethod {
        before {
            it.result = Unit
        }
    }

    // As of Reddit 2026.04+, placeholders are not hidden unless 'adsLoadCompleted' is false.
    // Hide placeholders by overriding 'adsLoadCompleted' to true.
    if (is_2026_04_0_or_greater) {
        CommentsAdStateConstructorFingerprint.hookMethod {
            val adsLoadCompletedField = ::adsLoadCompletedField.field
            after {
                adsLoadCompletedField.set(it.thisObject, true)
            }
        }
    }

    // endregion
}