package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.DefensiveReaction
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.bounce
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

class IncompletePassScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context)
        val concept = PassConcept(context, scene)
        val forward = context.forward
        val catchDepth = if (context.play.playId % 2 == 0) SHORT_TARGET else DEEP_TARGET
        val targetIndex = concept.target(deep = catchDepth == DEEP_TARGET)
        val catchPoint = FieldPoint(context.lineOfScrimmage + forward * catchDepth, scene.offense[targetIndex].lateral * 0.6f)
        val catchMarker = catchPoint - carryOffset(forward)
        val throwAt = concept.throwAt
        val arriveAt = throwAt + concept.flightTime(catchPoint)

        val receiver =
            switchAt(
                arriveAt,
                concept.routeTo(targetIndex, catchMarker, arriveAt),
                path(arriveAt to catchMarker, 0.9f to catchMarker + FieldPoint(forward * 2f, 0f)),
            )
        val quarterback = concept.throwingQuarterback()
        val offense = concept.offense(quarterback, mapOf(targetIndex to receiver))
        val before = DefensiveReaction.before(context, scene, offense, dropping = true)
        val breakupAt = arriveAt - BREAK_ON_BALL
        val defender =
            Pursuit.closest(before.map { it.at(breakupAt) }, scene.defensiveAlignment.secondary, catchPoint, 1).first()
        val defense =
            before.mapIndexed { index, track ->
                if (index == defender) {
                    Pursuit.chase(track, breakupAt, Pursuit.COVERAGE_SPEED, Pursuit.toward(catchPoint + FieldPoint(forward, 1f)))
                } else {
                    track
                }
            }

        val held = concept.heldBall(quarterback)
        val restSpot = catchPoint + FieldPoint(forward * 3f, context.side * 1.5f)
        val ball =
            BallTrack { progress ->
                when {
                    progress < throwAt -> held.at(progress)
                    progress < arriveAt -> concept.thrown(catchPoint, segment(progress, throwAt, arriveAt))
                    else -> {
                        val fraction = segment(progress, arriveAt, arriveAt + FALL_TIME)
                        BallState(catchPoint.lerp(restSpot, fraction), bounce(fraction, 1.8f, 3))
                    }
                }
            }
        return Choreography(offense, defense, ball)
    }

    companion object {
        private const val SHORT_TARGET = 8f
        private const val DEEP_TARGET = 13f
        private const val BREAK_ON_BALL = 0.2f
        private const val FALL_TIME = 0.25f
    }
}
