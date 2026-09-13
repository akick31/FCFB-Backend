package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.model.Play
import org.springframework.stereotype.Component

@Component
class PlayAnimationClassifier {
    fun classifyShape(play: Play): ShapeFamily {
        return when (play.actualResult) {
            ActualResult.RETURN_TOUCHDOWN,
            ActualResult.KICKING_TEAM_TOUCHDOWN,
            ActualResult.KICK_SIX,
            ActualResult.PUNT_RETURN_TOUCHDOWN,
            ActualResult.PUNT_TEAM_TOUCHDOWN,
            ActualResult.TURNOVER_TOUCHDOWN,
            ActualResult.DEFENSE_TWO_POINT,
            ActualResult.MUFFED_KICK,
            ActualResult.MUFFED_PUNT,
            -> ShapeFamily.RETURN_ZIGZAG
            ActualResult.END_OF_HALF,
            ActualResult.END_OF_GAME,
            -> ShapeFamily.STATIC_SNAP
            else -> classifyShapeByPlayCall(play.playCall)
        }
    }

    private fun classifyShapeByPlayCall(playCall: PlayCall?): ShapeFamily {
        return when (playCall) {
            PlayCall.RUN, PlayCall.TWO_POINT -> ShapeFamily.RUSH_ARC
            PlayCall.PASS -> ShapeFamily.PASS_ARC
            PlayCall.FIELD_GOAL, PlayCall.PAT, PlayCall.PUNT, PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_SQUIB -> ShapeFamily.KICK_ARC
            PlayCall.KICKOFF_ONSIDE -> ShapeFamily.ONSIDE_SCRAMBLE
            PlayCall.SPIKE, PlayCall.KNEEL -> ShapeFamily.STATIC_SNAP
            null -> ShapeFamily.RUSH_ARC
        }
    }
}
