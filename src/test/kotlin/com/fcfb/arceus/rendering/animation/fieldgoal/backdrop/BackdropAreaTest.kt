package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackdropAreaTest {
    private fun area(scale: Float) =
        BackdropArea(
            width = 800,
            groundY = 250,
            topY = 250 - (BackdropArea.CLOSE_ROOM * scale).toInt(),
            scale = scale,
            netHalfWidth = 0,
            uprightClearHalfWidth = 0,
        )

    @Test
    fun `distant layers shrink by the same zoom as the posts`() {
        val close = area(1f).atDepth(90f)
        val long = area(0.45f).atDepth(90f)

        assertEquals(0.45f, long.scale / close.scale, 0.01f)
        assertEquals(0.45f, long.height.toFloat() / close.height, 0.02f)
    }

    @Test
    fun `the virtual frame top sits one zoomed close-view room above the ground`() {
        assertEquals(250 - 112, area(0.45f).frameTopY)
        assertEquals(0, area(1f).frameTopY)
    }
}
