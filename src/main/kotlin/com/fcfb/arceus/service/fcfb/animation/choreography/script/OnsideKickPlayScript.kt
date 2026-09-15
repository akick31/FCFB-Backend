package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.Track
import com.fcfb.arceus.service.fcfb.animation.choreography.bounce
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.hold
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt
import kotlin.math.cos
import kotlin.math.sin

class OnsideKickPlayScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val forward = context.forward
        val side = context.side
        val result = context.play.actualResult
        val kickingRecovers = result == ActualResult.SUCCESSFUL_ONSIDE || result == ActualResult.KICKING_TEAM_TOUCHDOWN
        val kickSpot = FieldPoint(context.lineOfScrimmage, 0f)
        val bouncePoint = context.defenseSpot(BOUNCE_DEPTH, side * BOUNCE_WIDTH)
        val scrum = hold(bouncePoint)

        val kickingStarts =
            listOf(context.offenseSpot(KICKER_DEPTH, -side)) + KICK_LANES.map { context.offenseSpot(1f, it + side * OVERLOAD) }
        val handsStarts =
            FRONT.map { context.defenseSpot(11f, it + side * 3f) } +
                SECOND.map { context.defenseSpot(16f, it + side * 3f) } +
                DEEP.map { context.defenseSpot(25f, it) }
        val kickingBefore =
            kickingStarts.mapIndexed { index, start ->
                val approach = if (index == 0) path(0f to start, KICK_AT to context.offenseSpot(0.6f, 0f)) else hold(start)
                Pursuit.chase(approach, KICK_AT, SCRUM_SPEED, Pursuit.trail(scrum, SCRUM_RADIUS + (index % 4) * SCRUM_STEP))
            }
        val handsBefore =
            handsStarts.mapIndexed { index, start ->
                Pursuit.chase(
                    hold(start),
                    KICK_AT + 0.04f,
                    SCRUM_SPEED,
                    Pursuit.trail(scrum, SCRUM_RADIUS + ((index + 1) % 4) * SCRUM_STEP),
                )
            }

        val recoveringTeam = if (kickingRecovers) kickingBefore else handsBefore
        val recovererIndex = recoveringTeam.indices.minBy { recoveringTeam[it].at(RECOVER_AT).distanceTo(bouncePoint) }
        val direction = if (kickingRecovers) forward else -forward
        val endPoint = FieldPoint(context.endSpot, bouncePoint.lateral)
        val returnAt = minOf(0.88f, RECOVER_AT + 0.1f + 0.3f * minOf(bouncePoint.distanceTo(endPoint), 20f) / 20f)
        val returnBall = path(RECOVER_AT to bouncePoint, returnAt to endPoint)
        val recoverer = recoveringTeam[recovererIndex]
        val carrier =
            switchAt(
                DIVE_AT,
                recoverer,
                switchAt(
                    RECOVER_AT,
                    path(DIVE_AT to recoverer.at(DIVE_AT), RECOVER_AT to bouncePoint - carryOffset(direction)),
                    carrierOf(returnBall, direction),
                ),
            )
        val kickingTeam = afterRecovery(kickingBefore, carrier, if (kickingRecovers) recovererIndex else -1)
        val handsTeam = afterRecovery(handsBefore, carrier, if (kickingRecovers) -1 else recovererIndex)

        val ball =
            BallTrack { progress ->
                when {
                    progress < KICK_AT -> BallState(kickSpot)
                    progress < BOUNCE_END -> {
                        val fraction = segment(progress, KICK_AT, BOUNCE_END)
                        BallState(kickSpot.lerp(bouncePoint, fraction), bounce(fraction, HOP_HEIGHT, 2))
                    }
                    progress < RECOVER_AT -> {
                        val wobble = FieldPoint(sin(progress * 60f) * 0.8f, cos(progress * 50f) * 0.6f)
                        BallState(bouncePoint + wobble)
                    }
                    else -> BallState(returnBall.at(progress))
                }
            }
        return Choreography(kickingTeam, handsTeam, ball)
    }

    private fun afterRecovery(
        team: List<Track>,
        carrier: Track,
        carrierIndex: Int,
    ): List<Track> =
        team.mapIndexed { index, track ->
            if (index == carrierIndex) {
                carrier
            } else {
                Pursuit.chase(track, RECOVER_AT, SCRUM_SPEED, Pursuit.trail(carrier, 2.5f + index % 3))
            }
        }

    companion object {
        private const val KICK_AT = 0.08f
        private const val BOUNCE_END = 0.3f
        private const val DIVE_AT = 0.34f
        private const val RECOVER_AT = 0.42f
        private const val HOP_HEIGHT = 3f
        private const val BOUNCE_DEPTH = 10.5f
        private const val BOUNCE_WIDTH = 8f
        private const val KICKER_DEPTH = 5f
        private const val OVERLOAD = 4f
        private const val SCRUM_SPEED = 26f
        private const val SCRUM_RADIUS = 1.5f
        private const val SCRUM_STEP = 0.9f
        private val KICK_LANES = listOf(-12f, -9f, -6f, -3f, -1f, 1f, 3f, 6f, 9f, 12f)
        private val FRONT = listOf(-10f, -5f, 0f, 5f, 10f)
        private val SECOND = listOf(-8f, -3f, 3f, 8f)
        private val DEEP = listOf(-6f, 6f)
    }
}
