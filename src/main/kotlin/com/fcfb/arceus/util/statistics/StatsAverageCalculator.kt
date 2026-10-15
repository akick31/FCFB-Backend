package com.fcfb.arceus.util.statistics

class StatsAverageCalculator {
    fun average(
        total: Int,
        count: Int,
    ): Double = if (count > 0) total.toDouble() / count else 0.0
}
