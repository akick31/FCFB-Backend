package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Callout
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.GangTackle
import com.fcfb.arceus.service.fcfb.animation.choreography.OffensiveAlignments
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.Track
import com.fcfb.arceus.service.fcfb.animation.choreography.arc
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.carryTime
import com.fcfb.arceus.service.fcfb.animation.choreography.hold
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt
import java.util.Random
import kotlin.math.pow

/**
 * Mirrors NormalPlayProcessor's turnover spots: "TO + N" is recovered at the line of scrimmage and returned N yards,
 * "TO - N" is lost after the runner gains N yards, and a plain turnover changes hands at the line.
 */
internal object FumbleRecovery {
    private const val LOOSE_BALL_TIME = 0.2f
    private const val RETURNED_LOOSE_BALL_TIME = 0.1f
    private const val SQUIRT_BACK = 2.5f
    private const val SQUIRT_WIDE = 1.5f
    private const val HOPS = 4
    private const val FIRST_HOP_HEIGHT = 1.8f
    private const val HOP_DECAY = 0.6f
    private const val HOP_JITTER_ALONG = 1.2f
    private const val HOP_JITTER_LATERAL = 2.2f
    private const val SCRAMBLE_DELAY = 0.03f
    private const val DEFENDERS_IN_SCRAMBLE = 3
    private const val OFFENSE_IN_SCRAMBLE = 2
    private const val PILE_RADIUS = 1.3f
    private const val RETURN_WIDTH = 5f
    private const val ESCORT_DELAY = 0.05f
    private const val ESCORT_RADIUS = 4f
    private const val CHASE_TACKLERS = 2
    private const val LATEST_FUMBLE_AT = 0.6f
    private const val RETURNED_FUMBLE_AT = 0.24f
    private const val CALLOUT_LINGER = 0.1f
    private const val CALLOUT_TEXT = "FUMBLE!"
    private const val SCOOP_TO_STRIDE = 0.06f

    private val DEFENSIVE_SCORES = setOf(ActualResult.TURNOVER_TOUCHDOWN, ActualResult.DEFENSE_TWO_POINT)

    fun fumbleSpot(
        context: PlayContext,
        lateral: Float,
    ): FieldPoint = recoverySpot(context, lateral) + FieldPoint(context.forward * SQUIRT_BACK, -context.side * SQUIRT_WIDE)

    fun latestFumbleAt(context: PlayContext): Float = if (isReturned(context)) RETURNED_FUMBLE_AT else LATEST_FUMBLE_AT

    fun choreograph(
        context: PlayContext,
        offense: List<Track>,
        defense: List<Track>,
        carried: BallTrack,
        fumbleAt: Float,
        fumbleSpot: FieldPoint,
    ): Choreography {
        val returnDirection = -context.forward
        val returned = isReturned(context)
        val recoverAt = fumbleAt + if (returned) RETURNED_LOOSE_BALL_TIME else LOOSE_BALL_TIME
        val recoverySpot = recoverySpot(context, fumbleSpot.lateral + context.side * SQUIRT_WIDE)
        val scores = context.play.actualResult in DEFENSIVE_SCORES
        val returnEnd = FieldPoint(context.endSpot, recoverySpot.lateral + if (returned) context.side * RETURN_WIDTH else 0f)
        val returnAt = minOf(SCORE_AT, recoverAt + SCOOP_TO_STRIDE + carryTime(recoverySpot.distanceTo(returnEnd)))
        val returnBall = if (returned) path(recoverAt to recoverySpot, returnAt to returnEnd) else hold(recoverySpot)

        val hops = hopPoints(context, fumbleSpot, recoverySpot)
        val looseBall = looseBallTrack(hops, fumbleAt, recoverAt, returnBall)

        val recoverer = defense.indices.minBy { defense[it].at(fumbleAt).distanceTo(recoverySpot) }
        val dive = path(fumbleAt to defense[recoverer].at(fumbleAt), recoverAt to recoverySpot - carryOffset(returnDirection))
        val recovererTrack = switchAt(fumbleAt, defense[recoverer], switchAt(recoverAt, dive, carrierOf(returnBall, returnDirection)))

        val defenseDivers = Pursuit.closest(defense.map { it.at(fumbleAt) }, defense.indices - recoverer, fumbleSpot, DEFENDERS_IN_SCRAMBLE)
        val finalDefense =
            defense.mapIndexed { index, track ->
                val scrambling = if (index in defenseDivers) scramble(track, fumbleAt, looseBall, index) else track
                when {
                    index == recoverer -> recovererTrack
                    returned ->
                        Pursuit.chase(
                            scrambling,
                            recoverAt + ESCORT_DELAY,
                            Pursuit.LINEBACKER_SPEED,
                            Pursuit.trail(recovererTrack, ESCORT_RADIUS + index % 3),
                        )
                    else -> scrambling
                }
            }

        val offenseDivers = Pursuit.closest(offense.map { it.at(fumbleAt) }, offense.indices, fumbleSpot, OFFENSE_IN_SCRAMBLE)
        val scrambledOffense =
            offense.mapIndexed { index, track -> if (index in offenseDivers) scramble(track, fumbleAt, looseBall, index + 1) else track }
        val finalOffense =
            if (returned) {
                GangTackle.converge(
                    before = scrambledOffense,
                    candidates = offense.indices.filter { it !in OffensiveAlignments.LINEMEN },
                    carrier = recovererTrack,
                    direction = returnDirection,
                    tackleAt = returnAt,
                    tacklers = if (scores) 0 else CHASE_TACKLERS,
                    reactAt = { recoverAt },
                    speed = { if (it in OffensiveAlignments.LINEMEN) Pursuit.LINEMAN_SPEED else Pursuit.DEFENSIVE_BACK_SPEED },
                )
            } else {
                scrambledOffense
            }

        val ball =
            BallTrack { progress ->
                when {
                    progress < fumbleAt -> carried.at(progress)
                    progress < recoverAt -> hopState(hops, segment(progress, fumbleAt, recoverAt))
                    else -> BallState(returnBall.at(progress))
                }
            }
        return Choreography(finalOffense, finalDefense, ball, Callout(CALLOUT_TEXT, fumbleAt, recoverAt + CALLOUT_LINGER))
    }

