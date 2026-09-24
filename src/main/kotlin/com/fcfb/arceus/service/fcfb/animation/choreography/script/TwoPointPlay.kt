package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.TwoPointKind
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

/**
 * The option, read and statue looks for a two point try. All three are quarterback-handled plays
 * from the same three yard line, distinguished by where the ball goes and when: the option pitches
 * wide late, the read keeps up the middle off a mesh, and the statue fakes a throw before handing
 * behind the quarterback's back.
 */
internal object TwoPointPlay {
    fun choreograph(
        context: PlayContext,
        kind: TwoPointKind,
    ): Choreography {
        val scene = ScrimmageScene.from(context)
        val alignment = scene.offensiveAlignment
        val forward = context.forward
        val side = context.side
        val exchangeAt = exchangeAt(kind)
        val carrierIndex =
            when (kind) {
                TwoPointKind.READ -> alignment.quarterback
                else -> alignment.backs.firstOrNull { scene.offense[it].lateral * side >= 0f } ?: alignment.backs.first()
            }

        val quarterbackStart = scene.offense[alignment.quarterback]
        val quarterbackDepth = alignment.spots[alignment.quarterback].depth
        val mesh = context.offenseSpot(quarterbackDepth, side * MESH_WIDTH)
        val endPoint = FieldPoint(context.endSpot, side * endWidth(kind))
        val carryFrom = carryPoint(context, kind, side)

        val quarterback =
            if (kind == TwoPointKind.READ) {
                path(0f to quarterbackStart, SNAP_END to quarterbackStart, exchangeAt to mesh)
            } else {
                path(
                    0f to quarterbackStart,
                    SNAP_END to quarterbackStart,
                    exchangeAt to mesh,
                    SETTLE_AT to mesh - FieldPoint(forward * QUARTERBACK_DRIFT, 0f),
                )
            }

        val ballRun = path(exchangeAt to carryFrom, SCORE_LINE_AT to endPoint)
        val carrierStart = scene.offense[carrierIndex]
        val carrier =
            if (kind == TwoPointKind.READ) {
                switchAt(exchangeAt, quarterback, carrierOf(ballRun, forward))
            } else {
                switchAt(
                    exchangeAt,
                    path(0f to carrierStart, SNAP_END to carrierStart, exchangeAt to carryFrom - carryOffset(forward)),
                    carrierOf(ballRun, forward),
                )
            }

        val offense =
            scene.offense.mapIndexed { index, start ->
                when {
                    index == carrierIndex -> carrier
                    index == alignment.quarterback -> quarterback
                    index in OffensiveAlignments.LINEMEN ->
                        path(0f to start, 0.1f to start, BLOCK_AT to start + FieldPoint(forward * DRIVE_BLOCK, 0f))
                    index in alignment.backs ->
                        path(0f to start, SNAP_END to start, BLOCK_AT to context.offenseSpot(LEAD_DEPTH, side * LEAD_WIDTH))
                    else -> path(0f to start, SNAP_END to start, BLOCK_AT to start + FieldPoint(forward * STALK_DEPTH, 0f))
                }
            }

        val before = DefensiveReaction.before(context, scene, offense, dropping = false)
        val defense =
            DefensiveReaction.respond(scene, before, carrier, forward, READ_AT, SCORE_LINE_AT, TACKLERS, linemenCanTackle = true)

        val center = context.offenseSpot(0.3f, 0f)
        val hands = quarterback.offsetBy(carryOffset(forward))
        val pitched = kind == TwoPointKind.OPTION
        val ball =
            BallTrack { progress ->
                when {
                    progress < SNAP_END -> snapBall(progress, center, hands.at(SNAP_END), !alignment.underCenter)
                    progress < exchangeAt -> BallState(hands.at(progress))
                    pitched && progress < exchangeAt + PITCH_TIME -> {
                        val fraction = segment(progress, exchangeAt, exchangeAt + PITCH_TIME)
                        BallState(hands.at(exchangeAt).lerp(carryFrom, fraction), arc(fraction, PITCH_HEIGHT), spinning = true)
                    }
                    else -> BallState(ballRun.at(progress))
                }
            }
        return Choreography(offense, defense, ball, endsAt = minOf(1f, SCORE_LINE_AT + SETTLE))
    }

    private fun exchangeAt(kind: TwoPointKind): Float =
        when (kind) {
            TwoPointKind.STATUE -> STATUE_EXCHANGE
            TwoPointKind.OPTION -> OPTION_EXCHANGE
            else -> MESH_EXCHANGE
        }

    private fun endWidth(kind: TwoPointKind): Float =
        when (kind) {
            TwoPointKind.OPTION -> OPTION_END_WIDTH
            TwoPointKind.STATUE -> STATUE_END_WIDTH
            else -> READ_END_WIDTH
        }

    private fun carryPoint(
        context: PlayContext,
        kind: TwoPointKind,
        side: Float,
    ): FieldPoint =
        when (kind) {
            TwoPointKind.OPTION -> context.offenseSpot(OPTION_CATCH_DEPTH, side * OPTION_CATCH_WIDTH)
            TwoPointKind.STATUE -> context.offenseSpot(STATUE_CARRY_DEPTH, -side * STATUE_CARRY_WIDTH)
            else -> context.offenseSpot(READ_CARRY_DEPTH, side * READ_CARRY_WIDTH)
        }

    private const val MESH_EXCHANGE = 0.18f
    private const val OPTION_EXCHANGE = 0.24f
    private const val STATUE_EXCHANGE = 0.3f
    private const val PITCH_TIME = 0.06f
    private const val PITCH_HEIGHT = 1.3f
    private const val SCORE_LINE_AT = 0.62f
    private const val SETTLE = 0.05f
    private const val READ_AT = 0.12f
    private const val BLOCK_AT = 0.45f
    private const val SETTLE_AT = 0.5f
    private const val TACKLERS = 3
    private const val MESH_WIDTH = 1.2f
    private const val QUARTERBACK_DRIFT = 1.5f
    private const val DRIVE_BLOCK = 1.6f
    private const val LEAD_DEPTH = 1.5f
    private const val LEAD_WIDTH = 5f
    private const val STALK_DEPTH = 2.5f
    private const val OPTION_CATCH_DEPTH = 4.5f
    private const val OPTION_CATCH_WIDTH = 6f
    private const val OPTION_END_WIDTH = 9f
    private const val STATUE_CARRY_DEPTH = 5.5f
    private const val STATUE_CARRY_WIDTH = 2.5f
    private const val STATUE_END_WIDTH = 6f
    private const val READ_CARRY_DEPTH = 2.5f
    private const val READ_CARRY_WIDTH = 1.5f
    private const val READ_END_WIDTH = 2f
}
