package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.DefensiveReaction
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.OffensiveAlignments
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.SNAP_END
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.carryTime
import com.fcfb.arceus.service.fcfb.animation.choreography.offsetBy
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.snapBall
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt
import kotlin.math.abs

class RunPlayScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        if (context.play.actualResult == ActualResult.SAFETY) return PitchPlay.choreograph(context)
        val scene = ScrimmageScene.from(context)
        val alignment = scene.offensiveAlignment
        val forward = context.forward
        val side = context.side
        val result = context.play.actualResult
        val fumble = result in FUMBLES
        val offenseScores = result in OFFENSIVE_SCORES

        val runnerIndex = alignment.backs.firstOrNull { scene.offense[it].lateral * side > 0f } ?: alignment.backs.first()
        val quarterbackStart = scene.offense[alignment.quarterback]
        val meshDepth = if (alignment.underCenter) UNDER_CENTER_MESH_DEPTH else alignment.spots[alignment.quarterback].depth
        val mesh = context.offenseSpot(meshDepth, side * MESH_WIDTH)
        val quarterback =
            path(
                0f to quarterbackStart,
                SNAP_END to quarterbackStart,
                HANDOFF to mesh + FieldPoint(0f, -side * QUARTERBACK_MESH_GAP),
                FAKE_END to context.offenseSpot(meshDepth + FAKE_DEPTH, -side * FAKE_WIDTH),
            )

        val drift = side * minOf(DRIFT_BASE + minOf(abs(context.gain), 12f) * DRIFT_PER_YARD, MAX_DRIFT)
        val runEnd = if (fumble) FumbleRecovery.fumbleSpot(context, drift) else FieldPoint(context.endSpot, drift)
        val runGain = (runEnd.along - context.lineOfScrimmage) * forward
        val runTime = if (runGain <= 0f) STUFFED_AT else minOf(SCORE_AT, maxOf(SHORT_RUN_AT, HANDOFF + STRIDE_TIME + carryTime(runGain)))
        val tackleAt =
            when {
                offenseScores -> SCORE_AT
                fumble -> minOf(runTime, FumbleRecovery.latestFumbleAt(context))
                else -> runTime
            }

        val handoffBall = mesh + carryOffset(forward)
        val hole = context.defenseSpot(HOLE_DEPTH, side * HOLE_WIDTH)
        val ballRun =
            if (runGain > 1f) {
                val toHole = handoffBall.distanceTo(hole)
                val holeAt = HANDOFF + (tackleAt - HANDOFF) * toHole / (toHole + hole.distanceTo(runEnd))
                path(HANDOFF to handoffBall, holeAt to hole, tackleAt to runEnd)
            } else {
                path(HANDOFF to handoffBall, tackleAt to runEnd)
            }
        val runnerStart = scene.offense[runnerIndex]
        val runner = switchAt(HANDOFF, path(0f to runnerStart, SNAP_END to runnerStart, HANDOFF to mesh), carrierOf(ballRun, forward))

        val drive = if (runGain > 0f) DRIVE_BLOCK else -DRIVE_BLOCK * 0.6f
        val offense =
            scene.offense.mapIndexed { index, start ->
                when {
                    index in OffensiveAlignments.LINEMEN ->
                        path(
                            0f to start,
                            0.1f to start,
                            0.45f to start + FieldPoint(forward * drive, 0f),
                        )
                    index == alignment.quarterback -> quarterback
                    index == runnerIndex -> runner
                    index in alignment.backs -> path(0f to start, 0.12f to start, 0.42f to context.defenseSpot(2f, side * 4.5f))
                    abs(start.lateral) < 8f -> path(0f to start, 0.1f to start, 0.45f to start + FieldPoint(forward * 1.5f, 0f))
                    else -> path(0f to start, SNAP_END to start, 0.45f to start + FieldPoint(forward * 6f, 0f))
                }
            }

        val tacklers =
            when {
                offenseScores -> 0
                runGain <= 3f -> 3
                else -> 2
            }
        val before = DefensiveReaction.before(context, scene, offense, dropping = false)
        val defense =
            DefensiveReaction.respond(
                scene,
                before,
                runner,
                forward,
                READ_AT,
                tackleAt,
                tacklers,
                linemenCanTackle = runGain <= 2f,
            )

        val center = context.offenseSpot(0.3f, 0f)
        val quarterbackHands = quarterback.offsetBy(carryOffset(forward))
        val carried =
            BallTrack { progress ->
                when {
                    progress < SNAP_END -> snapBall(progress, center, quarterbackHands.at(SNAP_END), !alignment.underCenter)
                    progress < HANDOFF -> BallState(quarterbackHands.at(progress))
                    else -> BallState(ballRun.at(progress))
                }
            }
        if (!fumble) return Choreography(offense, defense, carried)
        return FumbleRecovery.choreograph(context, offense, defense, carried, tackleAt, runEnd)
    }

    companion object {
        private const val HANDOFF = 0.2f
        private const val FAKE_END = 0.45f
        private const val READ_AT = 0.22f
        private const val STUFFED_AT = 0.5f
        private const val SHORT_RUN_AT = 0.55f
        private const val STRIDE_TIME = 0.12f
        private const val UNDER_CENTER_MESH_DEPTH = 4f
        private const val MESH_WIDTH = 1.2f
        private const val QUARTERBACK_MESH_GAP = 1.6f
        private const val FAKE_DEPTH = 1.5f
        private const val FAKE_WIDTH = 4f
        private const val HOLE_DEPTH = 0.8f
        private const val HOLE_WIDTH = 3f
        private const val DRIFT_BASE = 2.5f
        private const val DRIFT_PER_YARD = 0.35f
        private const val MAX_DRIFT = 18f
        private const val DRIVE_BLOCK = 2f

        private val FUMBLES = setOf(ActualResult.TURNOVER, ActualResult.TURNOVER_TOUCHDOWN, ActualResult.DEFENSE_TWO_POINT)
        private val OFFENSIVE_SCORES = setOf(ActualResult.TOUCHDOWN, ActualResult.SUCCESS)
    }
}
