package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.service.fcfb.animation.OnsideKickKind
import com.fcfb.arceus.service.fcfb.animation.PlayRandom
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
        val result = context.play.actualResult
        val kickingRecovers = result == ActualResult.SUCCESSFUL_ONSIDE || result == ActualResult.KICKING_TEAM_TOUCHDOWN
        val random = PlayRandom(context.play)
        val kind = random.pick(OnsideKickKind.entries.toList())
        val kickSide = random.side()
        val bounceDepth = random.between(kind.minDepth, kind.maxDepth)
        val bouncePoint = context.defenseSpot(bounceDepth, kickSide * random.between(MIN_WIDTH, MAX_WIDTH))
        val cleanField = !kickingRecovers && random.chance(CLEAN_FIELD_CHANCE)
        val recoverAt = if (cleanField) CLEAN_RECOVER_AT else random.between(SCRAMBLE_RECOVER_MIN, SCRAMBLE_RECOVER_MAX)
        val diveAt = recoverAt - DIVE_LEAD
        val chasers = if (cleanField) CLEAN_CHASERS else random.pick(SCRUM_SIZES)

        val kickSpot = FieldPoint(context.lineOfScrimmage, 0f)
        val scrum = hold(bouncePoint)
        val kickingStarts =
            listOf(context.offenseSpot(KICKER_DEPTH, -kickSide)) +
                KICK_LANES.map { context.offenseSpot(1f, it + kickSide * OVERLOAD) }
        val handsStarts =
            FRONT.map { context.defenseSpot(11f, it + kickSide * 3f) } +
                SECOND.map { context.defenseSpot(16f, it + kickSide * 3f) } +
                DEEP.map { context.defenseSpot(25f, it) }

        val kickingChasers = Pursuit.closest(kickingStarts, kickingStarts.indices.drop(1), bouncePoint, chasers)
        val handsChasers = Pursuit.closest(handsStarts, handsStarts.indices, bouncePoint, if (cleanField) CLEAN_CHASERS else chasers)

        val kickingBefore =
            kickingStarts.mapIndexed { index, start ->
                val approach = if (index == 0) path(0f to start, KICK_AT to context.offenseSpot(0.6f, 0f)) else hold(start)
                converge(approach, KICK_AT, scrum, index, index in kickingChasers)
            }
        val handsBefore =
            handsStarts.mapIndexed { index, start ->
                converge(hold(start), KICK_AT + HANDS_REACTION, scrum, index + 1, index in handsChasers)
            }

        val recoveringTeam = if (kickingRecovers) kickingBefore else handsBefore
        val recovererIndex = recoveringTeam.indices.minBy { recoveringTeam[it].at(recoverAt).distanceTo(bouncePoint) }
        val direction = if (kickingRecovers) forward else -forward
        val endPoint = FieldPoint(context.endSpot, bouncePoint.lateral)
        val returnAt = minOf(RETURN_LIMIT, recoverAt + RETURN_LEAD + RETURN_PER_YARD * minOf(bouncePoint.distanceTo(endPoint), 20f) / 20f)
        val returnBall = path(recoverAt to bouncePoint, returnAt to endPoint)
        val recoverer = recoveringTeam[recovererIndex]
        val carrier =
            switchAt(
                diveAt,
                recoverer,
                switchAt(
                    recoverAt,
                    path(diveAt to recoverer.at(diveAt), recoverAt to bouncePoint - carryOffset(direction)),
                    carrierOf(returnBall, direction),
                ),
            )
        val kickingTeam = afterRecovery(kickingBefore, carrier, if (kickingRecovers) recovererIndex else -1, recoverAt)
        val handsTeam = afterRecovery(handsBefore, carrier, if (kickingRecovers) -1 else recovererIndex, recoverAt)

        val ball =
            BallTrack { progress ->
                when {
                    progress < KICK_AT -> BallState(kickSpot)
                    progress < BOUNCE_END -> {
                        val fraction = segment(progress, KICK_AT, BOUNCE_END)
                        BallState(kickSpot.lerp(bouncePoint, fraction), bounce(fraction, kind.hopHeight, kind.hops), tumbling = true)
                    }
                    progress < recoverAt -> BallState(bouncePoint + squirt(cleanField, progress))
                    else -> BallState(returnBall.at(progress))
                }
            }
        return Choreography(kickingTeam, handsTeam, ball, endsAt = minOf(1f, returnAt + SETTLE))
    }

    private fun converge(
        before: Track,
        from: Float,
        scrum: Track,
        index: Int,
        chasing: Boolean,
    ): Track {
        val speed = if (chasing) SCRUM_SPEED else CONTAIN_SPEED
        val radius = if (chasing) SCRUM_RADIUS + (index % 4) * SCRUM_STEP else CONTAIN_RADIUS + (index % 3) * SCRUM_STEP
        return Pursuit.chase(before, from, speed, Pursuit.trail(scrum, radius))
    }

    private fun squirt(
        cleanField: Boolean,
        progress: Float,
    ): FieldPoint {
        if (cleanField) return FieldPoint(0f, 0f)
        return FieldPoint(sin(progress * SQUIRT_ALONG) * SQUIRT_SCALE, cos(progress * SQUIRT_LATERAL) * SQUIRT_SCALE)
    }

    private fun afterRecovery(
        team: List<Track>,
        carrier: Track,
        carrierIndex: Int,
        recoverAt: Float,
    ): List<Track> =
        team.mapIndexed { index, track ->
            if (index == carrierIndex) {
                carrier
            } else {
                Pursuit.chase(track, recoverAt, SCRUM_SPEED, Pursuit.trail(carrier, ESCORT_RADIUS + index % 3))
            }
        }

    companion object {
        private const val KICK_AT = 0.08f
        private const val BOUNCE_END = 0.3f
        private const val DIVE_LEAD = 0.08f
        private const val HANDS_REACTION = 0.04f
        private const val CLEAN_RECOVER_AT = 0.34f
        private const val SCRAMBLE_RECOVER_MIN = 0.42f
        private const val SCRAMBLE_RECOVER_MAX = 0.55f
        private const val CLEAN_FIELD_CHANCE = 0.6f
        private const val CLEAN_CHASERS = 1
        private const val MIN_WIDTH = 4f
        private const val MAX_WIDTH = 13f
        private const val KICKER_DEPTH = 5f
        private const val OVERLOAD = 4f
        private const val SCRUM_SPEED = 26f
        private const val CONTAIN_SPEED = 18f
        private const val SCRUM_RADIUS = 1.5f
        private const val CONTAIN_RADIUS = 6f
        private const val SCRUM_STEP = 0.9f
        private const val ESCORT_RADIUS = 2.5f
        private const val RETURN_LIMIT = 0.88f
        private const val RETURN_LEAD = 0.1f
        private const val RETURN_PER_YARD = 0.3f
        private const val SETTLE = 0.05f
        private const val SQUIRT_ALONG = 60f
        private const val SQUIRT_LATERAL = 50f
        private const val SQUIRT_SCALE = 0.8f
        private val SCRUM_SIZES = listOf(3, 4, 5, 6)
        private val KICK_LANES = listOf(-12f, -9f, -6f, -3f, -1f, 1f, 3f, 6f, 9f, 12f)
        private val FRONT = listOf(-10f, -5f, 0f, 5f, 10f)
        private val SECOND = listOf(-8f, -3f, 3f, 8f)
        private val DEEP = listOf(-6f, 6f)
    }
}
