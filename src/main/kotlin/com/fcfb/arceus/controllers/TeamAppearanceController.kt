package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.dto.request.TeamUniformRequest
import com.fcfb.arceus.service.fcfb.TeamAppearanceService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/team-appearance")
class TeamAppearanceController(
    private val teamAppearanceService: TeamAppearanceService,
) {
    @Operation(summary = "Get a team's uniform")
    @GetMapping("/uniform")
    fun getUniform(
        @RequestParam team: String,
    ) = teamAppearanceService.getUniform(team)

    @Operation(summary = "Update a team's uniform")
    @PutMapping("/uniform")
    fun updateUniform(
        @RequestParam team: String,
        @RequestBody request: TeamUniformRequest,
    ) = teamAppearanceService.updateUniform(team, request)

    @Operation(summary = "Get a team's field")
    @GetMapping("/field")
    fun getField(
        @RequestParam team: String,
    ) = teamAppearanceService.getField(team)

    @Operation(summary = "Update a team's field")
    @PutMapping("/field")
    fun updateField(
        @RequestParam team: String,
        @RequestBody request: TeamFieldRequest,
    ) = teamAppearanceService.updateField(team, request)
}
