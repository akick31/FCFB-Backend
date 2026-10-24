package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.exception.user.UserForbiddenException
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.AuthContext
import org.springframework.stereotype.Service

@Service
class TeamEditPermissionService(
    private val teamService: TeamService,
    private val userService: UserService,
) {
    fun requireCanEdit(team: String) = requireCoachOfAny(listOf(teamService.getTeamByName(team)))

    fun requireCoachOfAny(teams: List<Team>) {
        if (AuthContext().isAdminOrCommissioner()) return
        val userId = AuthContext().currentUserId() ?: throw UserForbiddenException()
        val user = userService.getUserById(userId)
        val coaches =
            teams.any { team ->
                team.coachUsernames?.contains(user.username) == true ||
                    user.discordId?.let { team.coachDiscordIds?.contains(it) } == true
            }
        if (!coaches) throw UserForbiddenException()
    }
}
