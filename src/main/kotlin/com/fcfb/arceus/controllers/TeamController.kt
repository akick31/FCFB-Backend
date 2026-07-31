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
    /**
     * Get a team by id
     * @param id
     */
    @GetMapping("/{teamId}")
    fun getTeamById(
        @PathVariable teamId: Int,
    ) = teamService.getTeamById(teamId)

    /**
     * Get all teams
     * @return
     */
    @GetMapping("")
    fun getAllTeams() = teamService.getAllTeams()

    /**
     * Get a team by name
     * @param name
     * @return
     */
    @GetMapping("/name")
    fun getTeamByName(
        @RequestParam name: String?,
    ) = teamService.getTeamByName(name)

    /**
     * Create a team
     * @param team
     */
    @PostMapping("")
    fun createTeam(
        @RequestBody team: Team,
    ) = teamService.createTeam(team)

    /**
     * Update a team
     * @param team
     */
    @PutMapping("")
    fun updateTeam(
        @RequestBody team: Team,
    ) = teamService.updateTeam(team)

    /**
     * Hire a coach for a team
     * @param team
     * @param discordId
     * @param coachPosition
     * @param processedBy
     */
    @PostMapping("/hire")
    suspend fun hireCoach(
        @RequestParam team: String?,
        @RequestParam discordId: String,
        @RequestParam coachPosition: CoachPosition,
        @RequestParam processedBy: String,
    ) = teamService.hireCoach(team, discordId, coachPosition, processedBy)

    /**
     * Hire an interim coach for a team
     * @param team
     * @param discordId
     * @param processedBy
     */
    @PostMapping("/hire/interim")
    suspend fun hireInterimCoach(
        @RequestParam team: String,
        @RequestParam discordId: String,
        @RequestParam processedBy: String,
    ) = teamService.hireInterimCoach(team, discordId, processedBy)

    /**
     * Fire all coaches for a team
     * @param team
     */
    @PostMapping("/fire")
    fun fireCoach(
        @RequestParam team: String,
        @RequestParam processedBy: String,
    ) = teamService.fireCoach(team, processedBy)

    /**
     * Fire a single coach from a team, leaving any other coaches in place
     * @param team
     * @param discordId
     * @param coachPosition
     * @param processedBy
     */
    @PostMapping("/fire/coach")
    fun fireSingleCoach(
        @RequestParam team: String,
        @RequestParam discordId: String,
        @RequestParam coachPosition: CoachPosition,
        @RequestParam processedBy: String,
    ) = teamService.fireSingleCoach(team, discordId, coachPosition, processedBy)

    /**
     * Get open teams
     */
    @GetMapping("/open")
    fun getOpenTeams() = teamService.getOpenTeams()

    /**
     * Delete a team
     */
    @DeleteMapping("/{teamId}")
    fun deleteTeam(
        @PathVariable teamId: Int,
    ) = teamService.deleteTeam(teamId)
}
