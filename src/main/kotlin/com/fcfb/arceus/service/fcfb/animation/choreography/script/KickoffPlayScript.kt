package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.service.fcfb.animation.KickoffReturnLane
import com.fcfb.arceus.service.fcfb.animation.PlayRandom
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.GangTackle
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.WeavingRun
import com.fcfb.arceus.service.fcfb.animation.choreography.arc
import com.fcfb.arceus.service.fcfb.animation.choreography.bounce
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.hold
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

class KickoffPlayScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val forward = context.forward
        val side = context.side
        val result = context.play.actualResult
        val touchback = context.play.result == Scenario.TOUCHBACK
        val squib = context.play.playCall == PlayCall.KICKOFF_SQUIB
        val muffed = result == ActualResult.MUFFED_KICK || result == ActualResult.KICKING_TEAM_TOUCHDOWN
        val returnScores = result == ActualResult.RETURN_TOUCHDOWN
        val returning = !touchback && !muffed
        val receivingGoal = if (forward > 0f) 100f else 0f
        val returnDirection = -forward

        val lane = PlayRandom(context.play).pick(KickoffReturnLane.entries.toList())

        val kickSpot = FieldPoint(context.lineOfScrimmage, 0f)
        val catchSpot = if (squib) context.defenseSpot(SQUIB_DEPTH, side * 4f) else FieldPoint(receivingGoal - forward, side * 3f)
        val landing = if (touchback) FieldPoint(receivingGoal + forward * END_ZONE_CENTER, side * 2f) else catchSpot
        val endPoint =
            FieldPoint(
                context.endSpot,
                (side * lane.endLateralShare * RETURN_LANE_WIDTH).coerceIn(-MAX_RETURN_LATERAL, MAX_RETURN_LATERAL),
            )
        val catchAt = if (squib) SQUIB_CATCH_AT else CATCH_AT
        val returnAt =
            when {
                !returning -> catchAt
                returnScores -> SCORE_AT
                else -> minOf(RETURN_CAP, catchAt + RETURN_LEAD + RETURN_PER_YARD * minOf(catchSpot.distanceTo(endPoint), 40f) / 40f)
            }
        val returnPace = catchSpot.distanceTo(endPoint) / maxOf(returnAt - catchAt, MIN_RETURN_TIME)
        val coverageSpeed = maxOf(Pursuit.COVERAGE_SPEED, returnPace * COVERAGE_PACE)
        val returnBall =
            if (returning) {
                val weaveSide = if (lane == KickoffReturnLane.CUTBACK) -side else side
                WeavingRun.between(catchSpot, endPoint, catchAt, returnAt, weaveSide, context.lineOfScrimmage)
            } else {
                path(catchAt to catchSpot, returnAt to endPoint)
            }

        val kickingStarts = listOf(context.offenseSpot(KICKER_DEPTH, 0f)) + COVERAGE_LANES.map { context.offenseSpot(1f, it) }
        val receivingStarts =
            FRONT_LINE.map { context.defenseSpot(FRONT_DEPTH, it) } +
                SECOND_LINE.map { context.defenseSpot(SECOND_DEPTH, it) } +
                FieldPoint(receivingGoal - forward * UP_BACK_DEPTH, -side * 4f) +
                (catchSpot - carryOffset(returnDirection))
        val returnerIndex = receivingStarts.lastIndex
        val returner =
            if (returning) {
                switchAt(catchAt, hold(receivingStarts[returnerIndex]), carrierOf(returnBall, returnDirection))
            } else {
                hold(receivingStarts[returnerIndex])
            }

        val coverageTarget = if (touchback) hold(FieldPoint(receivingGoal - forward * TOUCHBACK_STOP, side * 2f)) else returner
        val coverage =
            GangTackle.converge(
                before =
                    kickingStarts.mapIndexed { index, start ->
                        if (index == 0) path(0f to start, KICK_AT to context.offenseSpot(0.6f, 0f)) else hold(start)
                    },
                candidates = if (returning) kickingStarts.indices.drop(1) else emptyList(),
                carrier = coverageTarget,
                direction = returnDirection,
                tackleAt = returnAt,
                tacklers = if (returnScores) 0 else 2,
                reactAt = { index -> if (index == 0) KICK_AT else RELEASE_AT },
                speed = { coverageSpeed },
            )
        val kickingTeam =
            if (muffed) {
                val recoverer = coverage.indices.minBy { coverage[it].at(catchAt).distanceTo(endPoint) }
                coverage.mapIndexed { index, track ->
                    if (index != recoverer) {
                        track
                    } else {
                        switchAt(catchAt, track, path(catchAt to track.at(catchAt), MUFF_RECOVER_AT to endPoint - carryOffset(forward)))
                    }
                }
            } else {
                coverage
            }

        val frontCount = FRONT_LINE.size
        val secondCount = SECOND_LINE.size
        val escortSpeed = returnPace * ESCORT_PACE
        val receivingTeam =
            receivingStarts.mapIndexed { index, start ->
                val blocking = index < frontCount + secondCount
                val wallDepth = if (index < frontCount) FRONT_WALL else SECOND_WALL
                val wallSpread = if (index < frontCount) FRONT_SPREAD else SECOND_SPREAD
                val wallSpot = FieldPoint(receivingGoal - forward * wallDepth, start.lateral * wallSpread)
                val wall =
                    if (blocking) {
                        path(0f to start, 0.1f to start, WALL_AT to wallSpot)
                    } else {
                        path(0f to start, catchAt to start, returnAt to endPoint + FieldPoint(returnDirection * 3f, -side * 2f))
                    }
                when {
                    index == returnerIndex -> returner
                    !returning -> if (blocking) wall else hold(start)
                    else -> Pursuit.chase(wall, catchAt, escortSpeed, Pursuit.trail(returner, ESCORT_RADIUS + (index % 3) * ESCORT_STEP))
                }
            }

        val ball =
            BallTrack { progress ->
                when {
                    progress < KICK_AT -> BallState(kickSpot)
                    progress < catchAt -> {
                        val fraction = segment(progress, KICK_AT, catchAt)
                        val height = if (squib) bounce(fraction, SQUIB_HEIGHT, 3) else arc(fraction, KICK_HEIGHT)
                        BallState(kickSpot.lerp(landing, fraction), height, tumbling = !squib)
                    }
                    touchback -> {
                        val fraction = segment(progress, catchAt, catchAt + SETTLE_TIME)
                        BallState(landing.lerp(landing + FieldPoint(forward * 0.8f, 0f), fraction), bounce(fraction, 1.2f, 2))
                    }
                    muffed -> {
                        val fraction = segment(progress, catchAt, MUFF_RECOVER_AT)
                        BallState(catchSpot.lerp(endPoint, fraction), bounce(fraction, 1.5f, 3))
                    }
                    else -> BallState(returnBall.at(progress))
                }
            }
        val settled =
            when {
                touchback -> minOf(1f, catchAt + SETTLE_TIME)
                muffed -> minOf(1f, MUFF_RECOVER_AT + RETURN_SETTLE)
                else -> minOf(1f, returnAt + RETURN_SETTLE)
            }
        return Choreography(kickingTeam, receivingTeam, ball, endsAt = settled)
    }

    companion object {
        private const val KICK_AT = 0.08f
        private const val RELEASE_AT = 0.06f
        private const val CATCH_AT = 0.4f
        private const val RETURN_CAP = 0.94f
        private const val RETURN_LEAD = 0.08f
        private const val RETURN_PER_YARD = 0.45f
        private const val RETURN_SETTLE = 0.05f
        private const val RETURN_LANE_WIDTH = 13f
        private const val MAX_RETURN_LATERAL = 14f
        private const val SQUIB_CATCH_AT = 0.36f
        private const val MUFF_RECOVER_AT = 0.62f
        private const val SETTLE_TIME = 0.15f
        private const val KICK_HEIGHT = 22f
        private const val SQUIB_HEIGHT = 3f
        private const val KICKER_DEPTH = 7f
        private const val FRONT_DEPTH = 11f
        private const val SECOND_DEPTH = 24f
        private const val SQUIB_DEPTH = 30f
        private const val UP_BACK_DEPTH = 6f
        private const val FRONT_WALL = 25f
        private const val SECOND_WALL = 15f
        private const val FRONT_SPREAD = 0.7f
        private const val SECOND_SPREAD = 0.8f
        private const val WALL_AT = 0.42f
        private const val MIN_RETURN_TIME = 0.05f
        private const val ESCORT_PACE = 0.85f
        private const val COVERAGE_PACE = 0.95f
        private const val ESCORT_RADIUS = 4f
        private const val ESCORT_STEP = 1.5f
        private const val TOUCHBACK_STOP = 20f
        private const val END_ZONE_CENTER = 5f
        private val COVERAGE_LANES = listOf(-22f, -17f, -12f, -7f, -2.5f, 2.5f, 7f, 12f, 17f, 22f)
        private val FRONT_LINE = listOf(-18f, -9f, 0f, 9f, 18f)
        private val SECOND_LINE = listOf(-13f, -4.5f, 4.5f, 13f)
    }
}
