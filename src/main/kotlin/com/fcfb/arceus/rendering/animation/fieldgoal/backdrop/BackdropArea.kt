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

    /** Scenery [yards] behind the end zone shrinks less than the posts as the camera closes in, like a real dolly shot. */
    fun atDepth(yards: Float): BackdropArea {
        val layerScale = REFERENCE_SCALE * (REFERENCE_CAMERA_YARDS + yards) / (CAMERA_YARDS / scale + yards)
        val canvas = (height * minOf(1f, layerScale / scale)).toInt()
        return copy(topY = groundY - canvas, scale = minOf(scale, layerScale))
    }

    companion object {
        private const val MIN_HEIGHT = 10
        private const val CAMERA_YARDS = 25f
        private const val REFERENCE_SCALE = 0.74f
        private const val REFERENCE_CAMERA_YARDS = CAMERA_YARDS / REFERENCE_SCALE
    }
}
