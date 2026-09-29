package com.fcfb.arceus.service.fcfb.elo

import kotlin.math.pow

object EloCalculator {
    const val BASE_ELO = 1500.0
    const val K_FACTOR = 32.0

    fun expectedScore(
        rating: Double,
        opponentRating: Double,
    ): Double = 1.0 / (1.0 + 10.0.pow((opponentRating - rating) / 400.0))

    fun updatedRatings(
        homeElo: Double,
        awayElo: Double,
        homeWon: Boolean,
    ): Pair<Double, Double> {
        val expectedHome = expectedScore(homeElo, awayElo)
        val actualHome = if (homeWon) 1.0 else 0.0
        val delta = K_FACTOR * (actualHome - expectedHome)
        return (homeElo + delta) to (awayElo - delta)
    }
}
