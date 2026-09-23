package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.model.Play
import org.springframework.stereotype.Component

@Component
class PlayAnimationClassifier {
    fun classify(play: Play): AnimatedPlayType {
        if (play.actualResult in DEAD_BALL_RESULTS) return AnimatedPlayType.DEAD_BALL
        return when (play.playCall) {
            PlayCall.RUN, PlayCall.TWO_POINT -> AnimatedPlayType.RUN
            PlayCall.PASS -> AnimatedPlayType.PASS
            PlayCall.KNEEL -> AnimatedPlayType.KNEEL
            PlayCall.SPIKE -> AnimatedPlayType.SPIKE
            PlayCall.PUNT -> if (play.actualResult == ActualResult.BLOCKED) AnimatedPlayType.BLOCKED_KICK else AnimatedPlayType.PUNT
            PlayCall.FIELD_GOAL, PlayCall.PAT ->
                when {
                    play.actualResult != ActualResult.KICK_SIX -> AnimatedPlayType.FIELD_GOAL
                    KickDistance.fallsShort(play) -> AnimatedPlayType.SHORT_KICK_RETURN
                    else -> AnimatedPlayType.BLOCKED_KICK
                }
            PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_SQUIB -> AnimatedPlayType.KICKOFF
            PlayCall.KICKOFF_ONSIDE -> AnimatedPlayType.ONSIDE_KICK
            null -> AnimatedPlayType.DEAD_BALL
        }
    }

    companion object {
        private val DEAD_BALL_RESULTS = setOf(ActualResult.DELAY_OF_GAME, ActualResult.END_OF_HALF, ActualResult.END_OF_GAME)
    }
}