    private fun looseBallTrack(
        hops: List<FieldPoint>,
        fumbleAt: Float,
        recoverAt: Float,
        afterRecovery: Track,
    ): Track =
        Track { progress ->
            if (progress < recoverAt) hopState(hops, segment(progress, fumbleAt, recoverAt)).position else afterRecovery.at(progress)
        }

    private fun scramble(
        track: Track,
        fumbleAt: Float,
        looseBall: Track,
        index: Int,
    ): Track =
        Pursuit.chase(
            track,
            fumbleAt + SCRAMBLE_DELAY,
            Pursuit.DEFENSIVE_BACK_SPEED,
            Pursuit.trail(looseBall, PILE_RADIUS + index % 2),
        )

    private fun isReturned(context: PlayContext): Boolean = context.play.actualResult in DEFENSIVE_SCORES || returnYards(context) > 0f

    private fun recoverySpot(
        context: PlayContext,
        lateral: Float,
    ): FieldPoint {
        val along =
            if (context.play.actualResult in DEFENSIVE_SCORES) {
                context.lineOfScrimmage
            } else {
                context.endSpot + context.forward * returnYards(context)
            }
        return FieldPoint(along, lateral)
    }

    private fun returnYards(context: PlayContext): Float =
        when (context.play.result) {
            Scenario.TURNOVER_PLUS_20_YARDS -> 20f
            Scenario.TURNOVER_PLUS_15_YARDS -> 15f
            Scenario.TURNOVER_PLUS_10_YARDS -> 10f
            Scenario.TURNOVER_PLUS_5_YARDS -> 5f
            else -> 0f
        }

    private fun hopPoints(
        context: PlayContext,
        fumbleSpot: FieldPoint,
        recoverySpot: FieldPoint,
    ): List<FieldPoint> {
        val random = Random(context.play.playId.toLong())
        return (0..HOPS).map { hop ->
            when (hop) {
                0 -> fumbleSpot
                HOPS -> recoverySpot
                else ->
                    fumbleSpot.lerp(recoverySpot, hop.toFloat() / HOPS) +
                        FieldPoint((random.nextFloat() * 2f - 1f) * HOP_JITTER_ALONG, (random.nextFloat() * 2f - 1f) * HOP_JITTER_LATERAL)
            }
        }
    }

    private fun hopState(
        hops: List<FieldPoint>,
        fraction: Float,
    ): BallState {
        val scaled = fraction * HOPS
        val hop = minOf(scaled.toInt(), HOPS - 1)
        val local = scaled - hop
        return BallState(hops[hop].lerp(hops[hop + 1], local), arc(local, FIRST_HOP_HEIGHT * HOP_DECAY.pow(hop)), tumbling = true)
    }
}
