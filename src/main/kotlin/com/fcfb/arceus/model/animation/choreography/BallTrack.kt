package com.fcfb.arceus.model.animation.choreography

fun interface BallTrack {
    fun at(progress: Float): BallState
}
