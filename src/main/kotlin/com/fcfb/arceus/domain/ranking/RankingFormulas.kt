package com.fcfb.arceus.domain.ranking

class RankingFormulas {
    fun calculateCappedMarginOfVictory(
        cappedPointDifferential: Int,
        gamesPlayed: Int,
    ): Double = if (gamesPlayed == 0) 0.0 else cappedPointDifferential.toDouble() / gamesPlayed

    fun calculateMarginOfVictory(
        pointsFor: Int,
        pointsAgainst: Int,
        gamesPlayed: Int,
    ): Double = if (gamesPlayed == 0) 0.0 else (pointsFor - pointsAgainst).toDouble() / gamesPlayed

    fun calculateScoringOffense(
        pointsFor: Int,
        gamesPlayed: Int,
    ): Double = if (gamesPlayed == 0) 0.0 else pointsFor.toDouble() / gamesPlayed

    fun calculateScoringDefense(
        pointsAgainst: Int,
        gamesPlayed: Int,
    ): Double = if (gamesPlayed == 0) 0.0 else pointsAgainst.toDouble() / gamesPlayed

    fun calculateEquivalentWins(
        pointsFor: Int,
        pointsAgainst: Int,
        gamesPlayed: Int,
    ): Double {
        if (gamesPlayed == 0 || (pointsFor == 0 && pointsAgainst == 0)) return 0.0
        val pointsForExp = Math.pow(pointsFor.toDouble(), PYTHAGOREAN_EXPONENT)
        val pointsAgainstExp = Math.pow(pointsAgainst.toDouble(), PYTHAGOREAN_EXPONENT)
        return gamesPlayed * (pointsForExp / (pointsForExp + pointsAgainstExp))
    }

    companion object {
        const val PYTHAGOREAN_EXPONENT = 2.37
    }
}
