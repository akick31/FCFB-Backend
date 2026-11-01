package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.team.TeamSide
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameRulesTest {
    @Test
    fun `scoring team trailing by two after final touchdown can still tie with two point conversion`() {
        assertFalse(
            GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 24, 22),
        )
        assertFalse(
            GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.HOME, ActualResult.TOUCHDOWN, 22, 24),
        )
    }

    @Test
    fun `defensive touchdown credits the opposite side when checking the margin`() {
        assertFalse(
            GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.HOME, ActualResult.TURNOVER_TOUCHDOWN, 24, 22),
        )
        assertFalse(
            GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.KICK_SIX, 22, 24),
        )
    }

    @Test
    fun `scoring team trailing by one or tied still plays the extra point`() {
        assertFalse(GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 21, 20))
        assertFalse(GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 20, 20))
    }

    @Test
    fun `scoring team leading by two or more ends the game`() {
        assertTrue(GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.HOME, ActualResult.TOUCHDOWN, 24, 22))
        assertTrue(GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 20, 28))
    }

    @Test
    fun `scoring team still trailing by three or more ends the game`() {
        assertTrue(GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 27, 24))
        assertTrue(GameRules.isRegulationOverAfterFinalTouchdown(TeamSide.HOME, ActualResult.TOUCHDOWN, 20, 28))
    }
}
