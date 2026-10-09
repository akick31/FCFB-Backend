package com.fcfb.arceus.service.fcfb.gamestats

object StatsAverageCalculator {
    fun average(
        total: Int,
        count: Int,
    ): Double = if (count > 0) total.toDouble() / count else 0.0
}
