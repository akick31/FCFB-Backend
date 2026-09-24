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
import com.fcfb.arceus.service.fcfb.animation.choreography.bounce
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.hold
import com.fcfb.arceus.service.fcfb.animation.choreography.offsetBy
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.snapBall

class SpikePlayScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context, OffensiveAlignments.underCenter(context.offensivePlaybook))
        val alignment = scene.offensiveAlignment
        val forward = context.forward
        val quarterbackStart = scene.offense[alignment.quarterback]
        val offense = scene.offense.map { hold(it) }
        val defense =
            scene.defense.mapIndexed { index, start ->
                if (index in scene.defensiveAlignment.linemen) {
                    offense[scene.blockerFor(index)].offsetBy(FieldPoint(forward * 2 * OffensiveAlignments.LINE_DEPTH, 0f))
                } else {
                    hold(start)
                }
            }
        val hands = quarterbackStart + carryOffset(forward)
        val spikeSpot = hands + FieldPoint(forward * 1.5f, 0f)
        val restSpot = spikeSpot + FieldPoint(forward * 1.5f, context.side)
        val center = context.offenseSpot(0.3f, 0f)
        val ball =
            BallTrack { progress ->
                when {
                    progress < SNAP_END -> snapBall(progress, center, hands, !alignment.underCenter)
                    progress < SPIKE_AT -> BallState(hands)
                    progress < GROUND_AT -> {
                        val fraction = segment(progress, SPIKE_AT, GROUND_AT)
                        BallState(hands.lerp(spikeSpot, fraction), 1.2f * (1f - fraction))
                    }
                    else -> {
                        val fraction = segment(progress, GROUND_AT, REST_AT)
                        BallState(spikeSpot.lerp(restSpot, fraction), bounce(fraction, 1.2f, 2))
                    }
                }
            }
        return Choreography(offense, defense, ball)
    }

    companion object {
        private const val SPIKE_AT = 0.14f
        private const val GROUND_AT = 0.2f
        private const val REST_AT = 0.45f
    }
}
