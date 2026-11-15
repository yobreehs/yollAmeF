package io.github.nexalloy.morphe.youtube.misc.navigation

import io.github.nexalloy.SkipTest
import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterImmediately
import io.github.nexalloy.morphe.InstructionLocation.MatchAfterWithin
import io.github.nexalloy.morphe.Opcode
import io.github.nexalloy.morphe.ResourceType
import io.github.nexalloy.morphe.accessFlags
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findClassDirect
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.fingerprint
import io.github.nexalloy.morphe.literal
import io.github.nexalloy.morphe.methodCall
import io.github.nexalloy.morphe.opcode
import io.github.nexalloy.morphe.resourceLiteral
import io.github.nexalloy.morphe.resourceMappings
import io.github.nexalloy.morphe.returns

// val actionBarSearchResultsFingerprint = fingerprint {
//    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
//    returns("Landroid/view/View;")
//    literal { actionBarSearchResultsViewMicId }
//}

val toolbarContainerId get() = resourceMappings["id", "toolbar_container"]

object ToolbarLayoutFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.CONSTRUCTOR),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "toolbar_container")
    )
)

/**
 * Matches to https://android.googlesource.com/platform/frameworks/support/+/9eee6ba/v7/appcompat/src/android/support/v7/widget/Toolbar.java#963
 */
object AppCompatToolbarBackButtonFingerprint : Fingerprint(
    definingClass = "Landroid/support/v7/widget/Toolbar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/graphics/drawable/Drawable;",
    parameters = listOf()
)

/**
 * Matches to the class found in [pivotBarConstructorFingerprint].
 */
val initializeButtonsFingerprint = fingerprint {
    classFingerprint(pivotBarConstructorFingerprint)
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    strings("FEvideo_picker")
}

val getNavigationEnumMethod = findMethodDirect {
    initializeButtonsFingerprint().invokes.findMethod {
        matcher {
            declaredClass(navigationEnumClass(this@findMethodDirect).name)
            accessFlags(AccessFlags.STATIC)
        }
    }.single()
}

/**
 * Matches to the Enum class that looks up ordinal -> instance.
 */
val navigationEnumFingerprint = fingerprint {
    accessFlags(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR)
    strings(
        "PIVOT_HOME",
        "TAB_SHORTS",
        "CREATION_TAB_LARGE",
        "PIVOT_SUBSCRIPTIONS",
        "TAB_ACTIVITY",
        "VIDEO_LIBRARY_WHITE",
        "INCOGNITO_CIRCLE",
    )
}

val navigationEnumClass = findClassDirect { navigationEnumFingerprint().declaredClass!! }

val pivotBarButtonsCreateDrawableViewFingerprint = findMethodDirect {
    val candidates = findMethod {
        matcher {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
            returns("Landroid/view/View;")
            declaredClass {
                descriptor =
                    "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"
            }
        }
    }
    // Up to 21.42 this method takes the tab drawable as its first argument.
    val drawableMethod = candidates.firstOrNull {
        it.paramTypes.firstOrNull()?.descriptor == "Landroid/graphics/drawable/Drawable;"
    }
    if (drawableMethod != null) return@findMethodDirect drawableMethod

    // 21.43 merged the drawable and resource-styled variants into one method that takes
    // the tab object, a boolean, a color and a styling object.
    candidates.singleOrNull { it.paramTypes.size == 4 }
        ?: error("PivotBar create view method not found")
}

object PivotBarButtonsCreateResourceStyledViewFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    parameters = listOf("L", "Z", "I", "L")
)

val pivotBarButtonsViewSetSelectedFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters("I", "Z")
    classMatcher {
        descriptor = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"
    }
    methodMatcher { addInvoke { name = "setSelected" } }
}

val pivotBarButtonsViewSetSelectedSubFingerprint = findMethodDirect {
    pivotBarButtonsViewSetSelectedFingerprint().invokes.single { it.name == "setSelected" }
}

val pivotBarConstructorFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    strings("com.google.android.apps.youtube.app.endpoint.flags")
}

val getNavIconResIdFingerprint = findMethodListDirect {
    // two matches in versions 20.24.xx-20.26.xx,
    // one match in versions <=v20.20.xx and >=v20.28.xx
    val navigationEnumClass = navigationEnumClass()
    findMethod {
        matcher {
            paramTypes(navigationEnumClass.name, "boolean")
            returnType = "int"
        }
    }
}

