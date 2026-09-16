package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.hold

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
        /** A penalty is shown as the referee alone at midfield, so the camera has nothing else to follow. */
        private const val MIDFIELD = 50f
    }
}
