package com.fcfb.arceus.service.fcfb.team

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.exception.signup.InvalidNewSignupException
import com.fcfb.arceus.exception.signup.NewSignupNotFoundException
import com.fcfb.arceus.exception.team.NoCoachDiscordIdsFoundException
import com.fcfb.arceus.exception.team.TooManyCoachesException
import com.fcfb.arceus.model.log.CoachTransactionLog
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.service.fcfb.account.NewSignupService
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.log.CoachTransactionLogService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class TeamCoachService(
    private val teamService: TeamService,
    private val userService: UserService,
    private val coachTransactionLogService: CoachTransactionLogService,
    private val newSignupService: NewSignupService,
) {
    suspend fun hireCoach(
        team: String?,
        discordId: String,
        coachPosition: CoachPosition,
        processedBy: String,
    ): Team {
        val existingTeam = teamService.getTeamByName(team)
        val user = userService.getUserDTOByDiscordId(discordId)
        if (user.team != null && user.team != existingTeam.name) {
            fireSingleCoach(user.team, discordId, user.position, processedBy)
        }
        user.team = existingTeam.name
        when (coachPosition) {
            CoachPosition.HEAD_COACH -> {
                if (existingTeam.coachUsernames != null) {
                    fireCoach(existingTeam.name, processedBy)
                }
                existingTeam.coachUsernames = mutableListOf(user.username)
                existingTeam.coachNames = mutableListOf(user.coachName)
                existingTeam.coachDiscordTags = mutableListOf(user.discordTag)
                existingTeam.coachDiscordIds = mutableListOf(discordId)
                existingTeam.offensivePlaybook = user.offensivePlaybook
                existingTeam.defensivePlaybook = user.defensivePlaybook
            }
            CoachPosition.OFFENSIVE_COORDINATOR -> {
                if (existingTeam.coachNames == null || existingTeam.coachNames == listOf<String>()) {
                    existingTeam.coachNames = mutableListOf(user.coachName)
                } else if (existingTeam.coachNames?.size == 1) {
                    existingTeam.coachNames?.add(user.coachName)
                } else {
                    throw TooManyCoachesException()
                }

                if (existingTeam.coachUsernames == null || existingTeam.coachUsernames == mutableListOf<String>()) {
                    existingTeam.coachUsernames = mutableListOf(user.username)
                } else if (existingTeam.coachUsernames?.size == 1) {
                    existingTeam.coachUsernames?.add(user.username)
                } else {
                    throw TooManyCoachesException()
                }

                if (existingTeam.coachDiscordTags == null || existingTeam.coachDiscordTags == mutableListOf<String>()) {
                    existingTeam.coachDiscordTags = mutableListOf(user.discordTag)
                } else if (existingTeam.coachDiscordTags?.size == 1) {
                    existingTeam.coachDiscordTags?.add(user.discordTag)
                } else {
                    throw TooManyCoachesException()
                }

                if (existingTeam.coachDiscordIds == null || existingTeam.coachDiscordIds == mutableListOf<String>()) {
                    existingTeam.coachDiscordIds = mutableListOf(discordId)
                } else if (existingTeam.coachDiscordIds?.size == 1) {
                    existingTeam.coachDiscordIds?.add(discordId)
                } else {
                    throw TooManyCoachesException()
                }
                existingTeam.offensivePlaybook = user.offensivePlaybook
            }
            CoachPosition.DEFENSIVE_COORDINATOR -> {
                if (existingTeam.coachNames == null || existingTeam.coachNames == mutableListOf<String>()) {
                    existingTeam.coachNames = mutableListOf(user.coachName)
                } else if (existingTeam.coachNames?.size == 1) {
                    existingTeam.coachNames?.add(user.coachName)
                } else {
                    throw TooManyCoachesException()
                }

                if (existingTeam.coachUsernames == null || existingTeam.coachUsernames == mutableListOf<String>()) {
                    existingTeam.coachUsernames = mutableListOf(user.username)
                } else if (existingTeam.coachUsernames?.size == 1) {
                    existingTeam.coachUsernames?.add(user.username)
                } else {
                    throw TooManyCoachesException()
                }

                if (existingTeam.coachDiscordTags == null || existingTeam.coachDiscordTags == mutableListOf<String>()) {
                    existingTeam.coachDiscordTags = mutableListOf(user.discordTag)
                } else if (existingTeam.coachDiscordTags?.size == 1) {
                    existingTeam.coachDiscordTags?.add(user.discordTag)
                } else {
                    throw TooManyCoachesException()
                }

                if (existingTeam.coachDiscordIds == null || existingTeam.coachDiscordIds == mutableListOf<String>()) {
                    existingTeam.coachDiscordIds = mutableListOf(discordId)
                } else if (existingTeam.coachDiscordIds?.size == 1) {
                    existingTeam.coachDiscordIds?.add(discordId)
                } else {
                    throw TooManyCoachesException()
                }
                existingTeam.defensivePlaybook = user.defensivePlaybook
            }
            CoachPosition.RETIRED -> {}
        }

        withContext(Dispatchers.IO) {
            existingTeam.isTaken = true
            teamService.saveTeam(existingTeam)
            userService.updateUser(user)
            val signupObject = newSignupService.getNewSignupByDiscordId(discordId)
            if (signupObject != null) {
                newSignupService.deleteNewSignup(signupObject)
            }
            coachTransactionLogService.logCoachTransaction(
                CoachTransactionLog(
                    existingTeam.name!!,
                    coachPosition,
                    mutableListOf(user.username),
                    TransactionType.HIRED,
                    ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                    processedBy,
                    mutableListOf(discordId),
                ),
            )
        }
        return existingTeam
    }

    suspend fun hireCoachFromSignup(
        signupId: Long,
        team: String,
        coachPosition: CoachPosition,
        processedBy: String,
    ): Team {
        val newSignup = newSignupService.getNewSignupById(signupId) ?: throw NewSignupNotFoundException(signupId)
        val discordId = newSignup.discordId ?: throw InvalidNewSignupException("New signup ${newSignup.username} has no linked Discord ID")
        if (userService.findUserByDiscordId(discordId) == null) {
            newSignupService.approveNewSignup(newSignup)
        }
        return hireCoach(team, discordId, coachPosition, processedBy)
    }

    suspend fun hireInterimCoach(
        team: String?,
        discordId: String,
        processedBy: String,
    ): Team {
        val existingTeam = teamService.getTeamByName(team)
        val user = userService.getUserDTOByDiscordId(discordId)

        existingTeam.coachUsernames = mutableListOf(user.username)
        existingTeam.coachNames = mutableListOf(user.coachName)
        existingTeam.coachDiscordTags = mutableListOf(user.discordTag)
        existingTeam.coachDiscordIds = mutableListOf(discordId)
        existingTeam.offensivePlaybook = user.offensivePlaybook
        existingTeam.defensivePlaybook = user.defensivePlaybook

        withContext(Dispatchers.IO) {
            teamService.saveTeam(existingTeam)
            val signupObject = newSignupService.getNewSignupByDiscordId(discordId)
            if (signupObject != null) {
                newSignupService.deleteNewSignup(signupObject)
            }
            coachTransactionLogService.logCoachTransaction(
                CoachTransactionLog(
                    existingTeam.name!!,
                    CoachPosition.HEAD_COACH,
                    mutableListOf(user.username),
                    TransactionType.HIRED_INTERIM,
                    ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                    processedBy,
                    mutableListOf(discordId),
                ),
            )
        }
        return existingTeam
    }

    fun fireCoach(
        name: String?,
        processedBy: String,
    ): Team {
        val existingTeam = teamService.getTeamByName(name)
        val coachDiscordIds = existingTeam.coachDiscordIds ?: throw NoCoachDiscordIdsFoundException()
        for (coach in coachDiscordIds) {
            val user = userService.getUserDTOByDiscordId(coach)
            if (user.team == existingTeam.name) {
                user.team = null
                userService.updateUser(user)
            }
        }

        coachTransactionLogService.logCoachTransaction(
            CoachTransactionLog(
                existingTeam.name!!,
                CoachPosition.HEAD_COACH,
                existingTeam.coachUsernames,
                TransactionType.FIRED,
                ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                processedBy,
                coachDiscordIds.toMutableList(),
            ),
        )
        existingTeam.coachUsernames = null
        existingTeam.coachNames = null
        existingTeam.coachDiscordTags = null
        existingTeam.coachDiscordIds = null
        existingTeam.offensivePlaybook = OffensivePlaybook.AIR_RAID
        existingTeam.defensivePlaybook = DefensivePlaybook.FOUR_THREE
        existingTeam.isTaken = false
        teamService.saveTeam(existingTeam)
        return existingTeam
    }

    fun fireSingleCoach(
        name: String?,
        discordId: String,
        position: CoachPosition,
        processedBy: String,
    ): Team {
        val existingTeam = teamService.getTeamByName(name)
        val discordIds = existingTeam.coachDiscordIds ?: throw NoCoachDiscordIdsFoundException()
        val index = discordIds.indexOf(discordId)
        if (index == -1) throw NoCoachDiscordIdsFoundException()

        if (discordIds.size <= 1) {
            return fireCoach(name, processedBy)
        }

        val user = userService.getUserDTOByDiscordId(discordId)
        if (user.team == existingTeam.name) {
            user.team = null
            userService.updateUser(user)
        }

        coachTransactionLogService.logCoachTransaction(
            CoachTransactionLog(
                existingTeam.name!!,
                position,
                mutableListOf(user.username),
                TransactionType.FIRED,
                ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                processedBy,
                mutableListOf(discordId),
            ),
        )

        existingTeam.coachDiscordIds = discordIds.toMutableList().also { it.removeAt(index) }
        existingTeam.coachUsernames = existingTeam.coachUsernames?.toMutableList()?.also { if (index < it.size) it.removeAt(index) }
        existingTeam.coachNames = existingTeam.coachNames?.toMutableList()?.also { if (index < it.size) it.removeAt(index) }
        existingTeam.coachDiscordTags = existingTeam.coachDiscordTags?.toMutableList()?.also { if (index < it.size) it.removeAt(index) }
        teamService.saveTeam(existingTeam)
        return existingTeam
    }
}
