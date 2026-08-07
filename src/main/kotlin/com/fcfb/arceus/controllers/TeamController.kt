package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.service.fcfb.TeamService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
    @Operation(summary = "Get a team by its ID")
    @GetMapping("/{teamId}")
    fun getTeamById(
        @PathVariable teamId: Int,
    ) = teamService.getTeamById(teamId)

    @Operation(summary = "List all active teams")
    @GetMapping("")
    fun getAllTeams() = teamService.getAllTeams()

    @Operation(summary = "List all teams, including inactive ones")
    @GetMapping("/all")
    fun getAllTeamsIncludingInactive() = teamService.getAllTeamsIncludingInactive()

    @Operation(summary = "Get a team by its name")
    @GetMapping("/name")
    fun getTeamByName(
        @RequestParam name: String?,
    ) = teamService.getTeamByName(name)

    @Operation(summary = "Create a new team")
    @PostMapping("")
    fun createTeam(
        @RequestBody team: Team,
    ) = teamService.createTeam(team)

    @Operation(summary = "Update an existing team")
    @PutMapping("")
    fun updateTeam(
        @RequestBody team: Team,
    ) = teamService.updateTeam(team)

    @Operation(summary = "Hire a coach onto a team")
    @PostMapping("/hire")
    suspend fun hireCoach(
        @RequestParam team: String?,
        @RequestParam discordId: String,
        @RequestParam coachPosition: CoachPosition,
        @RequestParam processedBy: String,
    ) = teamService.hireCoach(team, discordId, coachPosition, processedBy)

    @Operation(summary = "Hire an interim coach onto a team")
    @PostMapping("/hire/interim")
    suspend fun hireInterimCoach(
        @RequestParam team: String,
        @RequestParam discordId: String,
        @RequestParam processedBy: String,
    ) = teamService.hireInterimCoach(team, discordId, processedBy)

    @Operation(summary = "Fire a team's coaching staff")
    @PostMapping("/fire")
    fun fireCoach(
        @RequestParam team: String,
        @RequestParam processedBy: String,
    ) = teamService.fireCoach(team, processedBy)

    @Operation(summary = "Fire a single coach from a team")
    @PostMapping("/fire/coach")
    fun fireSingleCoach(
        @RequestParam team: String,
        @RequestParam discordId: String,
        @RequestParam coachPosition: CoachPosition,
        @RequestParam processedBy: String,
    ) = teamService.fireSingleCoach(team, discordId, coachPosition, processedBy)

    @Operation(summary = "List teams with an open coaching position")
    @GetMapping("/open")
    fun getOpenTeams() = teamService.getOpenTeams()

    @Operation(summary = "Delete a team")
    @DeleteMapping("/{teamId}")
    fun deleteTeam(
        @PathVariable teamId: Int,
    ) = teamService.deleteTeam(teamId)
}
