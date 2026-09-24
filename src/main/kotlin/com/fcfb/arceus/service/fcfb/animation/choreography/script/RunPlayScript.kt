package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.service.fcfb.animation.PlayRandom
import com.fcfb.arceus.service.fcfb.animation.RunConcept
import com.fcfb.arceus.service.fcfb.animation.TwoPointKind
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.DefensiveReaction
import com.fcfb.arceus.service.fcfb.animation.choreography.DownfieldEscort
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.OffensiveAlignments
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.SNAP_END
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.WeavingRun
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.carryTime
import com.fcfb.arceus.service.fcfb.animation.choreography.hold
import com.fcfb.arceus.service.fcfb.animation.choreography.offsetBy
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.snapBall
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt
import kotlin.math.abs

class RunPlayScript : PlayScript {
    private val twoPointPass = CompletedPassScript()

    override fun choreograph(context: PlayContext): Choreography {
        if (context.play.actualResult == ActualResult.SAFETY) return PitchPlay.choreograph(context)
        val random = PlayRandom(context.play)
        if (context.play.playCall == PlayCall.TWO_POINT) {
            if (random.chance(TWO_POINT_PASS_CHANCE)) return twoPointPass.choreograph(context)
            val kind = random.pick(TwoPointKind.entries.toList())
            if (kind != TwoPointKind.POWER && kind != TwoPointKind.FADE) return TwoPointPlay.choreograph(context, kind)
        }
        val scene = ScrimmageScene.from(context)
        val alignment = scene.offensiveAlignment
        val forward = context.forward
        val side = context.side
        val result = context.play.actualResult
        val fumble = result in FUMBLES
        val offenseScores = result in OFFENSIVE_SCORES

        // PitchPlay was written for losses, so a scoring or fumbled pitch falls back to a straight run rather than
        // being routed through a path that has never been exercised at that outcome.
        val drawn = random.pick(RunConcept.entries.toList())
        val concept = if ((offenseScores || fumble) && drawn == RunConcept.PITCH) RunConcept.POWER else drawn
        if (concept == RunConcept.PITCH) return PitchPlay.choreograph(context)
        val carrierSide = if (concept == RunConcept.REVERSE) -side else side
        val runnerIndex =
            if (concept.inMotion) {
                alignment.receivers.firstOrNull { scene.offense[it].lateral * -carrierSide > 0f } ?: alignment.backs.first()
            } else {
                alignment.backs.firstOrNull { scene.offense[it].lateral * side > 0f } ?: alignment.backs.first()
            }
        val quarterbackStart = scene.offense[alignment.quarterback]
        val meshDepth = if (alignment.underCenter) UNDER_CENTER_MESH_DEPTH else alignment.spots[alignment.quarterback].depth
        val meshWidth =
            when (concept) {
                RunConcept.REVERSE -> side * REVERSE_MESH_WIDTH
                RunConcept.JET_SWEEP -> carrierSide * JET_MESH_WIDTH
                else -> carrierSide * MESH_WIDTH
            }
        val mesh = context.offenseSpot(meshDepth, meshWidth)
        val quarterback =
            path(
                0f to quarterbackStart,
                SNAP_END to quarterbackStart,
                HANDOFF to mesh + FieldPoint(0f, -side * QUARTERBACK_MESH_GAP),
                FAKE_END to context.offenseSpot(meshDepth + FAKE_DEPTH, -side * FAKE_WIDTH),
            )

        val driftWidth =
            when (concept) {
                RunConcept.OUTSIDE -> OUTSIDE_DRIFT
                RunConcept.REVERSE -> REVERSE_DRIFT
                RunConcept.JET_SWEEP -> JET_DRIFT
                else -> minOf(DRIFT_BASE + minOf(abs(context.gain), 12f) * DRIFT_PER_YARD, MAX_DRIFT)
            }
        val drift = carrierSide * driftWidth
        val runEnd = if (fumble) FumbleRecovery.fumbleSpot(context, drift) else FieldPoint(context.endSpot, drift)
        val runGain = (runEnd.along - context.lineOfScrimmage) * forward
        val runTime =
            if (runGain <= 0f) {
                // A reverse crosses the formation, so a flat stuffed time would sprint the carrier sideways.
                minOf(SCORE_AT, maxOf(STUFFED_AT, HANDOFF + STRIDE_TIME + carryTime(mesh.distanceTo(runEnd))))
            } else {
                minOf(SCORE_AT, maxOf(SHORT_RUN_AT, HANDOFF + STRIDE_TIME + carryTime(runGain)))
            }
        val tackleAt =
            when {
                offenseScores -> SCORE_AT
                fumble -> minOf(maxOf(runTime, HANDOFF + carryTime(runGain) * FUMBLE_PURSUIT_TIME), FumbleRecovery.latestFumbleAt(context))
                else -> runTime
            }

        val handoffBall = mesh + carryOffset(forward)
        val holeWidth = if (concept == RunConcept.OUTSIDE) OUTSIDE_HOLE_WIDTH else HOLE_WIDTH
        val hole = context.defenseSpot(HOLE_DEPTH, carrierSide * holeWidth)
        val ballRun =
            if (runGain > 1f) {
                val toHole = handoffBall.distanceTo(hole)
                val holeAt = HANDOFF + (tackleAt - HANDOFF) * toHole / (toHole + hole.distanceTo(runEnd))
                val deepest = scene.defense.maxOf { (it.along - context.lineOfScrimmage) * forward }
                val clearAlong = context.lineOfScrimmage + forward * (deepest + CLEAR_MARGIN)
                val toDaylight = WeavingRun.between(hole, runEnd, holeAt, tackleAt, side, clearAlong, scene.defense.map { hold(it) })
                switchAt(holeAt, path(HANDOFF to handoffBall, holeAt to hole), toDaylight)
            } else {
                path(HANDOFF to handoffBall, tackleAt to runEnd)
            }
        val runnerStart = scene.offense[runnerIndex]
        // A motion man is already crossing at the snap, the way a real reverse or jet sweep starts.
        val runnerPath =
            if (concept.inMotion) {
                path(0f to runnerStart, MOTION_START to runnerStart, SNAP_END to runnerStart.lerp(mesh, MOTION_SHARE), HANDOFF to mesh)
            } else {
                path(0f to runnerStart, SNAP_END to runnerStart, HANDOFF to mesh)
            }
        val runner = switchAt(HANDOFF, runnerPath, carrierOf(ballRun, forward))

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

        val runPace = abs(runGain) / maxOf(tackleAt - HANDOFF, MIN_RUN_TIME)
        val escortSpeed = Pursuit.paced(maxOf(Pursuit.DEFENSIVE_BACK_SPEED, runPace * ESCORT_PACE))
        val escorted =
            DownfieldEscort.follow(
                before = offense,
                carrier = runner,
                from = if (offenseScores) SNAP_END else ESCORT_FROM,
                until = tackleAt,
                exclude = setOf(runnerIndex, alignment.quarterback),
                speed = { if (it in OffensiveAlignments.LINEMEN) Pursuit.LINEMAN_SPEED else escortSpeed },
            )
        val tacklers =
            when {
                offenseScores -> 0
                runGain <= 3f -> 3
                else -> 2
            }
        val before = DefensiveReaction.before(context, scene, escorted, dropping = false)
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
        if (!fumble) return Choreography(escorted, defense, carried, endsAt = minOf(1f, tackleAt + TACKLE_SETTLE))
        return FumbleRecovery.choreograph(context, escorted, defense, carried, tackleAt, runEnd)
    }

    companion object {
        private const val HANDOFF = 0.2f
        private const val ESCORT_FROM = 0.3f
        private const val TWO_POINT_PASS_CHANCE = 0.5f
        private const val MIN_RUN_TIME = 0.05f
        private const val CLEAR_MARGIN = 5f
        private const val ESCORT_PACE = 0.9f
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
        private const val TACKLE_SETTLE = 0.05f
        private const val FUMBLE_PURSUIT_TIME = 1.8f
        private const val OUTSIDE_DRIFT = 13f
        private const val OUTSIDE_HOLE_WIDTH = 11f
        private const val REVERSE_MESH_WIDTH = 4f
        private const val REVERSE_DRIFT = 10f
        private const val JET_MESH_WIDTH = 3f
        private const val JET_DRIFT = 8f
        private const val MOTION_START = 0.02f
        private const val MOTION_SHARE = 0.45f
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
