package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.DefensiveReaction
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import kotlin.math.abs

class SackScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context)
        val concept = PassConcept(context, scene)
        val forward = context.forward
        val sackSpot = FieldPoint(context.endSpot, context.side) - carryOffset(forward)
        val quarterback = concept.quarterback(HOLD_BALL_UNTIL to concept.setPoint, SACK_AT to sackSpot)
        val offense = concept.offense(quarterback, emptyMap())
        val before = DefensiveReaction.before(context, scene, offense, dropping = true)
        val rushers = scene.defensiveAlignment.linemen.sortedByDescending { abs(scene.defense[it].lateral) }.take(RUSHER_COUNT)
        val defense =
            before.mapIndexed { index, track ->
                val slot = rushers.indexOf(index)
                if (slot >= 0) {
                    Pursuit.chase(track, BEAT_BLOCK_AT, EDGE_RUSH_SPEED, Pursuit.tackle(quarterback, forward, slot, SACK_AT))
                } else {
                    track
                }
            }
        return Choreography(offense, defense, concept.heldBall(quarterback))
    }

    companion object {
        private const val HOLD_BALL_UNTIL = 0.42f
        private const val SACK_AT = 0.62f
        private const val BEAT_BLOCK_AT = 0.25f
        private const val EDGE_RUSH_SPEED = 26f
        private const val RUSHER_COUNT = 2
    }
}
