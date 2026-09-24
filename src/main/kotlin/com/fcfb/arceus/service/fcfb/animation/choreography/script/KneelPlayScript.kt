package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.OffensiveAlignments
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.SNAP_END
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.hold
import com.fcfb.arceus.service.fcfb.animation.choreography.offsetBy
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.snapBall

class KneelPlayScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context, OffensiveAlignments.victory())
        val alignment = scene.offensiveAlignment
        val forward = context.forward
        val quarterbackStart = scene.offense[alignment.quarterback]
        val kneelSpot = FieldPoint(context.endSpot, 0f) - carryOffset(forward)
        val quarterback = path(0f to quarterbackStart, SNAP_END to quarterbackStart, KNEEL_AT to kneelSpot)
        val offense =
            scene.offense.mapIndexed { index, start ->
                when (index) {
                    in OffensiveAlignments.LINEMEN -> path(0f to start, 0.1f to start, 0.25f to start + FieldPoint(forward * 0.5f, 0f))
                    alignment.quarterback -> quarterback
                    else -> hold(start)
                }
            }
        val defense =
            scene.defense.mapIndexed { index, start ->
                if (index in scene.defensiveAlignment.linemen) {
                    offense[scene.blockerFor(index)].offsetBy(FieldPoint(forward * 2 * OffensiveAlignments.LINE_DEPTH, 0f))
                } else {
                    hold(start)
                }
            }
        val center = context.offenseSpot(0.3f, 0f)
        val ball =
            BallTrack { progress ->
                if (progress < SNAP_END) {
                    snapBall(progress, center, quarterbackStart + carryOffset(forward), !alignment.underCenter)
                } else {
                    BallState(quarterback.at(progress) + carryOffset(forward))
                }
            }
        return Choreography(offense, defense, ball)
    }

    companion object {
        private const val KNEEL_AT = 0.22f
    }
}
