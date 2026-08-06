package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.service.fcfb.TeamService
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
    @GetMapping("/{teamId}")
    fun getTeamById(
        @PathVariable teamId: Int,
    ) = teamService.getTeamById(teamId)

    @GetMapping("")
    fun getAllTeams() = teamService.getAllTeams()

    @GetMapping("/all")
    fun getAllTeamsIncludingInactive() = teamService.getAllTeamsIncludingInactive()

    @GetMapping("/name")
    fun getTeamByName(
        @RequestParam name: String?,
    ) = teamService.getTeamByName(name)

    @PostMapping("")
    fun createTeam(
        @RequestBody team: Team,
    ) = teamService.createTeam(team)

    @PutMapping("")
    fun updateTeam(
        @RequestBody team: Team,
    ) = teamService.updateTeam(team)

    @PostMapping("/hire")
    suspend fun hireCoach(
        @RequestParam team: String?,
        @RequestParam discordId: String,
        @RequestParam coachPosition: CoachPosition,
        @RequestParam processedBy: String,
    ) = teamService.hireCoach(team, discordId, coachPosition, processedBy)

    @PostMapping("/hire/interim")
    suspend fun hireInterimCoach(
        @RequestParam team: String,
        @RequestParam discordId: String,
        @RequestParam processedBy: String,
    ) = teamService.hireInterimCoach(team, discordId, processedBy)

    @PostMapping("/fire")
    fun fireCoach(
        @RequestParam team: String,
        @RequestParam processedBy: String,
    ) = teamService.fireCoach(team, processedBy)

    @PostMapping("/fire/coach")
    fun fireSingleCoach(
        @RequestParam team: String,
        @RequestParam discordId: String,
        @RequestParam coachPosition: CoachPosition,
        @RequestParam processedBy: String,
    ) = teamService.fireSingleCoach(team, discordId, coachPosition, processedBy)

    @GetMapping("/open")
    fun getOpenTeams() = teamService.getOpenTeams()

    @DeleteMapping("/{teamId}")
    fun deleteTeam(
        @PathVariable teamId: Int,
    ) = teamService.deleteTeam(teamId)
}
