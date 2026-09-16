package com.fcfb.arceus.service.fcfb.animation.choreography

data class Choreography(
    val offense: List<Track>,
    val defense: List<Track>,
    val ball: BallTrack,
    val callout: Callout? = null,
)
