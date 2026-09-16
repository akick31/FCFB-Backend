package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
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
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.Track
import com.fcfb.arceus.service.fcfb.animation.choreography.WeavingRun
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.carryTime
import com.fcfb.arceus.service.fcfb.animation.choreography.offsetBy
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

class CompletedPassScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context)
        val concept = PassConcept(context, scene)
        val forward = context.forward
        val offenseScores = context.play.actualResult == ActualResult.TOUCHDOWN
        val gain = context.gain
        val random = PlayRandom(context.play)
        val contested = !offenseScores && gain > CONTESTED_MIN_GAIN && random.chance(CONTESTED_CHANCE)
        val catchDepth =
            when {
                offenseScores && gain <= SHORT_SCORE -> gain
                offenseScores -> BREAKAWAY_CATCH_DEPTH
                gain <= 3f -> gain
                contested -> gain
                gain > CONTESTED_MIN_GAIN -> random.between(MIN_CATCH_DEPTH, minOf(MAX_CATCH_DEPTH, gain - MIN_AFTER_CATCH))
                gain <= BREAKAWAY_CATCH_DEPTH -> maxOf(gain - RUN_AFTER_CATCH, 2f)
                else -> DEEP_CATCH_DEPTH
            }
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
        val runAfterCatch = WeavingRun.between(catchPoint, endPoint, catchAt, tackleAt, context.side, clearAlong)
        val receiver =
            switchAt(catchAt, concept.routeTo(targetIndex, catchPoint - carryOffset(forward), catchAt), carrierOf(runAfterCatch, forward))

        val runPace = afterCatch / maxOf(tackleAt - catchAt, MIN_RUN_TIME)
        val escortSpeed = maxOf(Pursuit.DEFENSIVE_BACK_SPEED, runPace * ESCORT_PACE)
        val quarterback = concept.throwingQuarterback()
        val blocking = concept.offense(quarterback, mapOf(targetIndex to receiver))
        val offense =
            DownfieldEscort.follow(
                before = blocking,
                carrier = receiver,
                from = throwAt,
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
        val defense = if (contested) coverTarget(scene, before, responded, receiver, throwAt, catchAt, forward) else responded

        val held = concept.heldBall(quarterback)
        val ball =
            BallTrack { progress ->
                when {
                    progress < throwAt -> held.at(progress)
                    progress < catchAt -> concept.thrown(catchPoint, segment(progress, throwAt, catchAt))
                    else -> BallState(runAfterCatch.at(progress))
                }
            }
        val settled = minOf(1f, tackleAt + TACKLE_SETTLE)
        return Choreography(offense, defense, ball, facingLocked = facingLocked(scene), endsAt = settled)
    }

    /**
     * A contested catch is only contested if the defender is stride for stride the whole way, so he simply runs the
     * receiver's route a step off his hip rather than trying to close from coverage depth once the ball is thrown.
     */
    private fun coverTarget(
        scene: ScrimmageScene,
        before: List<Track>,
        responded: List<Track>,
        receiver: Track,
        throwAt: Float,
        catchAt: Float,
        forward: Float,
    ): List<Track> {
        val catchSpot = receiver.at(catchAt)
        val cover = Pursuit.closest(before.map { it.at(throwAt) }, scene.defensiveAlignment.secondary, catchSpot, 1).first()
        val shadow = receiver.offsetBy(FieldPoint(-forward * COVER_TRAIL, COVER_SHOULDER))
        return responded.mapIndexed { index, track -> if (index == cover) shadow else track }
    }

    companion object {
        /** Linemen set back into pass protection, so like the quarterback they keep facing the defense rather than turning. */
        internal fun facingLocked(scene: ScrimmageScene): Set<Int> =
            OffensiveAlignments.LINEMEN.toSet() + scene.offensiveAlignment.quarterback

        /** A catch with no run after it still needs long enough for a defender to close and make the tackle. */
        private const val MIN_CONTEST_TIME = 0.12f

        private const val MIN_RUN_TIME = 0.05f
        private const val COVER_TRAIL = 0.9f
        private const val COVER_SHOULDER = 1.3f
        private const val CLEAR_MARGIN = 5f

        /** A beat after the tackle so the whistle lands before the result banner, rather than cutting instantly. */
        private const val TACKLE_SETTLE = 0.05f
        private const val ESCORT_PACE = 0.9f
        private const val SHORT_SCORE = 17f
        private const val BREAKAWAY_CATCH_DEPTH = 15f
        private const val DEEP_CATCH_DEPTH = 12f
        private const val RUN_AFTER_CATCH = 3f
        private const val CATCH_TO_STRIDE = 0.06f

        /** Long gains are either caught deep and tackled right there, or caught shorter and run down from behind. */
        private const val CONTESTED_MIN_GAIN = 20f
        private const val CONTESTED_CHANCE = 0.45f
        private const val MIN_CATCH_DEPTH = 10f
        private const val MAX_CATCH_DEPTH = 30f
        private const val MIN_AFTER_CATCH = 5f
    }
}