/**
 * Predictive back gesture callback (Android 13+), which does not call
 * `Activity.onBackPressed()`. Needed so the search-bar closing state is updated
 * on devices that use predictive back.
 *
 * Ported from Morphe v1.45.0:
 * `patches/src/main/kotlin/app/morphe/patches/youtube/misc/backgesture/Fingerprints.kt`
 */
internal object PredictiveGesturesOnBackCancelledFingerprint : Fingerprint(
    name = "onBackCancelled",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        literal(0),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/Object;",
            location = MatchAfterWithin(5)
        ),
        literal(-1, location = MatchAfterWithin(10)),
    )
)

internal object PredictiveGesturesOnBackInvokedFingerprint : Fingerprint(
    classFingerprint = PredictiveGesturesOnBackCancelledFingerprint,
    name = "onBackInvoked"
)

/**
 * Static factory that turns a navigation bar item proto into the view model the app uses.
 *
 * Ported from Morphe's `PivotBarRendererFingerprint`. The A/B layout with icon-only
 * navigation buttons builds its items with this method, and those items are ignored by
 * the rest of the navigation bar patches unless they are converted to labelled items.
 */
internal object PivotBarRendererFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L"),
    returnType = "Lj$/util/Optional;",
    filters = listOf(
        literal(117501096L),
        opcode(Opcode.IF_NE),
        opcode(Opcode.CHECK_CAST),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT_RANGE,
            definingClass = "this",
            name = "<init>",
            returnType = "V"
        ),
        opcode(Opcode.RETURN_OBJECT)
    )
)

/**
 * Animates the navigation bar when it is shown or hidden. The app passes the target
 * visibility and whether to animate; some layouts pass "no animation", which makes the
 * bar (and the glass capsule drawn as its background) jump into place.
 *
 * The method starts one of two [android.animation.ValueAnimator]s with a
 * `PathInterpolator(0.5, 0, 0, 1)`, one for showing and one for hiding.
 */
internal object NavigationBarAnimationFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "Z"),
    filters = listOf(
        methodCall(smali = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;->getVisibility()I"),
        methodCall(smali = "Landroid/animation/ValueAnimator;->ofFloat([F)Landroid/animation/ValueAnimator;"),
        methodCall(smali = "Landroid/animation/ValueAnimator;->ofInt([I)Landroid/animation/ValueAnimator;"),
        methodCall(smali = "Landroid/view/animation/PathInterpolator;-><init>(FFFF)V")
    )
)

/**
 * Every controller that animates the navigation bar show/hide, including the second
 * controller which is only called when the nav bar animation experiment is enabled
 * (every caller checks the experiment flag and calls both controllers in a row).
 *
 * The first controller (also matched by [NavigationBarAnimationFingerprint]) animates the
 * bar layout height. The second controller animates the bar padding and translation
 * instead, and when the experiment is enabled the first controller returns without doing
 * anything, so both have to be forced to animate.
 *
 * Matches exactly two methods on every supported app version (verified offline on
 * 21.38.130, 21.39.522, 21.41.525, 21.42.243, 21.43.523, 21.44.534 and 21.45.82: the
 * `p(ZZ)V` of the first class and the `a(ZZ)V` of the second, nothing else).
 *
 * Skipped by the fingerprint harness because it intentionally matches two methods, which
 * the single-result check of the harness rejects. It is hooked as a list
 * ([PatchExecutor.dexMethodList]), so a future failure degrades to a logged message and
 * no animation forcing instead of a failed patch.
 */
@SkipTest
internal object NavigationBarAnimationAllControllersFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z", "Z"),
    filters = listOf(
        methodCall(smali = "Landroid/animation/ValueAnimator;->ofFloat([F)Landroid/animation/ValueAnimator;"),
        methodCall(smali = "Landroid/animation/ValueAnimator;->setDuration(J)Landroid/animation/ValueAnimator;"),
        methodCall(smali = "Landroid/view/animation/PathInterpolator;-><init>(FFFF)V"),
        methodCall(smali = "Landroid/animation/ValueAnimator;->addUpdateListener(Landroid/animation/ValueAnimator\$AnimatorUpdateListener;)V"),
        methodCall(smali = "Landroid/animation/ValueAnimator;->addListener(Landroid/animation/Animator\$AnimatorListener;)V"),
        methodCall(smali = "Landroid/animation/ValueAnimator;->start()V")
    )
)

/**
 * The pivot bar consumes touches, so a tap on an already selected navigation tab
 * (which closes an on-screen search) can only be detected here.
 */
internal object PivotBarDispatchTouchEventFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
    name = "dispatchTouchEvent",
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;")
)
