package com.apps.unsealed.ui.screens.penpals.state

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.MutableFloatState

enum class PenPalsViewMode { LooseDesk, Grid }

fun defaultPenPalsViewMode(): PenPalsViewMode =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PenPalsViewMode.LooseDesk else PenPalsViewMode.Grid

/**
 * Per-card desk position/orientation state. [zIndex] is a field so grabbing one card
 * only recomposes that card, not every reader of a shared map.
 */
class DeskCardState(
    val offsetX: Animatable<Float, AnimationVector1D>,
    val offsetY: Animatable<Float, AnimationVector1D>,
    val rotationZ: Float,
    val zIndex: MutableFloatState,
)
