package com.fcfb.arceus.rendering.animation.choreography

import com.fcfb.arceus.model.animation.choreography.Choreography
import com.fcfb.arceus.model.animation.choreography.PlayContext

fun interface PlayScript {
    fun choreograph(context: PlayContext): Choreography
}
