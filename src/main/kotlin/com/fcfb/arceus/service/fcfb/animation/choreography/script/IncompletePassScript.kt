package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.IncompletionKind
import com.fcfb.arceus.service.fcfb.animation.PlayRandom
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
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.Track
import com.fcfb.arceus.service.fcfb.animation.choreography.arc
import com.fcfb.arceus.service.fcfb.animation.choreography.bounce
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

/**
 * Incompletions are drawn from a handful of ways a pass falls dead: drops, deep shots that get away, balls a defender nearly
 * picks, throwaways under pressure, and screens thrown wide. The play's own seed decides which one, so it never changes.
 */
class IncompletePassScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context)
        val concept = PassConcept(context, scene)
        val random = PlayRandom(context.play)
        val kind = random.pick(IncompletionKind.entries.toList())
        val forward = context.forward
        val side = context.side

        val depth = depthOf(kind, random)
        val targetIndex = concept.target(deep = depth > DEEP_TARGET_DEPTH, random = random)
        val targetLateral = scene.offense[targetIndex].lateral
        val lateral = lateralOf(kind, targetLateral, side)
        val arrivalPoint = FieldPoint(context.lineOfScrimmage + forward * depth, lateral)
        val throwAt = concept.throwAt
        val arriveAt = throwAt + concept.flightTime(arrivalPoint)

        val receiverSpot = receiverSpotFor(kind, arrivalPoint, forward)
        val receiver =
            switchAt(
                arriveAt,
                concept.routeTo(targetIndex, receiverSpot, arriveAt),
                path(arriveAt to receiverSpot, SETTLE_AT to receiverSpot + FieldPoint(forward * 2f, 0f)),
            )
        val quarterback = concept.throwingQuarterback()
        val blocking = concept.offense(quarterback, mapOf(targetIndex to receiver))
        val offense =
            DownfieldEscort.follow(
                before = blocking,
                carrier = quarterback,
                from = PROTECT_FROM,
                until = arriveAt,
                exclude = setOf(targetIndex, scene.offensiveAlignment.quarterback),
                speed = { if (it in OffensiveAlignments.LINEMEN) Pursuit.LINEMAN_SPEED else Pursuit.DEFENSIVE_BACK_SPEED },
            )
        val before = DefensiveReaction.before(context, scene, offense, dropping = true)
        val defense = defense(kind, scene, before, arrivalPoint, arriveAt, forward)

        val held = concept.heldBall(quarterback)
        val restSpot = arrivalPoint + FieldPoint(forward * BOUNCE_ROLL, side * BOUNCE_DRIFT)
        val tip = tipHeightOf(kind)
        val ball =
            BallTrack { progress ->
                when {
                    progress < throwAt -> held.at(progress)
                    progress < arriveAt -> concept.thrown(arrivalPoint, segment(progress, throwAt, arriveAt))
                    else -> {
                        val fraction = segment(progress, arriveAt, arriveAt + FALL_TIME)
                        val hop = if (tip > 0f) arc(fraction, tip) else bounce(fraction, BOUNCE_HEIGHT, BOUNCES)
                        BallState(arrivalPoint.lerp(restSpot, fraction), hop, tumbling = fraction < 1f)
                    }
                }
            }
        return Choreography(offense, defense, ball, facingLocked = CompletedPassScript.facingLocked(scene))
    }

    private fun depthOf(
        kind: IncompletionKind,
        random: PlayRandom,
    ): Float =
        when (kind) {
            IncompletionKind.DROPPED -> random.between(8f, 15f)
            IncompletionKind.DROPPED_DEEP -> random.between(28f, 45f)
            IncompletionKind.BROKEN_UP -> random.between(10f, 20f)
            IncompletionKind.ALMOST_INTERCEPTED -> random.between(14f, 24f)
            IncompletionKind.ALMOST_PICKED_CROSSER -> random.between(6f, 11f)
            IncompletionKind.OVERTHROWN -> random.between(22f, 38f)
            IncompletionKind.THROWN_AWAY -> random.between(4f, 9f)
            IncompletionKind.SCREEN_WIDE -> random.between(-2f, 1f)
        }

    private fun lateralOf(
        kind: IncompletionKind,
        targetLateral: Float,
        side: Float,
    ): Float =
        when (kind) {
            IncompletionKind.THROWN_AWAY -> side * THROWAWAY_WIDTH
            IncompletionKind.SCREEN_WIDE -> side * SCREEN_WIDTH
            IncompletionKind.ALMOST_PICKED_CROSSER -> -targetLateral * CROSSER_SWING
            else -> targetLateral * TARGET_DRIFT
        }

    /** On an overthrow or a throwaway the receiver never gets there, so he pulls up short of where the ball lands. */
    private fun receiverSpotFor(
        kind: IncompletionKind,
        arrivalPoint: FieldPoint,
        forward: Float,
    ): FieldPoint =
        when (kind) {
            IncompletionKind.OVERTHROWN -> arrivalPoint - FieldPoint(forward * OVERTHROW_GAP, 0f)
            IncompletionKind.THROWN_AWAY -> arrivalPoint - FieldPoint(forward * THROWAWAY_GAP, THROWAWAY_GAP)
            else -> arrivalPoint - carryOffset(forward)
        }

    private fun tipHeightOf(kind: IncompletionKind): Float =
        when (kind) {
            IncompletionKind.ALMOST_INTERCEPTED, IncompletionKind.ALMOST_PICKED_CROSSER -> TIP_HEIGHT
            else -> 0f
        }

    /** Only the kinds where a defender is at the ball send someone driving on it; the rest just stay in coverage. */
    private fun defense(
        kind: IncompletionKind,
        scene: ScrimmageScene,
        before: List<Track>,
        arrivalPoint: FieldPoint,
        arriveAt: Float,
        forward: Float,
    ): List<Track> {
        val contested =
            kind == IncompletionKind.BROKEN_UP ||
                kind == IncompletionKind.ALMOST_INTERCEPTED ||
                kind == IncompletionKind.ALMOST_PICKED_CROSSER
        if (!contested) return before
        val breakAt = arriveAt - BREAK_ON_BALL
        val defender = Pursuit.closest(before.map { it.at(breakAt) }, scene.defensiveAlignment.secondary, arrivalPoint, 1).first()
        return before.mapIndexed { index, track ->
            if (index == defender) {
                Pursuit.chase(track, breakAt, Pursuit.COVERAGE_SPEED, Pursuit.toward(arrivalPoint + FieldPoint(forward, 1f)))
            } else {
                track
            }
        }
    }

    companion object {
        private const val PROTECT_FROM = 0.3f
        private const val DEEP_TARGET_DEPTH = 18f
        private const val BREAK_ON_BALL = 0.2f
        private const val FALL_TIME = 0.25f
        private const val SETTLE_AT = 0.9f
        private const val TARGET_DRIFT = 0.6f
        private const val CROSSER_SWING = 0.5f
        private const val THROWAWAY_WIDTH = 20f
        private const val SCREEN_WIDTH = 13f
        private const val OVERTHROW_GAP = 4f
        private const val THROWAWAY_GAP = 3f
        private const val TIP_HEIGHT = 3.2f
        private const val BOUNCE_HEIGHT = 1.8f
        private const val BOUNCES = 3
        private const val BOUNCE_ROLL = 3f
        private const val BOUNCE_DRIFT = 1.5f
    }
}
