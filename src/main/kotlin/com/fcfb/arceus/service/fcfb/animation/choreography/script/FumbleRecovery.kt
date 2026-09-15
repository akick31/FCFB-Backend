package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.OffensiveAlignments
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.Track
import com.fcfb.arceus.service.fcfb.animation.choreography.bounce
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

internal object FumbleRecovery {
    private const val LOOSE_BALL_TIME = 0.1f
    private const val SCOOP_DISTANCE = 1.5f
    private const val RETURN_WIDTH = 6f
    private const val CHASE_RADIUS = 3f

    fun choreograph(
        context: PlayContext,
        offense: List<Track>,
        defense: List<Track>,
        carried: BallTrack,
        fumbleAt: Float,
        fumbleSpot: FieldPoint,
        defenseScores: Boolean,
    ): Choreography {
        val recoverAt = fumbleAt + LOOSE_BALL_TIME
        val returnDirection = -context.forward
        val recoverySpot =
            if (defenseScores) {
                fumbleSpot + FieldPoint(context.forward * SCOOP_DISTANCE, context.side * SCOOP_DISTANCE)
            } else {
                FieldPoint(context.endSpot, fumbleSpot.lateral)
            }
        val returnEnd = if (defenseScores) FieldPoint(context.endSpot, context.side * RETURN_WIDTH) else recoverySpot
        val returnBall = path(recoverAt to recoverySpot, (if (defenseScores) SCORE_AT else recoverAt) to returnEnd)
        val recoverer = defense.indices.minBy { defense[it].at(fumbleAt).distanceTo(recoverySpot) }
        val scoop = path(fumbleAt to defense[recoverer].at(fumbleAt), recoverAt to recoverySpot - carryOffset(returnDirection))
        val recovererTrack = switchAt(fumbleAt, defense[recoverer], switchAt(recoverAt, scoop, carrierOf(returnBall, returnDirection)))

        val finalDefense = defense.mapIndexed { index, track -> if (index == recoverer) recovererTrack else track }
        val finalOffense =
            if (defenseScores) {
                offense.mapIndexed { index, track ->
                    if (index in OffensiveAlignments.LINEMEN) {
                        track
                    } else {
                        Pursuit.chase(track, recoverAt, Pursuit.LINEBACKER_SPEED, Pursuit.trail(recovererTrack, CHASE_RADIUS))
                    }
                }
            } else {
                offense
            }
        val ball =
            BallTrack { progress ->
                when {
                    progress < fumbleAt -> carried.at(progress)
                    progress < recoverAt -> {
                        val fraction = segment(progress, fumbleAt, recoverAt)
                        BallState(fumbleSpot.lerp(recoverySpot, fraction), bounce(fraction, 1.5f, 2))
                    }
                    else -> BallState(returnBall.at(progress))
                }
            }
        return Choreography(finalOffense, finalDefense, ball)
    }
}
