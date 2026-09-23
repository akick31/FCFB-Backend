package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.DownfieldEscort
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.arc
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.hold
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt
import kotlin.math.abs

class ShortKickReturnScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val forward = context.forward
        val side = context.side
        val returnDirection = -forward
        val kickingGoal = if (forward > 0f) 100f else 0f
        val catchSpot = FieldPoint(kickingGoal + forward * CATCH_DEPTH_IN_END_ZONE, side * CATCH_WIDTH)
        val endPoint = FieldPoint(context.endSpot, side * RETURN_WIDTH)

        val kickingStarts = PuntFormation.kickingTeam(PuntFormation.FIELD_GOAL_DEPTH).map { context.offenseSpot(it.depth, it.lateral) }
        val returnStarts =
            (PuntFormation.RUSHERS + PuntFormation.JAMMERS + PuntFormation.MIDDLE).map { context.defenseSpot(it.depth, it.lateral) } +
                (catchSpot - carryOffset(returnDirection))
        val returnerIndex = returnStarts.lastIndex

        val returnAt = SCORE_AT
        val returnBall = path(CATCH_AT to catchSpot, returnAt to endPoint)
        val returner = switchAt(CATCH_AT, hold(returnStarts[returnerIndex]), carrierOf(returnBall, returnDirection))

        val kickerStart = kickingStarts[PuntFormation.KICKER]
        val kicker = path(0f to kickerStart, SNAP_AT to kickerStart, KICK_AT to kickerStart + FieldPoint(forward * 1.2f, 0f))
        val returnPace = catchSpot.distanceTo(endPoint) / maxOf(returnAt - CATCH_AT, MIN_RETURN_TIME)
        val coverage =
            kickingStarts.mapIndexed { index, start ->
                val before = if (index == PuntFormation.KICKER) kicker else hold(start)
                val aim = Pursuit.intercept(returner, coverageLead(index), returnAt, returnDirection)
                Pursuit.chase(before, coverageRelease(index), coveragePace(index, returnPace), aim)
            }

        val blocking =
            returnStarts.mapIndexed { index, start ->
                if (index == returnerIndex) {
                    returner
                } else {
                    val wallDepth = WALL_DEPTH + (index % WALL_RANKS) * WALL_RANK_SPACING
                    val spread = if (abs(start.lateral) > WIDE_ALIGNMENT) 1f else WALL_SPREAD
                    val wallSpot = FieldPoint(catchSpot.along - forward * wallDepth, start.lateral * spread)
                    path(0f to start, KICK_AT to start, CATCH_AT to wallSpot)
                }
            }
        val escortSpeed = Pursuit.paced(maxOf(Pursuit.DEFENSIVE_BACK_SPEED, returnPace * ESCORT_PACE))
        val returnTeam =
            DownfieldEscort.follow(
                before = blocking,
                carrier = returner,
                from = CATCH_AT,
                until = returnAt,
                exclude = setOf(returnerIndex),
                speed = { escortSpeed },
            )

        val snapFrom = context.offenseSpot(0.3f, 0f)
        val kickFrom = kicker.at(KICK_AT) + carryOffset(forward)
        val ball =
            BallTrack { progress ->
                when {
                    progress < SNAP_AT -> {
                        val fraction = segment(progress, 0f, SNAP_AT)
                        BallState(snapFrom.lerp(kickerStart + carryOffset(forward), fraction), arc(fraction, 1f))
                    }
                    progress < KICK_AT -> BallState(kicker.at(progress) + carryOffset(forward))
                    progress < CATCH_AT -> {
                        val fraction = segment(progress, KICK_AT, CATCH_AT)
                        BallState(kickFrom.lerp(catchSpot, fraction), arc(fraction, KICK_HEIGHT), tumbling = true)
                    }
                    else -> BallState(returnBall.at(progress))
                }
            }
        return Choreography(coverage, returnTeam, ball, endsAt = minOf(1f, returnAt + RETURN_SETTLE))
    }

    private fun coverageRelease(index: Int): Float =
        when {
            index == PuntFormation.KICKER -> CATCH_AT + KICKER_DELAY
            index in PuntFormation.GUNNERS -> CATCH_AT - GUNNER_LEAD
            index >= FIRST_BACK -> CATCH_AT
            else -> CATCH_AT + LINE_DELAY
        }

    private fun coverageLead(index: Int): Float = MIN_LEAD + (index % LEAD_LANES) * LEAD_STEP

    private fun coveragePace(
        index: Int,
        returnPace: Float,
    ): Float {
        val share =
            when {
                index == PuntFormation.KICKER -> KICKER_PACE
                index in PuntFormation.GUNNERS -> GUNNER_PACE
                index >= FIRST_BACK -> BACK_PACE
                else -> LINE_PACE
            }
        return Pursuit.paced(maxOf(Pursuit.LINEMAN_SPEED, returnPace * share))
    }

    companion object {
        private const val SNAP_AT = 0.1f
        private const val KICK_AT = 0.17f
        private const val CATCH_AT = 0.42f
        private const val RETURN_SETTLE = 0.05f
        private const val CATCH_DEPTH_IN_END_ZONE = 3f
        private const val CATCH_WIDTH = 2f
        private const val RETURN_WIDTH = 4f
        private const val WALL_DEPTH = 10f
        private const val WALL_RANKS = 4
        private const val WALL_RANK_SPACING = 5f
        private const val WALL_SPREAD = 1.3f
        private const val WIDE_ALIGNMENT = 12f
        private const val KICK_HEIGHT = 20f
        private const val FIRST_BACK = 5
        private const val GUNNER_PACE = 0.80f
        private const val BACK_PACE = 0.68f
        private const val LINE_PACE = 0.58f
        private const val KICKER_PACE = 0.45f
        private const val MIN_LEAD = 0.02f
        private const val LEAD_LANES = 5
        private const val LEAD_STEP = 0.02f
        private const val GUNNER_LEAD = 0.04f
        private const val LINE_DELAY = 0.03f
        private const val KICKER_DELAY = 0.06f
        private const val ESCORT_PACE = 0.85f
        private const val MIN_RETURN_TIME = 0.05f
    }
}
