package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.service.fcfb.animation.CatchKind
import com.fcfb.arceus.service.fcfb.animation.DeepScoreKind
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
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.SNAP_END
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.Track
import com.fcfb.arceus.service.fcfb.animation.choreography.WeavingRun
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.carryTime
import com.fcfb.arceus.service.fcfb.animation.choreography.hold
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

class CompletedPassScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context)
        val forward = context.forward
        val offenseScores = context.play.actualResult in OFFENSIVE_SCORES
        val gain = context.gain
        val random = PlayRandom(context.play)
        val contested = !offenseScores && gain > CONTESTED_MIN_GAIN && random.chance(CONTESTED_CHANCE)
        val scoreKind = if (offenseScores && gain > SHORT_SCORE) deepScoreKind(gain, random) else DeepScoreKind.WIDE_OPEN
        val scoringDepth =
            when {
                offenseScores && gain <= SHORT_SCORE -> gain
                offenseScores -> scoringCatchDepth(scoreKind, gain, random)
                contested -> gain
                else -> null
            }
        val fleaFlicker = gain >= FLEA_MIN_GAIN && random.chance(FLEA_CHANCE)
        val catchKind = if (scoringDepth == null) random.pick(CatchKind.entries.toList()) else CatchKind.WRAPPED_UP
        val tippedCatch = scoringDepth == null && gain >= TIPPED_MIN_GAIN && random.chance(TIPPED_CHANCE)
        val catchDepth =
            when {
                fleaFlicker -> gain
                scoringDepth != null -> scoringDepth
                else -> catchDepthFor(catchKind, gain, random)
            }
        val concept = PassConcept(context, scene, fleaFlicker)
        val targetIndex = concept.target(deep = catchDepth > 9f, random = random)
        val targetStart = scene.offense[targetIndex]
        val endLateral = (targetStart.lateral * 0.45f).coerceIn(-18f, 18f)
        val catchLateral = if (catchDepth >= gain - 0.01f) endLateral else targetStart.lateral * 0.6f
        val catchPoint = FieldPoint(context.lineOfScrimmage + forward * catchDepth, catchLateral)
        val endPoint = FieldPoint(context.endSpot, endLateral)

        val throwAt = concept.throwAt
        val catchAt = throwAt + concept.flightTime(catchPoint)
        val afterCatch = catchPoint.distanceTo(endPoint)
        val tackleAt = minOf(SCORE_AT, maxOf(catchAt + MIN_CONTEST_TIME, catchAt + CATCH_TO_STRIDE + carryTime(afterCatch)))
        val deepestDefender = scene.defense.maxOf { (it.along - context.lineOfScrimmage) * forward }
        val clearAlong = context.lineOfScrimmage + forward * (deepestDefender + CLEAR_MARGIN)
        val runAfterCatch =
            WeavingRun.between(catchPoint, endPoint, catchAt, tackleAt, context.side, clearAlong, scene.defense.map { hold(it) })
        val receiver =
            switchAt(catchAt, concept.routeTo(targetIndex, catchPoint - carryOffset(forward), catchAt), carrierOf(runAfterCatch, forward))

        val runPace = afterCatch / maxOf(tackleAt - catchAt, MIN_RUN_TIME)
        val escortSpeed = Pursuit.paced(maxOf(Pursuit.DEFENSIVE_BACK_SPEED, runPace * ESCORT_PACE))
        val quarterback = concept.throwingQuarterback()
        val backIndex = scene.offensiveAlignment.backs.first()
        val fleaBack = if (fleaFlicker) concept.fleaBack(backIndex) else null
        val overrides =
            if (fleaBack != null) mapOf(targetIndex to receiver, backIndex to fleaBack) else mapOf(targetIndex to receiver)
        val blocking = concept.offense(quarterback, overrides, conceptDepth = catchDepth)
        val offense =
            DownfieldEscort.follow(
                before = blocking,
                carrier = receiver,
                from = if (offenseScores) SNAP_END else throwAt,
                until = tackleAt,
                exclude = setOf(targetIndex, scene.offensiveAlignment.quarterback),
                speed = { if (it in OffensiveAlignments.LINEMEN) Pursuit.LINEMAN_SPEED else escortSpeed },
            )
        val tacklers =
            when {
                offenseScores -> 0
                afterCatch < 2f -> 3
                else -> 2
            }
        val before = DefensiveReaction.before(context, scene, offense, dropping = true)
        val responded =
            DefensiveReaction.respond(
                scene,
                before,
                receiver,
                forward,
                catchAt,
                tackleAt,
                tacklers,
                linemenCanTackle = catchDepth <= 2f,
            )
        val covered = offenseScores && scoreKind.covered
        val shadowTrail = if (covered) scoreKind.coverTrail else COVER_TRAIL
        val shadowShoulder = if (covered) scoreKind.coverShoulder else COVER_SHOULDER
        val defense =
            if (contested || covered) {
                coverTarget(scene, before, responded, receiver, throwAt, catchAt, forward, shadowTrail, shadowShoulder)
            } else {
                responded
            }

        val held = if (fleaBack != null) concept.fleaHeldBall(quarterback, fleaBack) else concept.heldBall(quarterback)
        val ball =
            BallTrack { progress ->
                when {
                    progress < throwAt -> held.at(progress)
                    progress < catchAt && tippedCatch ->
                        concept.tipped(
                            concept.tipPointFor(catchPoint, context.side * TIP_SWING),
                            catchPoint,
                            segment(progress, throwAt, catchAt),
                        )
                    progress < catchAt -> concept.thrown(catchPoint, segment(progress, throwAt, catchAt))
                    else -> BallState(runAfterCatch.at(progress))
                }
            }
        val settled = minOf(1f, tackleAt + TACKLE_SETTLE)
        return Choreography(offense, defense, ball, facingLocked = facingLocked(scene), endsAt = settled)
    }

    private fun coverTarget(
        scene: ScrimmageScene,
        before: List<Track>,
        responded: List<Track>,
        receiver: Track,
        throwAt: Float,
        catchAt: Float,
        forward: Float,
        trail: Float,
        shoulder: Float,
    ): List<Track> {
        val catchSpot = receiver.at(catchAt)
        val cover = Pursuit.closest(before.map { it.at(throwAt) }, scene.defensiveAlignment.secondary, catchSpot, 1).first()
        val needed = before[cover].at(SNAP_END).distanceTo(catchSpot) / maxOf(catchAt - SNAP_END, MIN_COVER_TIME)
        val speed = Pursuit.paced(maxOf(Pursuit.COVERAGE_SPEED, needed * COVER_PACE))
        val shadow =
            Pursuit.chase(before[cover], SNAP_END, speed) { progress, _ ->
                receiver.at(progress) + FieldPoint(-forward * trail, shoulder)
            }
        return responded.mapIndexed { index, track -> if (index == cover) shadow else track }
    }

    /**
     * Splits the gain between the throw and the run after it. The share comes from [CatchKind] so
     * the same play can be caught at the sticks and tackled on the spot, or caught short and run
     * the rest of the way. A loss is always caught where it ends — a receiver does not run
     * backwards for yardage.
     */
    private fun catchDepthFor(
        kind: CatchKind,
        gain: Float,
        random: PlayRandom,
    ): Float {
        if (gain <= MIN_RUNNABLE_GAIN) return gain
        val share = random.between(kind.minShare, kind.maxShare)
        return (gain * (1f - share)).coerceIn(MIN_CATCH_ABOVE_LINE, gain)
    }

    private fun deepScoreKind(
        gain: Float,
        random: PlayRandom,
    ): DeepScoreKind = random.pick(DeepScoreKind.entries.filter { it != DeepScoreKind.HAIL_MARY || gain <= HAIL_MARY_MAX_GAIN })

    private fun scoringCatchDepth(
        kind: DeepScoreKind,
        gain: Float,
        random: PlayRandom,
    ): Float =
        when (kind) {
            DeepScoreKind.HAIL_MARY -> gain
            DeepScoreKind.WIDE_OPEN -> BREAKAWAY_CATCH_DEPTH
            DeepScoreKind.IN_STRIDE ->
                random.between(IN_STRIDE_MIN_DEPTH, maxOf(IN_STRIDE_MIN_DEPTH, minOf(IN_STRIDE_MAX_DEPTH, gain - MIN_AFTER_CATCH)))
        }

    companion object {
        internal fun facingLocked(scene: ScrimmageScene): Set<Int> =
            OffensiveAlignments.LINEMEN.toSet() + scene.offensiveAlignment.quarterback

        private val OFFENSIVE_SCORES = setOf(ActualResult.TOUCHDOWN, ActualResult.SUCCESS)

        private const val MIN_RUNNABLE_GAIN = 3f
        private const val MIN_CATCH_ABOVE_LINE = 1.5f
        private const val TIPPED_MIN_GAIN = 6f
        private const val TIPPED_CHANCE = 0.12f
        private const val TIP_SWING = 2.2f

        private const val MIN_CONTEST_TIME = 0.12f

        private const val MIN_RUN_TIME = 0.05f
        private const val COVER_TRAIL = 0.9f
        private const val COVER_SHOULDER = 1.3f
        private const val CLEAR_MARGIN = 5f

        private const val TACKLE_SETTLE = 0.05f
        private const val ESCORT_PACE = 0.9f
        private const val SHORT_SCORE = 17f
        private const val BREAKAWAY_CATCH_DEPTH = 15f
        private const val DEEP_CATCH_DEPTH = 12f
        private const val RUN_AFTER_CATCH = 3f
        private const val CATCH_TO_STRIDE = 0.06f

        private const val HAIL_MARY_MAX_GAIN = 55f
        private const val IN_STRIDE_MIN_DEPTH = 20f
        private const val IN_STRIDE_MAX_DEPTH = 40f

        private const val FLEA_MIN_GAIN = 25f
        private const val FLEA_CHANCE = 0.1f

        private const val MIN_COVER_TIME = 0.05f
        private const val COVER_PACE = 1.15f
        private const val CONTESTED_MIN_GAIN = 20f
        private const val CONTESTED_CHANCE = 0.45f
        private const val MIN_CATCH_DEPTH = 10f
        private const val MAX_CATCH_DEPTH = 30f
        private const val MIN_AFTER_CATCH = 5f
    }
}
