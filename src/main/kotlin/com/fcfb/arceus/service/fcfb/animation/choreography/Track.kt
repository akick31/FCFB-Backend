package com.fcfb.arceus.service.fcfb.animation.choreography

fun interface Track {
    fun at(progress: Float): FieldPoint
}
