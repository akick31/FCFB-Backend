package com.fcfb.arceus.service.fcfb.animation.choreography

data class Choreography(
    val offense: List<Track>,
    val defense: List<Track>,
    val ball: BallTrack,
    val callout: Callout? = null,
    val facingLocked: Set<Int> = emptySet(),
    val showBall: Boolean = true,
    val endsAt: Float = 1f,
)
