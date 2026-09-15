package com.fcfb.arceus.service.fcfb.animation.choreography

fun interface PlayScript {
    fun choreograph(context: PlayContext): Choreography
}
