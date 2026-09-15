package com.fcfb.arceus.service.fcfb.animation.choreography

fun interface BallTrack {
    fun at(progress: Float): BallState
}
