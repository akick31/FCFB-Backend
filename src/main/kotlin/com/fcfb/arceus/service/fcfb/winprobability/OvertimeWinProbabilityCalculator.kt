package com.fcfb.arceus.service.fcfb.winprobability

import com.fcfb.arceus.model.winprobability.DriveOutcomeProbabilities
import com.fcfb.arceus.model.winprobability.OvertimeState
import org.springframework.stereotype.Component

@Component
class OvertimeWinProbabilityCalculator(
    private val driveOutcomeTable: DriveOutcomeTable,
) {
    private val freshPossession = driveOutcomeTable.outcomesFrom(1, 10, POSSESSION_START_YARDS_TO_GOAL)
    private val forcedTwoPointPeriodValue = solveForcedTwoPointPeriodValue()
    private val secondPeriodValue = firstPossessionValue(2, freshPossession, forcedTwoPointPeriodValue)

    fun winProbability(state: OvertimeState): Double {
        val next = tiedNextPeriodValue(state.period)
        return when {
            state.offenseHasFirstPossession && state.conversionPending ->
                firstTeamConversionValue(state.period, state.offenseMargin, next)
            state.offenseHasFirstPossession ->
                firstPossessionValue(state.period, outcomesFrom(state), next)
            state.conversionPending ->
                secondTeamConversionValue(state.period, state.offenseMargin, next)
            else ->
                secondPossessionValue(state.period, state.offenseMargin, outcomesFrom(state), next)
        }
    }

    private fun outcomesFrom(state: OvertimeState) = driveOutcomeTable.outcomesFrom(state.down, state.distance, state.yardsToGoal)

    private fun tiedNextPeriodValue(period: Int) =
        if (period + 1 >= FORCED_TWO_POINT_PERIOD) forcedTwoPointPeriodValue else secondPeriodValue

    private fun solveForcedTwoPointPeriodValue(): Double {
        var value = 0.5
        repeat(FIXED_POINT_ITERATIONS) { value = firstPossessionValue(FORCED_TWO_POINT_PERIOD, freshPossession, value) }
        return value
    }

    private fun firstPossessionValue(
        period: Int,
        outcomes: DriveOutcomeProbabilities,
        next: Double,
    ) = outcomes.touchdown * firstTeamConversionValue(period, TOUCHDOWN_POINTS, next) +
        outcomes.fieldGoal * (1.0 - secondPossessionValue(period, -FIELD_GOAL_POINTS, freshPossession, next)) +
        outcomes.noScore * (1.0 - secondPossessionValue(period, 0, freshPossession, next))

    private fun firstTeamConversionValue(
        period: Int,
        margin: Int,
        next: Double,
    ) = conversionOptions(period).maxOf { (successRate, points) ->
        successRate * (1.0 - secondPossessionValue(period, -(margin + points), freshPossession, next)) +
            (1.0 - successRate) * (1.0 - secondPossessionValue(period, -margin, freshPossession, next))
    }

    private fun secondPossessionValue(
        period: Int,
        margin: Int,
        outcomes: DriveOutcomeProbabilities,
        next: Double,
    ): Double {
        val playingToNeed = outcomesPlayingToNeed(margin, outcomes)
        return playingToNeed.touchdown * secondTeamTouchdownValue(period, margin + TOUCHDOWN_POINTS, next) +
            playingToNeed.fieldGoal * periodResultForSecondTeam(margin + FIELD_GOAL_POINTS, next) +
            playingToNeed.noScore * periodResultForSecondTeam(margin, next)
    }

    private fun outcomesPlayingToNeed(
        margin: Int,
        outcomes: DriveOutcomeProbabilities,
    ) = if (margin + FIELD_GOAL_POINTS < 0) {
        outcomes.copy(
            touchdown = outcomes.touchdown + driveOutcomeTable.touchdownNeededFieldGoalConversion * outcomes.fieldGoal,
            fieldGoal = 0.0,
        )
    } else {
        outcomes.copy(fieldGoal = outcomes.fieldGoal + driveOutcomeTable.fieldGoalSufficientNoScoreReduction * outcomes.noScore)
    }

    private fun secondTeamTouchdownValue(
        period: Int,
        margin: Int,
        next: Double,
    ) = when {
        margin > MAX_CONVERSION_POINTS -> 1.0
        margin < -MAX_CONVERSION_POINTS -> 0.0
        else -> secondTeamConversionValue(period, margin, next)
    }

    private fun secondTeamConversionValue(
        period: Int,
        margin: Int,
        next: Double,
    ) = conversionOptions(period).maxOf { (successRate, points) ->
        successRate * periodResultForSecondTeam(margin + points, next) +
            (1.0 - successRate) * periodResultForSecondTeam(margin, next)
    }

    private fun periodResultForSecondTeam(
        margin: Int,
        next: Double,
    ) = when {
        margin > 0 -> 1.0
        margin < 0 -> 0.0
        else -> next
    }

    private fun conversionOptions(period: Int): List<Pair<Double, Int>> {
        val twoPoint = driveOutcomeTable.twoPointSuccessRate to TWO_POINT_POINTS
        return if (period >= FORCED_TWO_POINT_PERIOD) listOf(twoPoint) else listOf(driveOutcomeTable.patSuccessRate to PAT_POINTS, twoPoint)
    }

    companion object {
        private const val POSSESSION_START_YARDS_TO_GOAL = 25
        private const val FORCED_TWO_POINT_PERIOD = 3
        private const val FIXED_POINT_ITERATIONS = 100
        private const val TOUCHDOWN_POINTS = 6
        private const val FIELD_GOAL_POINTS = 3
        private const val PAT_POINTS = 1
        private const val TWO_POINT_POINTS = 2
        private const val MAX_CONVERSION_POINTS = 2
    }
}
