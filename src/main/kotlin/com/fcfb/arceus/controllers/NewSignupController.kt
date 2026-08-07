package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.service.fcfb.NewSignupService
import com.fcfb.arceus.service.fcfb.TeamService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/new_signups")
class NewSignupController(
    private var newSignupService: NewSignupService,
    private val teamService: TeamService,
) {
    @Operation(summary = "List all pending new signups")
    @GetMapping("")
    fun getNewSignups() = newSignupService.getNewSignups()

    @Operation(summary = "Delete a new signup by its ID")
    @DeleteMapping("/{id}")
    fun deleteNewSignup(
        @PathVariable id: Long,
    ) = newSignupService.deleteNewSignupById(id)

    @Operation(summary = "Hire a coach from a pending new signup onto a team")
    @PostMapping("/{id}/hire")
    suspend fun hireFromSignup(
        @PathVariable id: Long,
        @RequestParam team: String,
        @RequestParam coachPosition: CoachPosition,
        @RequestParam processedBy: String,
    ) = teamService.hireCoachFromSignup(id, team, coachPosition, processedBy)
}
