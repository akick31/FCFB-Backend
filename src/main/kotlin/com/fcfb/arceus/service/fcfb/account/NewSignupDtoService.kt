package com.fcfb.arceus.service.fcfb.account

import com.fcfb.arceus.dto.response.NewSignupDTO
import com.fcfb.arceus.model.account.NewSignup
import org.springframework.stereotype.Service

@Service
class NewSignupDtoService {
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
