package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.DefensiveReaction
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.carryTime
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

class CompletedPassScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context)
        val concept = PassConcept(context, scene)
        val forward = context.forward
        val offenseScores = context.play.actualResult == ActualResult.TOUCHDOWN
        val gain = context.gain
        val catchDepth =
            when {
                offenseScores && gain <= SHORT_SCORE -> gain
                offenseScores -> BREAKAWAY_CATCH_DEPTH
                gain <= 3f -> gain
                gain <= BREAKAWAY_CATCH_DEPTH -> maxOf(gain - RUN_AFTER_CATCH, 2f)
                else -> DEEP_CATCH_DEPTH
            }
        val targetIndex = concept.target(deep = catchDepth > 9f)
        val targetStart = scene.offense[targetIndex]
        val endLateral = (targetStart.lateral * 0.45f).coerceIn(-18f, 18f)
        val catchLateral = if (catchDepth >= gain - 0.01f) endLateral else targetStart.lateral * 0.6f
        val catchPoint = FieldPoint(context.lineOfScrimmage + forward * catchDepth, catchLateral)
        val endPoint = FieldPoint(context.endSpot, endLateral)

        val throwAt = concept.throwAt
        val catchAt = throwAt + concept.flightTime(catchPoint)
        val afterCatch = catchPoint.distanceTo(endPoint)
        val tackleAt = minOf(SCORE_AT, catchAt + CATCH_TO_STRIDE + carryTime(afterCatch))
        val runAfterCatch = path(catchAt to catchPoint, tackleAt to endPoint)
        val receiver =
            switchAt(catchAt, concept.routeTo(targetIndex, catchPoint - carryOffset(forward), catchAt), carrierOf(runAfterCatch, forward))

        val quarterback = concept.throwingQuarterback()
        val offense = concept.offense(quarterback, mapOf(targetIndex to receiver))
        val tacklers =
            when {
                offenseScores -> 0
                afterCatch < 2f -> 3
                else -> 2
            }
        val before = DefensiveReaction.before(context, scene, offense, dropping = true)
        val defense =
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

        val held = concept.heldBall(quarterback)
        val ball =
            BallTrack { progress ->
                when {
                    progress < throwAt -> held.at(progress)
                    progress < catchAt -> concept.thrown(catchPoint, segment(progress, throwAt, catchAt))
                    else -> BallState(runAfterCatch.at(progress))
                }
            }
        return Choreography(offense, defense, ball)
    }

    companion object {
        private const val SHORT_SCORE = 17f
        private const val BREAKAWAY_CATCH_DEPTH = 15f
        private const val DEEP_CATCH_DEPTH = 12f
        private const val RUN_AFTER_CATCH = 3f
        private const val CATCH_TO_STRIDE = 0.06f
    }
}
