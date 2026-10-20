package com.fcfb.arceus.model.animation

data class GameSituation(
    val homeTeamName: String,
    val awayTeamName: String,
    val homeScore: Int,
    val awayScore: Int,
    val quarter: Int,
    val clockSeconds: Int,
    val down: Int,
    val yardsToGo: Int,
) {
    companion object {
        fun sample(
            homeTeamName: String,
            awayTeamName: String,
        ): GameSituation =
            GameSituation(
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                homeScore = SAMPLE_HOME_SCORE,
                awayScore = SAMPLE_AWAY_SCORE,
                quarter = SAMPLE_QUARTER,
                clockSeconds = SAMPLE_CLOCK_SECONDS,
                down = SAMPLE_DOWN,
                yardsToGo = SAMPLE_YARDS_TO_GO,
            )

        private const val SAMPLE_HOME_SCORE = 14
        private const val SAMPLE_AWAY_SCORE = 10
        private const val SAMPLE_QUARTER = 2
        private const val SAMPLE_CLOCK_SECONDS = 522
        private const val SAMPLE_DOWN = 2
        private const val SAMPLE_YARDS_TO_GO = 7
    }
}
