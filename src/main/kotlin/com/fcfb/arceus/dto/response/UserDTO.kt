package com.fcfb.arceus.dto.response

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole

data class UserDTO(
    val id: Long,
    var username: String,
    var coachName: String,
    var discordTag: String,
    var discordId: String?,
    var position: CoachPosition,
    var role: UserRole,
    var team: String?,
    var delayOfGameInstances: Int,
    var wins: Int,
    var losses: Int,
    var winPercentage: Double,
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
    var averageResponseTime: Double,
    var delayOfGameWarningOptOut: Boolean,
)
