package com.fcfb.arceus.rendering.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.model.animation.choreography.BallState
import com.fcfb.arceus.model.animation.choreography.BallTrack
import com.fcfb.arceus.model.animation.choreography.Choreography
import com.fcfb.arceus.model.animation.choreography.FieldPoint
import com.fcfb.arceus.model.animation.choreography.PlayContext
import com.fcfb.arceus.model.animation.choreography.ScrimmageScene
import com.fcfb.arceus.rendering.animation.choreography.PlayScript
import com.fcfb.arceus.rendering.animation.choreography.hold

class DeadBallPlayScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        if (context.play.actualResult == ActualResult.DELAY_OF_GAME) {
            val midfield = BallState(FieldPoint(MIDFIELD, 0f))
            return Choreography(emptyList(), emptyList(), BallTrack { midfield }, showBall = false)
        }
        val scene = ScrimmageScene.from(context)
        val spot = BallState(context.offenseSpot(0.3f, 0f))
        return Choreography(scene.offense.map { hold(it) }, scene.defense.map { hold(it) }, BallTrack { spot })
    }

    companion object {
        private const val MIDFIELD = 50f
    }
}
