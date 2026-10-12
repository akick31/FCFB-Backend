package com.fcfb.arceus.model.animation.choreography

data class BallState(
    val position: FieldPoint,
    val height: Float = 0f,
    val spinning: Boolean = false,
    val tumbling: Boolean = false,
)
