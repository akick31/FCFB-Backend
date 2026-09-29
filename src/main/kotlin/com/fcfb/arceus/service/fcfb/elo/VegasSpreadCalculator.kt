package com.fcfb.arceus.service.fcfb.elo

import kotlin.math.roundToInt

object VegasSpreadCalculator {
    private const val POINTS_PER_HUNDRED_ELO = 3.0
    private const val HOME_FIELD_ADVANTAGE = 2.5

    fun homeSpread(
        homeElo: Double,
        awayElo: Double,
    ): Double {
        val spread = ((homeElo - awayElo) / 100.0) * POINTS_PER_HUNDRED_ELO + HOME_FIELD_ADVANTAGE
        return -((spread * 2).roundToInt() / 2.0)
    }
}
