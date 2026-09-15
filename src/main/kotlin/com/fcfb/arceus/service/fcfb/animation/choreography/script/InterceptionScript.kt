package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.DefensiveReaction
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.GangTackle
import com.fcfb.arceus.service.fcfb.animation.choreography.OffensiveAlignments
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.Pursuit
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.carrierOf
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.switchAt

class InterceptionScript : PlayScript {
    override fun choreograph(context: PlayContext): Choreography {
        val scene = ScrimmageScene.from(context)
        val concept = PassConcept(context, scene)
        val forward = context.forward
        val returnDirection = -forward
        val defenseScores = context.play.actualResult != ActualResult.TURNOVER
        val targetIndex = concept.target(deep = true)

        val nominal = context.lineOfScrimmage + forward * INTERCEPTION_DEPTH
        val returnWouldOvershoot = !defenseScores && (context.endSpot - nominal) * returnDirection < 0f
        val catchAlong = (if (returnWouldOvershoot) context.endSpot + forward * SHORT_RETURN else nominal).coerceIn(1f, 99f)
        val catchPoint = FieldPoint(catchAlong, scene.offense[targetIndex].lateral * 0.6f)
        val endPoint = FieldPoint(context.endSpot, catchPoint.lateral * 0.5f)
        val throwAt = concept.throwAt
        val catchAt = throwAt + concept.flightTime(catchPoint)
        val returnAt =
            if (defenseScores) {
                SCORE_AT
            } else {
                minOf(
                    0.9f,
                    catchAt + 0.08f + 0.3f * minOf(catchPoint.distanceTo(endPoint), 30f) / 30f,
                )
            }
        val returnBall = path(catchAt to catchPoint, returnAt to endPoint)

        val quarterback = concept.throwingQuarterback()
        val lateReceiver = concept.routeTo(targetIndex, catchPoint - carryOffset(forward) - FieldPoint(forward * 1.5f, 0f), catchAt + 0.02f)
        val routes = concept.offense(quarterback, mapOf(targetIndex to lateReceiver))
        val before = DefensiveReaction.before(context, scene, routes, dropping = true)
        val jumpAt = catchAt - JUMP_ROUTE
        val interceptorIndex = Pursuit.closest(before.map { it.at(jumpAt) }, scene.defensiveAlignment.secondary, catchPoint, 1).first()
        val interceptor =
            switchAt(
                catchAt,
                Pursuit.chase(
                    before[interceptorIndex],
                    jumpAt,
                    Pursuit.COVERAGE_SPEED,
                    Pursuit.toward(catchPoint - carryOffset(returnDirection)),
                ),
                carrierOf(returnBall, returnDirection),
            )
        val defense =
            before.mapIndexed { index, track ->
                if (index == interceptorIndex) {
                    interceptor
                } else {
                    Pursuit.chase(
                        track,
                        catchAt + ESCORT_DELAY,
                        Pursuit.LINEBACKER_SPEED,
                        Pursuit.trail(interceptor, ESCORT_RADIUS + index % 3),
                    )
                }
            }
        val offense =
            GangTackle.converge(
                before = routes,
                candidates = routes.indices.filter { it !in OffensiveAlignments.LINEMEN },
                carrier = interceptor,
                direction = returnDirection,
                tackleAt = returnAt,
                tacklers = if (defenseScores) 0 else 2,
                reactAt = { catchAt },
                speed = { if (it in OffensiveAlignments.LINEMEN) Pursuit.LINEMAN_SPEED * 0.6f else Pursuit.DEFENSIVE_BACK_SPEED },
            )

        val held = concept.heldBall(quarterback)
        val ball =
            BallTrack { progress ->
                when {
                    progress < throwAt -> held.at(progress)
                    progress < catchAt -> concept.thrown(catchPoint, segment(progress, throwAt, catchAt))
                    else -> BallState(returnBall.at(progress))
                }
            }
        return Choreography(offense, defense, ball)
    }

    companion object {
        private const val INTERCEPTION_DEPTH = 11f
        private const val SHORT_RETURN = 4f
        private const val JUMP_ROUTE = 0.18f
        private const val ESCORT_DELAY = 0.05f
        private const val ESCORT_RADIUS = 4f
    }
}
