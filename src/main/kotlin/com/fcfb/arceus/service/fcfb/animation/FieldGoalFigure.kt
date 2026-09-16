package com.fcfb.arceus.service.fcfb.animation

/** [facingCamera] is true for the rushers, who line up facing the kicker and the camera behind him. */
data class FieldGoalFigure(
    val x: Float,
    val footY: Float,
    val scale: Float,
    val uniform: Uniform,
    val number: Int,
    val pose: PlayerPose,
    val facingCamera: Boolean = false,
)
