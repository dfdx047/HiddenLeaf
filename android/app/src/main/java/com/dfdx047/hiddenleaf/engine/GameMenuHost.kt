package com.dfdx047.hiddenleaf.engine

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * The game menu ([GameMenu]) over the game's surface. SDL's activity is not one of AndroidX's, so the menu brings
 * the lifecycle Compose asks for of its own. The view stays in place between openings (gone while closed, so the
 * touches reach the on-screen controls).
 */
class GameMenuHost(context: Context, val state: GameMenuState, actions: GameMenuActions) : FrameLayout(context) {
    private val owner = MenuOwner()
    private val compose = ComposeView(context).apply { setContent { GameMenu(state, actions) } }

    init {
        setViewTreeLifecycleOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
        addView(compose, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        visibility = GONE
    }

    /** Open (the drawer on screen or the layout editor in its place). */
    val isOpen: Boolean get() = state.open

    /** The drawer is on screen: the controller's buttons are the menu's. */
    val isShowing: Boolean get() = state.open && visibility == VISIBLE

    fun show(byPad: Boolean) {
        removeCallbacks(hideLater)
        visibility = VISIBLE
        state.padNav = byPad
        state.open = true
        state.focusTick++
        compose.requestFocus()
    }

    fun hide() {
        state.open = false
        state.capture = null
        state.overlay = null
        state.confirmQuit = false
        postDelayed(hideLater, 230) // (after the drawer's slide)
    }

    /** The layout editor takes the screen: the drawer goes, the menu stays open behind it. */
    fun stepAside() {
        state.capture = null
        visibility = GONE
    }

    /** Back from the layout editor. */
    fun comeBack() {
        visibility = VISIBLE
        state.focusTick++
        compose.requestFocus()
    }

    /** The keys reach the menu's page (the focus may have gone to the game's surface). */
    fun ensureFocus() {
        if (!compose.hasFocus()) compose.requestFocus()
    }

    private val hideLater = Runnable { if (!state.open) visibility = GONE }

    private class MenuOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

        init {
            saved.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }
    }
}
