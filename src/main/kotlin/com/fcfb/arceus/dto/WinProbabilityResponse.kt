package com.fcfb.arceus.dto

data class EloRatingResponse(
    val teamId: Int,
    val teamName: String,
    val currentElo: Double,
    val overallElo: Double,
)

data class InitializeEloResponse(
    val message: String,
    val initializedCount: Int,
)

data class SinglePlayWinProbabilityResponse(
    val playId: Int,
    val playNumber: Int,
    val quarter: Int,
    val clock: Int,
    val homeScore: Int,
    val awayScore: Int,
    val winProbability: Double,
    val winProbabilityAdded: Double,
    val possession: String,
    val possessionTeam: String,
    val ballLocation: Int,
    val down: Int,
    val distance: Int,
    val playCall: String?,
    val homeElo: Double,
    val awayElo: Double,
)

data class SingleGameWinProbabilitiesResponse(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val totalPlays: Int,
    val processedPlays: Int,
    val plays: List<SinglePlayWinProbabilityResponse>,
)

data class ProcessedGameResult(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val playsProcessed: Int,
)

data class WinProbabilitiesForAllGamesResponse(
    val totalGames: Int,
    val gamesProcessed: Int,
    val totalPlaysProcessed: Int,
    val processedGames: List<ProcessedGameResult>,
)

data class PlayWinProbabilityResponse(
    val playNumber: Int,
    val quarter: Int,
    val clock: Int,
    val homeScore: Int,
    val awayScore: Int,
    val homeTeamWinProbability: Double,
    val awayTeamWinProbability: Double,
)

data class GameWinProbabilitiesResponse(
    val gameId: Int,
    val totalPlays: Int,
    val plays: List<PlayWinProbabilityResponse>,
)
