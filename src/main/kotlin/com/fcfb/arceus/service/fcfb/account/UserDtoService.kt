package com.fcfb.arceus.service.fcfb.account

import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.model.account.User
import org.springframework.stereotype.Service

@Service
class UserDtoService {
    fun convertToUserDTO(user: User): UserDTO {
        return UserDTO(
            id = user.id,
            username = user.username,
            coachName = user.coachName,
            discordTag = user.discordTag,
            discordId = user.discordId,
            position = user.position,
            role = user.role,
            team = user.team,
            delayOfGameInstances = user.delayOfGameInstances,
            wins = user.wins,
            losses = user.losses,
            winPercentage = user.winPercentage,
            conferenceWins = user.conferenceWins,
            conferenceLosses = user.conferenceLosses,
            conferenceChampionshipWins = user.conferenceChampionshipWins,
            conferenceChampionshipLosses = user.conferenceChampionshipLosses,
            bowlWins = user.bowlWins,
            bowlLosses = user.bowlLosses,
            playoffWins = user.playoffWins,
            playoffLosses = user.playoffLosses,
            nationalChampionshipWins = user.nationalChampionshipWins,
            nationalChampionshipLosses = user.nationalChampionshipLosses,
            offensivePlaybook = user.offensivePlaybook,
            defensivePlaybook = user.defensivePlaybook,
            averageResponseTime = user.averageResponseTime,
            delayOfGameWarningOptOut = user.delayOfGameWarningOptOut,
            scoutingReportHiddenColumns = user.scoutingReportHiddenColumns,
        )
    }
}
