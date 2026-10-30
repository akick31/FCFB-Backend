package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

data class BackdropArea(
    val width: Int,
    val groundY: Int,
    val topY: Int,
    val scale: Float,
    val netHalfWidth: Int,
    val uprightClearHalfWidth: Int,
    val playSeed: Long = 0L,
    val frontY: Int = groundY,
    val behindWall: Boolean = false,
    val time: Float = 0f,
    val postTopY: Int = topY,
) {
    val centerX: Int get() = width / 2

    val height: Int get() = (groundY - topY).coerceAtLeast(MIN_HEIGHT)

    fun scaled(reference: Int): Int = (reference * scale).toInt().coerceAtLeast(1)

    fun yAt(fractionOfHeight: Float): Int = groundY - (height * fractionOfHeight).toInt()

    val frameTopY: Int get() = groundY - (CLOSE_ROOM * scale).toInt()

    fun atDepth(yards: Float): BackdropArea {
        val closeScale = minOf(1f, REFERENCE_SCALE * (REFERENCE_CAMERA_YARDS + yards) / (CAMERA_YARDS + yards))
        return copy(topY = groundY - (height * closeScale).toInt(), scale = scale * closeScale)
    }

    companion object {
        const val CLOSE_ROOM = 250f
        private const val MIN_HEIGHT = 10
        private const val CAMERA_YARDS = 25f
        private const val REFERENCE_SCALE = 0.74f
        private const val REFERENCE_CAMERA_YARDS = CAMERA_YARDS / REFERENCE_SCALE
    }
}
