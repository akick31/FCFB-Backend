package com.fcfb.arceus.util.ranking

class RatingNormalizer {
    fun normalize(valuesByTeamId: Map<Int, Double>): Map<Int, Double> {
        if (valuesByTeamId.isEmpty()) return emptyMap()
        val min = valuesByTeamId.values.min()
        val max = valuesByTeamId.values.max()
        if (min == max) return valuesByTeamId.mapValues { NEUTRAL_RATING }
        return valuesByTeamId.mapValues { (_, value) -> (value - min) / (max - min) * RATING_SCALE }
    }

    companion object {
        private const val NEUTRAL_RATING = 50.0
        private const val RATING_SCALE = 100
    }
}
