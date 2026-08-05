package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.WinProbabilityOrchestrationService
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
    @GetMapping("/elo-ratings")
    fun getEloRatings() = winProbabilityOrchestrationService.getEloRatings()

    @PostMapping("/calculate")
    fun calculateWinProbabilityForGame(
        @RequestParam gameId: Int,
    ) = winProbabilityOrchestrationService.calculateWinProbabilityForGame(gameId)

    @PostMapping("/calculate/all")
    fun calculateWinProbabilityForAllGames() = winProbabilityOrchestrationService.calculateWinProbabilityForAllGames()

    @GetMapping("")
    fun getWinProbabilitiesForGame(
        @RequestParam gameId: Int,
    ) = winProbabilityOrchestrationService.getWinProbabilitiesForGame(gameId)
}
