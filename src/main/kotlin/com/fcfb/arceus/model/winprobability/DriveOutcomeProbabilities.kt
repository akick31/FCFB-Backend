package com.fcfb.arceus.model.winprobability

data class DriveOutcomeProbabilities(
    val touchdown: Double,
    val fieldGoal: Double,
    val defensiveTouchdown: Double,
) {
    val noScore: Double get() = 1.0 - touchdown - fieldGoal - defensiveTouchdown
}
