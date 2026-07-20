package com.fcfb.arceus.dto.response

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition

data class NewSignupDTO(
    val id: Long,
    var username: String,
    var coachName: String,
    var discordTag: String,
    var discordId: String?,
    var position: CoachPosition,
    var teamChoiceOne: String?,
    var teamChoiceTwo: String?,
    var teamChoiceThree: String?,
    var offensivePlaybook: OffensivePlaybook,
    var defensivePlaybook: DefensivePlaybook,
    var approved: Boolean,
)
