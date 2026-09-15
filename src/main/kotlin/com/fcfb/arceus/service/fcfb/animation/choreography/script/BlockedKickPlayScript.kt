package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
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

class BlockedKickPlayScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val forward = context.forward
        val side = context.side
        val returnDirection = -forward
        val fieldGoal = context.play.playCall == PlayCall.FIELD_GOAL || context.play.playCall == PlayCall.PAT
        val defenseScores = context.play.actualResult == ActualResult.KICK_SIX
        val kickerDepth = if (fieldGoal) PuntFormation.FIELD_GOAL_DEPTH else PuntFormation.PUNT_DEPTH

        val kickingStarts = PuntFormation.kickingTeam(kickerDepth).map { context.offenseSpot(it.depth, it.lateral) }
        val kickerStart = kickingStarts[PuntFormation.KICKER]
        val kicker = path(0f to kickerStart, SNAP_AT to kickerStart, KICK_AT to kickerStart + FieldPoint(forward * 1.2f, 0f))
        val blockSpot = kickerStart + FieldPoint(forward * BLOCK_DISTANCE, 0f)

        val rushStarts =
            (PuntFormation.RUSHERS + PuntFormation.JAMMERS + PuntFormation.MIDDLE).map { context.defenseSpot(it.depth, it.lateral) } +
                context.defenseSpot(DEEP_DEPTH, 0f)
        val blocker = BLOCKER
        val rushBefore =
            rushStarts.mapIndexed { index, start ->
                when {
                    index == blocker -> path(0f to start, KICK_AT to blockSpot + FieldPoint(forward, 0f))
                    index < PuntFormation.RUSHERS.size -> path(0f to start, 0.25f to start - FieldPoint(forward * 1.5f, 0f))
                    else -> hold(start)
                }
            }

        val recoverySpot =
            if (defenseScores) {
                blockSpot + FieldPoint(-forward * 3f, side * 2f)
            } else {
                FieldPoint(context.endSpot, side * 2f)
            }
        val returnEnd = if (defenseScores) FieldPoint(context.endSpot, side * 4f) else recoverySpot
        val returnBall = path(RECOVER_AT to recoverySpot, (if (defenseScores) SCORE_AT else RECOVER_AT) to returnEnd)
        val recoverer = rushBefore.indices.minBy { rushBefore[it].at(RECOVER_AT).distanceTo(recoverySpot) }
        val recovererTrack =
            switchAt(
                DEFLECT_AT,
                rushBefore[recoverer],
                switchAt(
                    RECOVER_AT,
                    path(DEFLECT_AT to rushBefore[recoverer].at(DEFLECT_AT), RECOVER_AT to recoverySpot - carryOffset(returnDirection)),
                    carrierOf(returnBall, returnDirection),
                ),
            )
        val rushTeam = rushBefore.mapIndexed { index, track -> if (index == recoverer) recovererTrack else track }
        val kickingTeam =
            kickingStarts.mapIndexed { index, start ->
                val before = if (index == PuntFormation.KICKER) kicker else hold(start)
                if (defenseScores) {
                    Pursuit.chase(
                        before,
                        RECOVER_AT,
                        Pursuit.LINEBACKER_SPEED,
                        Pursuit.trail(recovererTrack, 3f),
                    )
                } else {
                    before
                }
            }

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
                    progress < DEFLECT_AT -> BallState(kickFrom.lerp(blockSpot, segment(progress, KICK_AT, DEFLECT_AT)), 1.5f)
                    progress < RECOVER_AT -> {
                        val fraction = segment(progress, DEFLECT_AT, RECOVER_AT)
                        BallState(blockSpot.lerp(recoverySpot, fraction), bounce(fraction, 3f, 2))
                    }
                    else -> BallState(returnBall.at(progress))
                }
            }
        return Choreography(kickingTeam, rushTeam, ball)
    }

    companion object {
        private const val SNAP_AT = 0.1f
        private const val KICK_AT = 0.17f
        private const val DEFLECT_AT = 0.2f
        private const val RECOVER_AT = 0.4f
        private const val BLOCK_DISTANCE = 3f
        private const val DEEP_DEPTH = 12f
        private const val BLOCKER = 2
    }
}
