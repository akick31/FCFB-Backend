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
    @Operation(summary = "Get team ELO ratings")
    @GetMapping("/elo-ratings")
    fun getEloRatings() = winProbabilityOrchestrationService.getEloRatings()

    @Operation(summary = "Calculate game win probability")
    @PostMapping("/calculate")
    fun calculateWinProbabilityForGame(
        @RequestParam gameId: Int,
    ) = winProbabilityOrchestrationService.calculateWinProbabilityForGame(gameId)

    @Operation(summary = "Calculate all win probabilities")
    @PostMapping("/calculate/all")
    fun calculateWinProbabilityForAllGames() = winProbabilityOrchestrationService.calculateWinProbabilityForAllGames()

    @Operation(summary = "Get game win probabilities")
    @GetMapping("")
    fun getWinProbabilitiesForGame(
        @RequestParam gameId: Int,
    ) = winProbabilityOrchestrationService.getWinProbabilitiesForGame(gameId)
}
