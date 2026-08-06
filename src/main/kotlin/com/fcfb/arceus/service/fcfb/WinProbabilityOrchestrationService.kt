package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.response.EloRatingResponse
import com.fcfb.arceus.dto.response.GameWinProbabilitiesResponse
import com.fcfb.arceus.dto.response.SingleGameWinProbabilitiesResponse
import com.fcfb.arceus.dto.response.WinProbabilitiesForAllGamesResponse
import org.springframework.stereotype.Service

@Service
class WinProbabilityOrchestrationService(
    private val winProbabilityService: WinProbabilityService,
    private val gameService: GameService,
    private val playService: PlayService,
    private val teamService: TeamService,
) {
    fun getEloRatings(): List<EloRatingResponse> = winProbabilityService.getEloRatings(teamService.getAllTeams())

    fun calculateWinProbabilityForGame(gameId: Int): SingleGameWinProbabilitiesResponse {
        val game = gameService.getGameById(gameId)
        return winProbabilityService.calculateWinProbabilitiesForSingleGame(
            gameId,
            game,
            playService.getAllPlaysByGameId(gameId),
            teamService.getTeamByName(game.homeTeam),
            teamService.getTeamByName(game.awayTeam),
            playService,
        )
    }

    fun calculateWinProbabilityForAllGames(): WinProbabilitiesForAllGamesResponse =
        winProbabilityService.calculateWinProbabilitiesForAllGames(gameService.getAllGames(), playService, teamService)

    fun getWinProbabilitiesForGame(gameId: Int): GameWinProbabilitiesResponse =
        winProbabilityService.getWinProbabilitiesForGame(gameId, playService.getAllPlaysByGameId(gameId))
}
