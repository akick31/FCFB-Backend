package com.fcfb.arceus.service.fcfb.animation

data class FieldGoalFigure(
    val x: Float,
    val footY: Float,
    val scale: Float,
    val uniform: Uniform,
    val number: Int,
    val pose: PlayerPose,
    val facingCamera: Boolean = false,
)
