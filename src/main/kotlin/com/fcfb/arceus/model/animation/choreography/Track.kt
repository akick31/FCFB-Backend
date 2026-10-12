package com.fcfb.arceus.model.animation.choreography

fun interface Track {
    fun at(progress: Float): FieldPoint
}
