package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.GameService
import com.fcfb.arceus.service.fcfb.PlayService
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.service.fcfb.WinProbabilityService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/win-probability")
@CrossOrigin(origins = ["*"])
class WinProbabilityController(
    private val winProbabilityService: WinProbabilityService,
    private val teamService: TeamService,
    private val gameService: GameService,
    private val playService: PlayService,
) {
    @GetMapping("/elo-ratings")
    fun getEloRatings() = winProbabilityService.getEloRatings(teamService.getAllTeams())

    @PostMapping("/calculate")
    fun calculateWinProbabilityForGame(
        @RequestParam gameId: Int,
    ) = winProbabilityService.calculateWinProbabilitiesForSingleGame(
        gameId,
        gameService.getGameById(gameId),
        playService.getAllPlaysByGameId(gameId),
        teamService.getTeamByName(gameService.getGameById(gameId).homeTeam),
        teamService.getTeamByName(gameService.getGameById(gameId).awayTeam),
        playService,
    )

    @PostMapping("/calculate/all")
    fun calculateWinProbabilityForAllGames() =
        winProbabilityService.calculateWinProbabilitiesForAllGames(
            gameService.getAllGames(),
            playService,
            teamService,
        )

    @GetMapping("")
    fun getWinProbabilitiesForGame(
        @RequestParam gameId: Int,
    ) = winProbabilityService.getWinProbabilitiesForGame(
        gameId,
        playService.getAllPlaysByGameId(gameId),
    )
}
