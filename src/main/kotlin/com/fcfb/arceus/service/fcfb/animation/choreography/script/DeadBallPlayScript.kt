package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.hold

class DeadBallPlayScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context)
        val spot = BallState(context.offenseSpot(0.3f, 0f))
        return Choreography(scene.offense.map { hold(it) }, scene.defense.map { hold(it) }, BallTrack { spot })
    }
}
