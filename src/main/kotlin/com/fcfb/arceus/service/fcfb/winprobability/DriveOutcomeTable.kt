package com.fcfb.arceus.service.fcfb.winprobability

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fcfb.arceus.model.winprobability.DriveOutcomeProbabilities
import com.fcfb.arceus.model.winprobability.DriveOutcomeTableData
import org.springframework.stereotype.Component

@Component
class DriveOutcomeTable {
    private val data: DriveOutcomeTableData =
        javaClass.classLoader.getResourceAsStream(RESOURCE_NAME)!!.use { jacksonObjectMapper().readValue(it) }

    val patSuccessRate: Double get() = data.patSuccessRate
    val twoPointSuccessRate: Double get() = data.twoPointSuccessRate
    val touchdownNeededFieldGoalConversion: Double get() = data.touchdownNeededFieldGoalConversion
    val fieldGoalSufficientNoScoreReduction: Double get() = data.fieldGoalSufficientNoScoreReduction

    fun outcomesFrom(
        down: Int,
        distance: Int,
        yardsToGoal: Int,
    ): DriveOutcomeProbabilities {
        val key =
            listOf(
                down,
                bucketIndex(minOf(distance, yardsToGoal), data.distanceBounds),
                bucketIndex(yardsToGoal, data.yardsToGoalBounds),
            ).joinToString(":")
        return data.cells.getValue(key)
    }

    private fun bucketIndex(
        value: Int,
        bounds: List<Int>,
    ) = bounds.indexOfFirst { value <= it }

    companion object {
        private const val RESOURCE_NAME = "drive_outcomes.json"
    }
}
