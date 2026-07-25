package io.github.nexalloy.morphe.youtube.misc.playercontrols

import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.RelativeLayout
import app.morphe.extension.shared.ResourceUtils
import app.morphe.extension.shared.Utils
import app.morphe.extension.youtube.patches.LegacyPlayerControlsPatch
import io.github.nexalloy.HookDsl
import io.github.nexalloy.IHookCallback
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.playservice.VersionCheck
import io.github.nexalloy.morphe.youtube.misc.playservice.is_20_31_or_greater
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch
import org.luckypray.dexkit.wrap.DexMethod

class ControlInitializer(
    val id: Int,
    @JvmField val initializeButton: (controlsView: ViewGroup) -> Unit,
)

private data class TopControlLayout(
    val layout: Int,
    val startViewId: Int,
    val endViewId: Int
)

private val topControlLayouts = mutableListOf<TopControlLayout>()
private val bottomControlLayouts = mutableListOf<Int>()
private val topControls = mutableListOf<ControlInitializer>()
private val bottomControls = mutableListOf<ControlInitializer>()

fun addTopControl(layout: Int, startViewId: Int, endViewId: Int) {
    topControlLayouts.add(TopControlLayout(layout, startViewId, endViewId))
}

fun addLegacyBottomControl(layout: Int) {
    bottomControlLayouts.add(layout)
}

fun initializeTopControl(control: ControlInitializer) {
    topControls.add(control)
}

fun initializeLegacyBottomControl(control: ControlInitializer) {
    bottomControls.add(control)
}

private fun onTopContainerInflate(viewStub: ViewStub, root: ViewGroup) {
    topControlLayouts.forEach { control ->
        viewStub.layoutInflater.inflate(control.layout, root, true)
    }

    var insertViewId = ResourceUtils.getIdIdentifier("player_video_heading")
    val anchorViewId = ResourceUtils.getIdIdentifier("music_app_deeplink_button")

    for (control in topControlLayouts) {
        val insertView = root.findViewById<View>(insertViewId) ?: continue
        val endView = root.findViewById<View>(control.endViewId) ?: continue

        (insertView.layoutParams as RelativeLayout.LayoutParams).addRule(
            RelativeLayout.START_OF, control.startViewId
        )

        (endView.layoutParams as RelativeLayout.LayoutParams).addRule(
            RelativeLayout.START_OF, anchorViewId
        )

        insertViewId = control.endViewId
    }

    topControls.forEach { control ->
        control.initializeButton(root)
    }
}

private fun onBottomContainerInflate(viewStub: ViewStub, root: ViewGroup) {
    if (LegacyPlayerControlsPatch.usePlayerBottomControlsExploderLayout(/*ignored*/ true)) {
        return
    }

    bottomControlLayouts.forEach { layout ->
        viewStub.layoutInflater.inflate(layout, root, true)
    }
    bottomControls.forEach { control ->
        control.initializeButton(root)
    }
}

val LegacyPlayerControls = patch(
    description = "Manages the code for the player controls of the YouTube player.",
) {
    dependsOn(
        PlayerControlsOverlayVisibility,
        VersionCheck,
    )

    if (is_20_31_or_greater) {
        PreferenceScreen.PLAYER.addPreferences(
            SwitchPreference("morphe_restore_old_player_buttons", summary = true)
        )
    }

    DexMethod("Landroid/view/ViewStub;->inflate()Landroid/view/View;").hookMethod {
        after {
            val viewStub = it.thisObject as ViewStub
            val viewStubName = Utils.getContext().resources.getResourceName(viewStub.id)
//            Logger.printDebug { "ViewStub->inflate()" + viewStubName }

            when {
                viewStubName.endsWith("bottom_ui_container_stub") -> {
                    onBottomContainerInflate(viewStub, it.result as ViewGroup)
                }

                viewStubName.endsWith("controls_layout_stub") -> {
                    onTopContainerInflate(viewStub, it.result as ViewGroup)
                }

                else -> return@after
            }
//            Logger.printDebug { "inject into $viewStubName" }
        }
    }

    val youtube_controls_bottom_ui_container =
        ResourceUtils.getIdIdentifier("youtube_controls_bottom_ui_container")

    val onLayoutHook: HookDsl<IHookCallback>.() -> Unit = {
        after {
            val controlsView = it.thisObject as ViewGroup
            if (controlsView.id != youtube_controls_bottom_ui_container) return@after

            val fullscreenButton =
                Utils.getChildViewByResourceName<View>(controlsView, "fullscreen_button")
            var rightButton = fullscreenButton

            for (bottomControl in bottomControls) {
                val leftButton = controlsView.findViewById<View>(bottomControl.id) ?: continue
                if (leftButton.visibility == View.GONE) continue
                // put this button to the left
                leftButton.x = rightButton.x - leftButton.width
                leftButton.y = rightButton.y
                leftButton.layoutParams = leftButton.layoutParams.apply {
                    height = fullscreenButton.height
                }
                rightButton = leftButton
            }
        }
    }

    DexMethod("Landroid/support/constraint/ConstraintLayout;->onLayout(ZIIII)V").hookMethod(onLayoutHook)
    DexMethod("Landroidx/constraintlayout/widget/ConstraintLayout;->onLayout(ZIIII)V").hookMethod(onLayoutHook)
}
