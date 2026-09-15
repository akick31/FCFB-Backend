package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.GangTackle
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.arc
import com.fcfb.arceus.service.fcfb.animation.choreography.bounce
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.hold
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

/** College kickoff: five cover men on each side of the kicker, a five-man front line restrained 10 yards off the ball, returner fielding at the goal line. */
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

        val kickSpot = FieldPoint(context.lineOfScrimmage, 0f)
        val catchSpot = if (squib) context.defenseSpot(SQUIB_DEPTH, side * 4f) else FieldPoint(receivingGoal - forward, side * 3f)
        val landing = if (touchback) FieldPoint(receivingGoal + forward * END_ZONE_CENTER, side * 2f) else catchSpot
        val endPoint = FieldPoint(context.endSpot, side * 5f)
        val catchAt = if (squib) SQUIB_CATCH_AT else CATCH_AT
        val returnAt =
            when {
                !returning -> catchAt
                returnScores -> SCORE_AT
                else -> minOf(0.9f, catchAt + 0.08f + 0.3f * minOf(catchSpot.distanceTo(endPoint), 40f) / 40f)
            }
        val returnBall = path(catchAt to catchSpot, returnAt to endPoint)

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
                candidates = if (returning && !returnScores) kickingStarts.indices.drop(1) else emptyList(),
                carrier = coverageTarget,
                direction = returnDirection,
                tackleAt = returnAt,
                tacklers = 2,
                reactAt = { index -> if (index == 0) KICK_AT else RELEASE_AT },
                speed = { Pursuit.COVERAGE_SPEED },
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
        val receivingTeam =
            receivingStarts.mapIndexed { index, start ->
                when {
                    index == returnerIndex -> returner
                    index < frontCount ->
                        path(0f to start, 0.1f to start, 0.42f to FieldPoint(receivingGoal - forward * FRONT_WALL, start.lateral * 0.7f))
                    index < frontCount + secondCount ->
                        path(0f to start, 0.1f to start, 0.42f to FieldPoint(receivingGoal - forward * SECOND_WALL, start.lateral * 0.8f))
                    returning -> path(0f to start, catchAt to start, returnAt to endPoint + FieldPoint(returnDirection * 3f, -side * 2f))
                    else -> hold(start)
                }
            }

        val ball =
            BallTrack { progress ->
                when {
                    progress < KICK_AT -> BallState(kickSpot)
                    progress < catchAt -> {
                        val fraction = segment(progress, KICK_AT, catchAt)
                        val height = if (squib) bounce(fraction, SQUIB_HEIGHT, 3) else arc(fraction, KICK_HEIGHT)
                        BallState(kickSpot.lerp(landing, fraction), height, spinning = !squib)
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
        return Choreography(kickingTeam, receivingTeam, ball)
    }

    companion object {
        private const val KICK_AT = 0.08f
        private const val RELEASE_AT = 0.06f
        private const val CATCH_AT = 0.45f
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
        private const val TOUCHBACK_STOP = 20f
        private const val END_ZONE_CENTER = 5f
        private val COVERAGE_LANES = listOf(-22f, -17f, -12f, -7f, -2.5f, 2.5f, 7f, 12f, 17f, 22f)
        private val FRONT_LINE = listOf(-18f, -9f, 0f, 9f, 18f)
        private val SECOND_LINE = listOf(-13f, -4.5f, 4.5f, 13f)
    }
}
