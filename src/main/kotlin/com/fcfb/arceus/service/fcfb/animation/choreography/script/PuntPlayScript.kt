package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.DownfieldEscort
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
import com.fcfb.arceus.service.fcfb.animation.choreography.offsetBy
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt
import kotlin.math.abs

class PuntPlayScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val forward = context.forward
        val side = context.side
        val result = context.play.actualResult
        val returnScores = result == ActualResult.PUNT_RETURN_TOUCHDOWN
        val muffed = result == ActualResult.MUFFED_PUNT || result == ActualResult.PUNT_TEAM_TOUCHDOWN
        val returnDirection = -forward
        val endPoint = FieldPoint(context.endSpot, side * 3f)
        val landing = landingSpot(context, endPoint, returnScores, muffed)
        val fairCatch = !muffed && !returnScores && abs(landing.along - endPoint.along) < 0.5f

        val kickingStarts = PuntFormation.kickingTeam(PuntFormation.PUNT_DEPTH).map { context.offenseSpot(it.depth, it.lateral) }
        val returnStarts =
            (PuntFormation.RUSHERS + PuntFormation.JAMMERS + PuntFormation.MIDDLE).map { context.defenseSpot(it.depth, it.lateral) } +
                (landing - carryOffset(returnDirection))
        val returnerIndex = returnStarts.lastIndex

        val returnAt =
            when {
                returnScores -> SCORE_AT
                fairCatch || muffed -> CATCH_AT
                else -> minOf(0.9f, CATCH_AT + 0.08f + 0.3f * minOf(landing.distanceTo(endPoint), 25f) / 25f)
            }
        val returnBall = path(CATCH_AT to landing, returnAt to endPoint)
        val returner =
            if (muffed) {
                hold(returnStarts[returnerIndex])
            } else {
                switchAt(CATCH_AT, hold(returnStarts[returnerIndex]), carrierOf(returnBall, returnDirection))
            }

        val punterStart = kickingStarts[PuntFormation.KICKER]
        val punter = path(0f to punterStart, SNAP_AT to punterStart, KICK_AT to punterStart + FieldPoint(forward * 1.5f, 0f))
        val coverage =
            GangTackle.converge(
                before = kickingStarts.mapIndexed { index, start -> if (index == PuntFormation.KICKER) punter else hold(start) },
                candidates =
                    if (muffed || fairCatch) emptyList() else PuntFormation.GUNNERS + PuntFormation.PERSONAL_PROTECTOR,
                carrier = returner,
                direction = returnDirection,
                tackleAt = returnAt,
                tacklers = if (returnScores) 0 else 2,
                reactAt = { index -> if (index in PuntFormation.GUNNERS) GUNNER_RELEASE else KICK_AT },
                speed = { index -> if (index == PuntFormation.KICKER) PUNTER_SPEED else Pursuit.COVERAGE_SPEED },
            )
        val kickingTeam =
            if (muffed) {
                val recoverer = coverage.indices.minBy { coverage[it].at(CATCH_AT).distanceTo(endPoint) }
                coverage.mapIndexed { index, track ->
                    if (index != recoverer) {
                        track
                    } else {
                        switchAt(CATCH_AT, track, path(CATCH_AT to track.at(CATCH_AT), RECOVER_AT to endPoint - carryOffset(forward)))
                    }
                }
            } else {
                coverage
            }

        val jammerCount = PuntFormation.JAMMERS.size
        val blocking =
            returnStarts.mapIndexed { index, start ->
                val rusher = index < PuntFormation.RUSHERS.size
                val jammer = !rusher && index < PuntFormation.RUSHERS.size + jammerCount
                when {
                    index == returnerIndex -> returner
                    rusher ->
                        path(
                            0f to start,
                            0.15f to start - FieldPoint(forward * 2f, 0f),
                            0.5f to FieldPoint(landing.along - forward * 12f, start.lateral * 1.4f),
                        )
                    jammer -> {
                        val gunner = PuntFormation.GUNNERS[index - PuntFormation.RUSHERS.size]
                        coverage[gunner].offsetBy(start - kickingStarts[gunner])
                    }
                    else -> path(0f to start, 0.2f to start, 0.5f to FieldPoint(landing.along - forward * 6f, start.lateral))
                }
            }

        val returnTeam =
            DownfieldEscort.follow(
                before = blocking,
                carrier = returner,
                from = CATCH_AT,
                until = returnAt,
                exclude = setOf(returnerIndex),
                speed = { Pursuit.DEFENSIVE_BACK_SPEED },
            )

        val snapFrom = context.offenseSpot(0.3f, 0f)
        val kickFrom = punter.at(KICK_AT) + carryOffset(forward)
        val ball =
            BallTrack { progress ->
                when {
                    progress < SNAP_AT -> {
                        val fraction = segment(progress, 0f, SNAP_AT)
                        BallState(snapFrom.lerp(punterStart + carryOffset(forward), fraction), arc(fraction, 1.2f))
                    }
                    progress < KICK_AT -> BallState(punter.at(progress) + carryOffset(forward))
                    progress < CATCH_AT -> {
                        val fraction = segment(progress, KICK_AT, CATCH_AT)
                        BallState(kickFrom.lerp(landing, fraction), arc(fraction, PUNT_HEIGHT), tumbling = true)
                    }
                    muffed -> {
                        val fraction = segment(progress, CATCH_AT, RECOVER_AT)
                        BallState(landing.lerp(endPoint, fraction), bounce(fraction, 1.5f, 3))
                    }
                    else -> BallState(returnBall.at(progress))
                }
            }
        val settled = if (muffed) minOf(1f, RECOVER_AT + RETURN_SETTLE) else minOf(1f, returnAt + RETURN_SETTLE)
        return Choreography(kickingTeam, returnTeam, ball, endsAt = settled)
    }

    private fun landingSpot(
        context: PlayContext,
        endPoint: FieldPoint,
        returnScores: Boolean,
        muffed: Boolean,
    ): FieldPoint {
        val forward = context.forward
        val receivingGoal = if (forward > 0f) 100f else 0f
        val deepest = receivingGoal - forward * MIN_DEPTH_FROM_GOAL
        val nominal =
            when {
                returnScores -> context.lineOfScrimmage + forward * TOUCHDOWN_RETURN_PUNT
                muffed -> endPoint.along + forward * 2f
                else -> endPoint.along + forward * RETURN_YARDS
            }
        val clamped = if ((nominal - deepest) * forward > 0f) deepest else nominal
        val shortest = context.lineOfScrimmage + forward * MIN_PUNT
        val along = if ((clamped - shortest) * forward < 0f) endPoint.along else clamped
        return FieldPoint(along, endPoint.lateral)
    }

    companion object {
        private const val SNAP_AT = 0.1f
        private const val KICK_AT = 0.17f
        private const val CATCH_AT = 0.55f
        private const val RECOVER_AT = 0.7f
        private const val RETURN_SETTLE = 0.05f
        private const val GUNNER_RELEASE = 0.05f
        private const val PUNT_HEIGHT = 16f
        private const val PUNTER_SPEED = 12f
        private const val RETURN_YARDS = 8f
        private const val TOUCHDOWN_RETURN_PUNT = 42f
        private const val MIN_DEPTH_FROM_GOAL = 5f
        private const val MIN_PUNT = 20f
    }
}
