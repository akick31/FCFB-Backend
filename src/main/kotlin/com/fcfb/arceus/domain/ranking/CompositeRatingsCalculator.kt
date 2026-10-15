package com.fcfb.arceus.domain.ranking

import com.fcfb.arceus.util.ranking.RatingNormalizer

class CompositeRatingsCalculator(private val ratingNormalizer: RatingNormalizer) {
    fun calculate(
        colleyRatings: Map<Int, Double>,
        asrRatings: Map<Int, Double>,
        equivalentWins: Map<Int, Double>,
    ): Map<Int, Double> {
        val teamIds = colleyRatings.keys + asrRatings.keys + equivalentWins.keys
        if (teamIds.isEmpty()) return emptyMap()

        val normalizedColley = ratingNormalizer.normalize(colleyRatings)
        val normalizedAsr = ratingNormalizer.normalize(asrRatings)
        val normalizedEquivalentWins = ratingNormalizer.normalize(equivalentWins)

        return teamIds.associateWith { teamId ->
            COMPOSITE_COLLEY_WEIGHT * (normalizedColley[teamId] ?: 0.0) +
                COMPOSITE_ASR_WEIGHT * (normalizedAsr[teamId] ?: 0.0) +
                COMPOSITE_EQUIVALENT_WINS_WEIGHT * (normalizedEquivalentWins[teamId] ?: 0.0)
        }
    }

    companion object {
        private const val COMPOSITE_COLLEY_WEIGHT = 0.45
        private const val COMPOSITE_ASR_WEIGHT = 0.40
        private const val COMPOSITE_EQUIVALENT_WINS_WEIGHT = 0.15
    }
}
