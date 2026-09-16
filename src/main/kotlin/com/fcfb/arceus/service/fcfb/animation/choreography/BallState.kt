package com.fcfb.arceus.service.fcfb.animation.choreography

/** [spinning] is a thrown or punted spiral; [tumbling] is end over end, like a kickoff or a loose ball. */
data class BallState(
    val position: FieldPoint,
    val height: Float = 0f,
    val spinning: Boolean = false,
    val tumbling: Boolean = false,
)
