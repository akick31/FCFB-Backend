package com.fcfb.arceus.dto.request

import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole

data class UpdateUserRequest(
    val id: Long,
    var username: String,
    var coachName: String,
    var discordTag: String,
    var discordId: String?,
    var position: CoachPosition,
    var role: UserRole,
    var team: String?,
    var wins: Int,
    var losses: Int,
    var conferenceWins: Int,
    var conferenceLosses: Int,
    var conferenceChampionshipWins: Int,
    var conferenceChampionshipLosses: Int,
    var bowlWins: Int,
    var bowlLosses: Int,
    var playoffWins: Int,
    var playoffLosses: Int,
    var nationalChampionshipWins: Int,
    var nationalChampionshipLosses: Int,
    var offensivePlaybook: OffensivePlaybook,
    var defensivePlaybook: DefensivePlaybook,
) {
    fun toUserDTO() =
        UserDTO(
            id = id,
            username = username,
            coachName = coachName,
            discordTag = discordTag,
            discordId = discordId,
            position = position,
            role = role,
            team = team,
            delayOfGameInstances = 0,
            wins = wins,
            losses = losses,
            winPercentage = 0.0,
            conferenceWins = conferenceWins,
            conferenceLosses = conferenceLosses,
            conferenceChampionshipWins = conferenceChampionshipWins,
            conferenceChampionshipLosses = conferenceChampionshipLosses,
            bowlWins = bowlWins,
            bowlLosses = bowlLosses,
            playoffWins = playoffWins,
            playoffLosses = playoffLosses,
            nationalChampionshipWins = nationalChampionshipWins,
            nationalChampionshipLosses = nationalChampionshipLosses,
            offensivePlaybook = offensivePlaybook,
            defensivePlaybook = defensivePlaybook,
            averageResponseTime = 0.0,
            delayOfGameWarningOptOut = false,
        )
}
