package com.fcfb.arceus.model.animation

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team

data class GameSituation(
    val homeTeamName: String,
    val awayTeamName: String,
    val homeScore: Int,
    val awayScore: Int,
    val quarter: Int,
    val clockSeconds: Int,
    val down: Int,
    val yardsToGo: Int,
    val possession: TeamSide? = null,
    val ballOn: Int? = null,
    val homeTimeouts: Int = MAX_TIMEOUTS,
    val awayTimeouts: Int = MAX_TIMEOUTS,
) {
    companion object {
        fun scoreboardName(team: Team): String = team.abbreviation?.takeIf { it.isNotBlank() } ?: team.name.orEmpty()

        /** A finished play's own scores are already post-play, so the score going into it comes from the play before. */
        fun beforeSnap(
            play: Play,
            previousPlay: Play?,
            homeTeamName: String,
            awayTeamName: String,
        ): GameSituation =
            GameSituation(
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                homeScore = previousPlay?.homeScore ?: 0,
                awayScore = previousPlay?.awayScore ?: 0,
                quarter = play.quarter,
                clockSeconds = play.clock,
                down = play.down,
                yardsToGo = play.yardsToGo,
                possession = play.possession,
                ballOn = yardLine(play.ballLocation),
                homeTimeouts = play.homeTimeouts,
                awayTimeouts = play.awayTimeouts,
            )

        private fun yardLine(ballLocation: Int): Int = if (ballLocation > HALF_FIELD) FULL_FIELD - ballLocation else ballLocation

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
                possession = TeamSide.HOME,
                ballOn = SAMPLE_BALL_ON,
                homeTimeouts = SAMPLE_HOME_TIMEOUTS,
            )

        private const val SAMPLE_HOME_SCORE = 14
        private const val SAMPLE_AWAY_SCORE = 10
        private const val SAMPLE_QUARTER = 2
        private const val SAMPLE_CLOCK_SECONDS = 522
        private const val SAMPLE_DOWN = 2
        private const val SAMPLE_YARDS_TO_GO = 7
        private const val SAMPLE_BALL_ON = 32
        private const val SAMPLE_HOME_TIMEOUTS = 2
        private const val MAX_TIMEOUTS = 3
        private const val HALF_FIELD = 50
        private const val FULL_FIELD = 100
    }
}
