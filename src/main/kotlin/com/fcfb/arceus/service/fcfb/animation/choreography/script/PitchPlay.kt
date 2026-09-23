package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.DefensiveReaction
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.OffensiveAlignments
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.SNAP_END
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.arc
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.offsetBy
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.snapBall
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

internal object PitchPlay {
    private const val PITCH_AT = 0.2f
    private const val CATCH_AT = 0.28f
    private const val READ_AT = 0.1f
    private const val TACKLE_AT = 0.56f
    private const val TACKLE_SETTLE = 0.05f
    private const val TACKLERS = 3
    private const val QUARTERBACK_OPEN_DEPTH = 1.2f
    private const val QUARTERBACK_OPEN_WIDTH = 1.5f
    private const val CATCH_DEPTH = 6f
    private const val CATCH_WIDTH = 6.5f
    private const val STRING_OUT_WIDTH = 11f
    private const val PITCH_HEIGHT = 1.4f
    private const val REACH_STEP = 1.5f
    private const val LEAD_BLOCK_DEPTH = 2f
    private const val STALK_DEPTH = 3f

    fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context)
        val alignment = scene.offensiveAlignment
        val forward = context.forward
        val side = context.side
        val runnerIndex = alignment.backs.firstOrNull { scene.offense[it].lateral * side > 0f } ?: alignment.backs.first()
        val quarterbackStart = scene.offense[alignment.quarterback]
        val quarterbackDepth = alignment.spots[alignment.quarterback].depth
        val quarterback =
            path(
                0f to quarterbackStart,
                SNAP_END to quarterbackStart,
                PITCH_AT to context.offenseSpot(quarterbackDepth + QUARTERBACK_OPEN_DEPTH, side * QUARTERBACK_OPEN_WIDTH),
            )

        val catchPoint = context.offenseSpot(CATCH_DEPTH, side * CATCH_WIDTH)
        val ballRun = path(CATCH_AT to catchPoint, TACKLE_AT to FieldPoint(context.endSpot, side * STRING_OUT_WIDTH))
        val runnerStart = scene.offense[runnerIndex]
        val runner =
            switchAt(
                CATCH_AT,
                path(0f to runnerStart, SNAP_END to runnerStart, CATCH_AT to catchPoint - carryOffset(-forward)),
                carrierOf(ballRun, -forward),
            )

        val offense =
            scene.offense.mapIndexed { index, start ->
                when {
                    index in OffensiveAlignments.LINEMEN ->
                        path(0f to start, 0.1f to start, 0.4f to start + FieldPoint(forward * 0.4f, side * REACH_STEP))
                    index == alignment.quarterback -> quarterback
                    index == runnerIndex -> runner
                    index in alignment.backs ->
                        path(0f to start, SNAP_END to start, 0.35f to context.offenseSpot(LEAD_BLOCK_DEPTH, side * (CATCH_WIDTH + 2f)))
                    else -> path(0f to start, SNAP_END to start, 0.4f to start + FieldPoint(forward * STALK_DEPTH, 0f))
                }
            }
        val before = DefensiveReaction.before(context, scene, offense, dropping = false)
        val defense = DefensiveReaction.respond(scene, before, runner, -forward, READ_AT, TACKLE_AT, TACKLERS, linemenCanTackle = true)

        val center = context.offenseSpot(0.3f, 0f)
        val hands = quarterback.offsetBy(carryOffset(forward))
        val ball =
            BallTrack { progress ->
                when {
                    progress < SNAP_END -> snapBall(progress, center, hands.at(SNAP_END), !alignment.underCenter)
                    progress < PITCH_AT -> BallState(hands.at(progress))
                    progress < CATCH_AT -> {
                        val fraction = segment(progress, PITCH_AT, CATCH_AT)
                        BallState(hands.at(PITCH_AT).lerp(catchPoint, fraction), arc(fraction, PITCH_HEIGHT), spinning = true)
                    }
                    else -> BallState(ballRun.at(progress))
                }
            }
        return Choreography(offense, defense, ball, endsAt = minOf(1f, TACKLE_AT + TACKLE_SETTLE))
    }
}
