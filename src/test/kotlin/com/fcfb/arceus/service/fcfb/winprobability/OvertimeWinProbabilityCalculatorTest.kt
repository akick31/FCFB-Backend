package com.fcfb.arceus.service.fcfb.winprobability

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.winprobability.DriveOutcomeProbabilities
import com.fcfb.arceus.model.winprobability.OvertimeState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OvertimeWinProbabilityCalculatorTest {
    private fun calculator(
        fresh: DriveOutcomeProbabilities,
        current: DriveOutcomeProbabilities = fresh,
        patRate: Double = 1.0,
        twoPointRate: Double = 0.5,
        touchdownNeededConversion: Double = 0.0,
        noScoreReduction: Double = 0.0,
    ): OvertimeWinProbabilityCalculator {
        val table = mockk<DriveOutcomeTable>()
        every { table.outcomesFrom(1, 10, 25) } returns fresh
        every { table.outcomesFrom(neq(1), any(), any()) } returns current
        every { table.patSuccessRate } returns patRate
        every { table.twoPointSuccessRate } returns twoPointRate
        every { table.touchdownNeededFieldGoalConversion } returns touchdownNeededConversion
        every { table.fieldGoalSufficientNoScoreReduction } returns noScoreReduction
        return OvertimeWinProbabilityCalculator(table)
    }

    private fun state(
        period: Int = 1,
        first: Boolean,
        margin: Int = 0,
        conversionPending: Boolean = false,
        down: Int = 1,
    ) = OvertimeState(TeamSide.HOME, period, first, margin, conversionPending, down, 10, 25)

    private val even = DriveOutcomeProbabilities(touchdown = 0.5, fieldGoal = 0.25, defensiveTouchdown = 0.0)

    @Test
    fun `second team tying touchdown with a certain PAT goes to the next period`() {
        val calc = calculator(even, twoPointRate = 0.0)
        val tiedNextPeriodAsFirstTeam = calc.winProbability(state(period = 2, first = true))

        assertEquals(tiedNextPeriodAsFirstTeam, calc.winProbability(state(first = false, margin = -1, conversionPending = true)), 1e-9)
    }

    @Test
    fun `second team goes for two to win when two point odds beat the tied period`() {
        val calc = calculator(even, twoPointRate = 0.9)

        assertEquals(0.9, calc.winProbability(state(first = false, margin = -1, conversionPending = true)), 1e-9)
    }

    @Test
    fun `second team touchdown after a first team field goal wins outright`() {
        val calc = calculator(DriveOutcomeProbabilities(touchdown = 1.0, fieldGoal = 0.0, defensiveTouchdown = 0.0))

        assertEquals(1.0, calc.winProbability(state(first = false, margin = -3)), 1e-9)
    }

    @Test
    fun `second team trailing loses when its possession cannot score`() {
        val calc = calculator(even, current = DriveOutcomeProbabilities(0.0, 0.0, 0.0))

        assertEquals(0.0, calc.winProbability(state(first = false, margin = -3, down = 4)), 1e-9)
    }

    @Test
    fun `second team needing a touchdown turns field goal drives into touchdown attempts`() {
        val calc = calculator(even, current = even, touchdownNeededConversion = 0.6)

        assertEquals(0.5 + 0.6 * 0.25, calc.winProbability(state(first = false, margin = -4, down = 2)), 1e-9)
    }

    @Test
    fun `second team needing only a field goal comes up empty less often`() {
        val calc = calculator(even, current = DriveOutcomeProbabilities(0.0, 0.0, 0.0), noScoreReduction = 0.4)

        val tiedNextPeriodAsFirstTeam = calc.winProbability(state(period = 2, first = true))

        assertEquals(0.4 + 0.6 * tiedNextPeriodAsFirstTeam, calc.winProbability(state(first = false, margin = 0, down = 2)), 1e-9)
    }

    @Test
    fun `third overtime forces a two point try after a touchdown`() {
        val calc = calculator(even, patRate = 1.0, twoPointRate = 0.0)

        assertEquals(0.0, calc.winProbability(state(period = 3, first = false, margin = -1, conversionPending = true)), 1e-9)
        assertEquals(0.0, calc.winProbability(state(period = 4, first = false, margin = -1, conversionPending = true)), 1e-9)
    }

    @Test
    fun `forced two point periods share one tied value`() {
        val calc = calculator(even)

        assertEquals(calc.winProbability(state(period = 3, first = true)), calc.winProbability(state(period = 7, first = true)), 1e-9)
    }

    @Test
    fun `first team is ahead but not safe after scoring`() {
        val calc = calculator(even)
        val tied = calc.winProbability(state(first = true))
        val afterTouchdownAndPat = 1.0 - calc.winProbability(state(first = false, margin = -7))

        assertTrue(tied in 0.35..0.65)
        assertTrue(afterTouchdownAndPat in 0.6..0.9)
    }
}
