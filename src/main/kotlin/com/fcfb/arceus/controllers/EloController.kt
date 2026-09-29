package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.elo.EloService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/elo")
@CrossOrigin(origins = ["*"])
class EloController(
    private val eloService: EloService,
) {
    @Operation(summary = "Get team ELO ratings")
    @GetMapping("/ratings")
    fun getEloRatings() = eloService.getEloRatings()

    @Operation(summary = "Get team ELO history")
    @GetMapping("/history")
    fun getEloHistory(
        @RequestParam team: String,
        @RequestParam(required = false) season: Int?,
    ) = eloService.getEloHistory(team, season)

    @Operation(summary = "Rebuild ELO from game results (dry run unless apply=true)")
    @PostMapping("/rebuild")
    fun rebuildElo(
        @RequestParam(defaultValue = "false") apply: Boolean,
    ) = eloService.rebuild(apply)
}
