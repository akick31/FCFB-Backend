package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.WinProbabilityOrchestrationService
import io.swagger.v3.oas.annotations.Operation
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
    private val winProbabilityOrchestrationService: WinProbabilityOrchestrationService,
) {
    @Operation(summary = "Get Elo ratings for all teams")
    @GetMapping("/elo-ratings")
    fun getEloRatings() = winProbabilityOrchestrationService.getEloRatings()

    @Operation(summary = "Calculate win probability for a game")
    @PostMapping("/calculate")
    fun calculateWinProbabilityForGame(
        @RequestParam gameId: Int,
    ) = winProbabilityOrchestrationService.calculateWinProbabilityForGame(gameId)

    @Operation(summary = "Calculate win probability for all games")
    @PostMapping("/calculate/all")
    fun calculateWinProbabilityForAllGames() = winProbabilityOrchestrationService.calculateWinProbabilityForAllGames()

    @Operation(summary = "Get win probabilities for a game")
    @GetMapping("")
    fun getWinProbabilitiesForGame(
        @RequestParam gameId: Int,
    ) = winProbabilityOrchestrationService.getWinProbabilitiesForGame(gameId)
}
