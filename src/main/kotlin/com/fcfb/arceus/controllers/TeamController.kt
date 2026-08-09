package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.service.fcfb.TeamService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/team")
class TeamController(
    private var teamService: TeamService,
) {
    @Operation(summary = "Get team by ID")
    @GetMapping(params = ["teamId"])
    fun getTeamById(
        @RequestParam teamId: Int,
    ) = teamService.getTeamById(teamId)

    @Operation(summary = "List all active teams")
    @GetMapping("/active")
    fun getAllTeams() = teamService.getAllTeams()

    @Operation(summary = "List all teams")
    @GetMapping("/all")
    fun getAllTeamsIncludingInactive() = teamService.getAllTeamsIncludingInactive()

    @Operation(summary = "Get team by name")
    @GetMapping("/name")
    fun getTeamByName(
        @RequestParam name: String?,
    ) = teamService.getTeamByName(name)

    @Operation(summary = "Create team")
    @PostMapping("")
    fun createTeam(
        @RequestBody team: Team,
    ) = teamService.createTeam(team)

    @Operation(summary = "Update team")
    @PutMapping("")
    fun updateTeam(
        @RequestBody team: Team,
    ) = teamService.updateTeam(team)

    @Operation(summary = "Hire coach")
    @PostMapping("/hire")
    suspend fun hireCoach(
        @RequestParam team: String?,
        @RequestParam discordId: String,
        @RequestParam coachPosition: CoachPosition,
        @RequestParam processedBy: String,
    ) = teamService.hireCoach(team, discordId, coachPosition, processedBy)

    @Operation(summary = "Hire interim coach")
    @PostMapping("/hire/interim")
    suspend fun hireInterimCoach(
        @RequestParam team: String,
        @RequestParam discordId: String,
        @RequestParam processedBy: String,
    ) = teamService.hireInterimCoach(team, discordId, processedBy)

    @Operation(summary = "Fire coaching staff")
    @PostMapping("/fire")
    fun fireCoach(
        @RequestParam team: String,
        @RequestParam processedBy: String,
    ) = teamService.fireCoach(team, processedBy)

    @Operation(summary = "Fire coach")
    @PostMapping("/fire/coach")
    fun fireSingleCoach(
        @RequestParam team: String,
        @RequestParam discordId: String,
        @RequestParam coachPosition: CoachPosition,
        @RequestParam processedBy: String,
    ) = teamService.fireSingleCoach(team, discordId, coachPosition, processedBy)

    @Operation(summary = "List open coaching positions")
    @GetMapping("/open")
    fun getOpenTeams() = teamService.getOpenTeams()

    @Operation(summary = "Delete team")
    @DeleteMapping("")
    fun deleteTeam(
        @RequestParam teamId: Int,
    ) = teamService.deleteTeam(teamId)
}
