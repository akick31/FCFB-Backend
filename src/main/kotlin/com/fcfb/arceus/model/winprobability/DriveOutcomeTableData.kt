package com.fcfb.arceus.model.winprobability

data class DriveOutcomeTableData(
    val yardsToGoalBounds: List<Int>,
    val distanceBounds: List<Int>,
    val patSuccessRate: Double,
    val twoPointSuccessRate: Double,
    val touchdownNeededFieldGoalConversion: Double,
    val fieldGoalSufficientNoScoreReduction: Double,
    val cells: Map<String, DriveOutcomeProbabilities>,
)
