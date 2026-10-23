package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea

class FacilityFrame(
    val area: BackdropArea,
    val footY: Int,
) {
    fun x(reference: Int): Int = area.centerX + (reference * area.scale).toInt()

    fun y(reference: Int): Int = footY - (reference * area.scale).toInt()

    fun length(reference: Int): Int = (reference * area.scale).toInt().coerceAtLeast(1)

    fun leftEdgeReference(): Int = (-area.centerX / area.scale).toInt() - 1

    fun rightEdgeReference(): Int = ((area.width - area.centerX) / area.scale).toInt() + 1
}
