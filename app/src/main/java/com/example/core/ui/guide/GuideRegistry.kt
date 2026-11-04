@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.core.ui.guide

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

class GuideRegistry {

    companion object {
        val Default = GuideRegistry()
    }

    private val _targets = mutableStateMapOf<String, LayoutCoordinates>()
    val targets: Map<String, LayoutCoordinates> get() = _targets

    var version by mutableIntStateOf(0)
        private set

    fun register(id: String, coordinates: LayoutCoordinates) {
        if (coordinates.isAttached && coordinates.size.width > 0 && coordinates.size.height > 0) {
            _targets[id] = coordinates
            version++
        }
    }

    fun unregister(id: String) {
        _requesters.remove(id)
        if (_targets.remove(id) != null) {
            version++
        }
    }

    private val _requesters = mutableMapOf<String, BringIntoViewRequester>()

    fun registerRequester(id: String, requester: BringIntoViewRequester) {
        _requesters[id] = requester
    }

    /** Scrolls the target into view inside its scroll container (no-op if it is not scrollable). */
    suspend fun bringIntoView(id: String) {
        try {
            _requesters[id]?.bringIntoView()
        } catch (_: Exception) {
        }
    }

    /**
     * Returns the target's bounding box relative to [containerCoordinates].
     * If [containerCoordinates] is null, returns coordinates in window.
     */
    fun getTargetBounds(id: String, containerCoordinates: LayoutCoordinates?): Rect? {
        // Read version to ensure observation upon layout changes
        @Suppress("UNUSED_VARIABLE")
        val v = version
        val coords = _targets[id] ?: return null
        if (!coords.isAttached || coords.size.width <= 0 || coords.size.height <= 0) {
            return null
        }
        return try {
            if (containerCoordinates != null && containerCoordinates.isAttached) {
                // Screen coordinates: the overlay (a popup) and the target (main window or a
                // bottom sheet) live in different windows, so window-local math does not apply.
                val origin = containerCoordinates.localToScreen(Offset.Zero)
                val topLeft = coords.localToScreen(Offset.Zero) - origin
                Rect(topLeft, Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
            } else {
                coords.boundsInWindow()
            }
        } catch (_: Exception) {
            null
        }
    }
}

val LocalGuideRegistry = staticCompositionLocalOf { GuideRegistry.Default }

/**
 * Modifier to register an element as a guide tour target.
 */
fun Modifier.guideTarget(id: String, enabled: Boolean = true): Modifier = if (!enabled) this else composed {
    val registry = LocalGuideRegistry.current
    val requester = remember { BringIntoViewRequester() }
    DisposableEffect(id, registry) {
        registry.registerRequester(id, requester)
        onDispose {
            registry.unregister(id)
        }
    }
    this
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { coordinates ->
            registry.register(id, coordinates)
        }
}
