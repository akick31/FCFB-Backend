package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.model.Play
import org.springframework.stereotype.Component

@Component
class PlayOutcomeOverlayClassifier {
    fun classifyOverlay(play: Play): OverlayType {
        if (play.result == Scenario.TOUCHBACK) return OverlayType.TOUCHBACK
        return when (play.actualResult) {
            ActualResult.TOUCHDOWN,
            ActualResult.TURNOVER_TOUCHDOWN,
            ActualResult.RETURN_TOUCHDOWN,
            ActualResult.KICKING_TEAM_TOUCHDOWN,
            ActualResult.KICK_SIX,
            ActualResult.PUNT_RETURN_TOUCHDOWN,
            ActualResult.PUNT_TEAM_TOUCHDOWN,
            -> OverlayType.TOUCHDOWN_FLASH
            ActualResult.FIRST_DOWN -> OverlayType.FIRST_DOWN_MARKER
            ActualResult.TURNOVER, ActualResult.TURNOVER_ON_DOWNS -> OverlayType.TURNOVER_FLAG
            ActualResult.SAFETY -> OverlayType.SAFETY_FLASH
            ActualResult.GOOD -> OverlayType.KICK_GOOD
            ActualResult.NO_GOOD -> OverlayType.KICK_NO_GOOD
            ActualResult.BLOCKED -> OverlayType.KICK_BLOCKED
            ActualResult.MUFFED_KICK, ActualResult.MUFFED_PUNT -> OverlayType.MUFFED_BOUNCE
            ActualResult.SUCCESS -> OverlayType.TWO_POINT_SUCCESS
            ActualResult.FAILED -> OverlayType.TWO_POINT_FAILED
            ActualResult.DEFENSE_TWO_POINT -> OverlayType.DEFENSE_TWO_POINT_RETURN
            ActualResult.DELAY_OF_GAME -> OverlayType.FLAG
            ActualResult.SPIKE -> OverlayType.SPIKE_ICON
            ActualResult.KNEEL -> OverlayType.KNEEL_ICON
            ActualResult.END_OF_HALF, ActualResult.END_OF_GAME -> OverlayType.DEAD_PLAY
            else -> OverlayType.NONE
        }
    }
}
