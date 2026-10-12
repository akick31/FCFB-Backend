package com.fcfb.arceus.service.fcfb.account

import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.service.fcfb.team.TeamService
import org.springframework.stereotype.Service

@Service
class NewSignupManagementService(
    private val newSignupService: NewSignupService,
    private val teamService: TeamService,
) {
    fun getNewSignups() = newSignupService.getNewSignups()

    fun deleteNewSignupById(id: Long) = newSignupService.deleteNewSignupById(id)

    suspend fun hireCoachFromSignup(
        signupId: Long,
        team: String,
        coachPosition: CoachPosition,
        processedBy: String,
    ) = teamService.hireCoachFromSignup(signupId, team, coachPosition, processedBy)
}
