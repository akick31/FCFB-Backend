package com.fcfb.arceus.domain.game

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.team.TeamSide
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameConditionsTest {
    private val conditions = GameConditions()

    @Test
    fun `scoring team trailing by two after final touchdown can still tie with two point conversion`() {
        assertFalse(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 24, 22))
        assertFalse(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.HOME, ActualResult.TOUCHDOWN, 22, 24))
    }

    @Test
    fun `defensive touchdown credits the opposite side when checking the margin`() {
        assertFalse(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.HOME, ActualResult.TURNOVER_TOUCHDOWN, 24, 22))
        assertFalse(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.KICK_SIX, 22, 24))
    }

    @Test
    fun `scoring team trailing by one or tied still plays the extra point`() {
        assertFalse(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 21, 20))
        assertFalse(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 20, 20))
    }

    @Test
    fun `scoring team leading by two or more ends the game`() {
        assertTrue(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.HOME, ActualResult.TOUCHDOWN, 24, 22))
        assertTrue(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 20, 28))
    }

    @Test
    fun `scoring team still trailing by three or more ends the game`() {
        assertTrue(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.AWAY, ActualResult.TOUCHDOWN, 27, 24))
        assertTrue(conditions.isRegulationOverAfterFinalTouchdown(TeamSide.HOME, ActualResult.TOUCHDOWN, 20, 28))
    }
}
