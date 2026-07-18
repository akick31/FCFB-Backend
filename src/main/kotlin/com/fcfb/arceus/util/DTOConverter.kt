package com.fcfb.arceus.util

import com.fcfb.arceus.dto.response.NewSignupDTO
import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.model.NewSignup
import com.fcfb.arceus.model.User
import org.springframework.stereotype.Component

@Component
class DTOConverter {
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
        )
    }

    fun convertToNewSignupDTO(newSignup: NewSignup): NewSignupDTO {
        return NewSignupDTO(
            id = newSignup.id,
            username = newSignup.username,
            coachName = newSignup.coachName,
            discordTag = newSignup.discordTag,
            discordId = newSignup.discordId,
            position = newSignup.position,
            teamChoiceOne = newSignup.teamChoiceOne,
            teamChoiceTwo = newSignup.teamChoiceTwo,
            teamChoiceThree = newSignup.teamChoiceThree,
            offensivePlaybook = newSignup.offensivePlaybook,
            defensivePlaybook = newSignup.defensivePlaybook,
            approved = newSignup.approved,
        )
    }
}
