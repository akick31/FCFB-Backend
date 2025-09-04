package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.log.CoachTransactionLogService
import com.fcfb.arceus.util.NoCoachDiscordIdsFoundException
import com.fcfb.arceus.util.TeamNotFoundException
import com.fcfb.arceus.util.TooManyCoachesException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val userService: UserService,
    private val coachTransactionLogService: CoachTransactionLogService,
    private val newSignupService: NewSignupService,
) {
    /**
     * After a game ends, update the team's wins and losses
     * @param game
     */
    fun updateTeamWinsAndLosses(game: Game) {
        val homeTeam = getTeamByName(game.homeTeam)
        val awayTeam = getTeamByName(game.awayTeam)

        if (game.homeScore > game.awayScore) {
            homeTeam.currentWins += 1
            awayTeam.currentLosses += 1
            homeTeam.overallWins += 1
            awayTeam.overallLosses += 1
            if (game.gameType == GameType.CONFERENCE_GAME) {
                homeTeam.currentConferenceWins += 1
                awayTeam.currentConferenceLosses += 1
                homeTeam.overallConferenceWins += 1
                awayTeam.overallConferenceLosses += 1
            } else if (game.gameType == GameType.CONFERENCE_CHAMPIONSHIP) {
                homeTeam.conferenceChampionshipWins += 1
                awayTeam.conferenceChampionshipLosses += 1
            } else if (game.gameType == GameType.BOWL) {
                homeTeam.bowlWins += 1
                awayTeam.bowlLosses += 1
            } else if (game.gameType == GameType.PLAYOFFS) {
                homeTeam.bowlWins += 1
                awayTeam.bowlLosses += 1
                homeTeam.playoffWins += 1
                awayTeam.playoffLosses += 1
            } else if (game.gameType == GameType.NATIONAL_CHAMPIONSHIP) {
                homeTeam.bowlWins += 1
                awayTeam.bowlLosses += 1
                homeTeam.playoffWins += 1
                awayTeam.playoffLosses += 1
                homeTeam.nationalChampionshipWins += 1
                awayTeam.nationalChampionshipLosses += 1
            }
        } else {
            homeTeam.currentLosses += 1
            awayTeam.currentWins += 1
            homeTeam.overallLosses += 1
            awayTeam.overallWins += 1
            if (game.gameType == GameType.CONFERENCE_GAME) {
                homeTeam.currentConferenceLosses += 1
                awayTeam.currentConferenceWins += 1
                homeTeam.overallConferenceLosses += 1
                awayTeam.overallConferenceWins += 1
            } else if (game.gameType == GameType.CONFERENCE_CHAMPIONSHIP) {
                homeTeam.conferenceChampionshipLosses += 1
                awayTeam.conferenceChampionshipWins += 1
            } else if (game.gameType == GameType.BOWL) {
                homeTeam.bowlLosses += 1
                awayTeam.bowlWins += 1
            } else if (game.gameType == GameType.PLAYOFFS) {
                homeTeam.bowlLosses += 1
                awayTeam.bowlWins += 1
                homeTeam.playoffLosses += 1
                awayTeam.playoffWins += 1
            } else if (game.gameType == GameType.NATIONAL_CHAMPIONSHIP) {
                homeTeam.bowlLosses += 1
                awayTeam.bowlWins += 1
                homeTeam.playoffLosses += 1
                awayTeam.playoffWins += 1
                homeTeam.nationalChampionshipLosses += 1
                awayTeam.nationalChampionshipWins += 1
            }
        }
        updateTeam(homeTeam)
        updateTeam(awayTeam)
    }

    /**
     * Get a team by its ID
     * @param id
     */
    fun getTeamById(id: Int) =
        teamRepository.findById(id)
            ?: throw TeamNotFoundException("Team not found with ID: $id")

    /**
     * Get all teams
     */
    fun getAllTeams() =
        teamRepository.getAllActiveTeams().ifEmpty {
            throw TeamNotFoundException("No active teams found")
        }

    /**
     * Get a team by its name
     * @param name
     */
    fun getTeamByName(name: String?) =
        teamRepository.getTeamByName(name)
            ?: throw TeamNotFoundException("Team not found with name: $name")

    /**
     * Create a new team
     * @param team
     */
    fun createTeam(team: Team): Team {
        try {
            val newTeam =
                teamRepository.save(
                    Team(
                        team.logo,
                        team.scorebugLogo,
                        team.coachUsernames ?: mutableListOf(),
                        team.coachNames ?: mutableListOf(),
                        team.coachDiscordTags ?: mutableListOf(),
                        team.coachDiscordIds ?: mutableListOf(),
                        team.name,
                        team.shortName,
                        team.abbreviation,
                        team.primaryColor,
                        team.secondaryColor,
                        0,
                        0,
                        team.subdivision,
                        team.offensivePlaybook,
                        team.defensivePlaybook,
                        team.conference,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        false,
                        true,
                        1500.0,
                        1500.0,
                    ),
                )
            return newTeam
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Update a team
     * @param team
     */
    fun updateTeam(team: Team): Team {
        val existingTeam = getTeamByName(team.name)

        existingTeam.apply {
            this.name = team.name
            coachUsernames = team.coachUsernames
            coachNames = team.coachNames
            coachDiscordTags = team.coachDiscordTags
            coachDiscordIds = team.coachDiscordIds
            subdivision = team.subdivision
            conference = team.conference
            primaryColor = team.primaryColor
            secondaryColor = team.secondaryColor
            coachesPollRanking = team.coachesPollRanking
            playoffCommitteeRanking = team.playoffCommitteeRanking
            offensivePlaybook = team.offensivePlaybook
            defensivePlaybook = team.defensivePlaybook
            currentWins = team.currentWins
            currentLosses = team.currentLosses
            currentConferenceWins = team.currentConferenceWins
            currentConferenceLosses = team.currentConferenceLosses
            overallWins = team.overallWins
            overallLosses = team.overallLosses
            overallConferenceWins = team.overallConferenceWins
            overallConferenceLosses = team.overallConferenceLosses
            conferenceChampionshipWins = team.conferenceChampionshipWins
            conferenceChampionshipLosses = team.conferenceChampionshipLosses
            bowlWins = team.bowlWins
            bowlLosses = team.bowlLosses
            playoffWins = team.playoffWins
            playoffLosses = team.playoffLosses
            nationalChampionshipWins = team.nationalChampionshipWins
            nationalChampionshipLosses = team.nationalChampionshipLosses
        }
        teamRepository.save(existingTeam)
        return existingTeam
    }

    /**
     * Update team color
     */
    fun updateTeamColor(
        team: String,
        color: String,
    ): Team {
        val existingTeam = getTeamByName(team)
        val hexRegex = Regex("^#([a-fA-F0-9]{3}|[a-fA-F0-9]{4}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})$")
        if (!hexRegex.matches(color)) {
            throw IllegalArgumentException("Invalid color")
        }

        existingTeam.apply {
            primaryColor = color
        }
        teamRepository.save(existingTeam)
        return existingTeam
    }

    /**
     * Hire a coach for a team
     * @param team
     * @param discordId
     * @param coachPosition
     */
    suspend fun hireCoach(
        team: String?,
        discordId: String,
        coachPosition: CoachPosition,
        processedBy: String,
    ): Team {
        val existingTeam = getTeamByName(team)
        val user = userService.getUserDTOByDiscordId(discordId)
        user.team = existingTeam.name
        when (coachPosition) {
            CoachPosition.HEAD_COACH -> {
                // Fire previous coach if hiring a new head coach
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
            saveTeam(existingTeam)
            userService.updateUser(user)
            val signupObject = newSignupService.getNewSignupByDiscordId(discordId)
            if (signupObject != null) {
                newSignupService.deleteNewSignup(signupObject)
            }
            coachTransactionLogService.logCoachTransaction(
                CoachTransactionLog(
                    existingTeam.name ?: "TEAM_NOT_FOUND",
                    coachPosition,
                    mutableListOf(user.username),
                    TransactionType.HIRED,
                    ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                    processedBy,
                ),
            )
        }
        return existingTeam
    }

    /**
     * Hire an interim coach for a team
     * @param team
     * @param discordId
     * @param processedBy
     */
    suspend fun hireInterimCoach(
        team: String?,
        discordId: String,
        processedBy: String,
    ): Team {
        val existingTeam = getTeamByName(team)
        val user = userService.getUserDTOByDiscordId(discordId)

        existingTeam.coachUsernames = mutableListOf(user.username)
        existingTeam.coachNames = mutableListOf(user.coachName)
        existingTeam.coachDiscordTags = mutableListOf(user.discordTag)
        existingTeam.coachDiscordIds = mutableListOf(discordId)
        existingTeam.offensivePlaybook = user.offensivePlaybook
        existingTeam.defensivePlaybook = user.defensivePlaybook

        withContext(Dispatchers.IO) {
            saveTeam(existingTeam)
            val signupObject = newSignupService.getNewSignupByDiscordId(discordId)
            if (signupObject != null) {
                newSignupService.deleteNewSignup(signupObject)
            }
            coachTransactionLogService.logCoachTransaction(
                CoachTransactionLog(
                    existingTeam.name ?: "TEAM_NOT_FOUND",
                    CoachPosition.HEAD_COACH,
                    mutableListOf(user.username),
                    TransactionType.HIRED_INTERIM,
                    ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                    processedBy,
                ),
            )
        }
        return existingTeam
    }

    /**
     * Fire all coaches for a team
     * @param name
     */
    fun fireCoach(
        name: String?,
        processedBy: String,
    ): Team {
        val existingTeam = getTeamByName(name)
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
                existingTeam.name ?: "TEAM_NOT_FOUND",
                CoachPosition.HEAD_COACH,
                existingTeam.coachUsernames,
                TransactionType.FIRED,
                ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                processedBy,
            ),
        )
        existingTeam.coachUsernames = null
        existingTeam.coachNames = null
        existingTeam.coachDiscordTags = null
        existingTeam.coachDiscordIds = null
        existingTeam.offensivePlaybook = OffensivePlaybook.AIR_RAID
        existingTeam.defensivePlaybook = DefensivePlaybook.FOUR_THREE
        existingTeam.isTaken = false
        saveTeam(existingTeam)
        return existingTeam
    }

    /**
     * Get open teams
     */
    fun getOpenTeams() = teamRepository.getOpenTeams()

    /**
     * Get all teams in a conference
     */
    fun getTeamsInConference(conference: String) = teamRepository.getTeamsInConference(conference)

    /**
     * Get team ranks for a game
     */
    fun getTeamRanks(
        homeTeamId: Int,
        awayTeamId: Int,
    ): Pair<Int?, Int?> {
        val usePlayoff = usePlayoffRanking()

        val homeRank =
            if (usePlayoff) {
                getPlayoffRankingById(homeTeamId)
            } else {
                getCoachesPollRankingById(homeTeamId)
            }

        val awayRank =
            if (usePlayoff) {
                getPlayoffRankingById(awayTeamId)
            } else {
                getCoachesPollRankingById(awayTeamId)
            }

        return Pair(homeRank, awayRank)
    }

    fun resetWinsAndLosses() = teamRepository.resetWinsAndLosses()

    /**
     * Check if playoff ranking is used
     * @return
     */
    private fun usePlayoffRanking() = teamRepository.usePlayoffRanking() == 1

    /**
     * Get playoff ranking by team ID
     * @param id
     * @return
     */
    private fun getPlayoffRankingById(id: Int) = teamRepository.getPlayoffRankingById(id)

    /**
     * Get coaches poll ranking by team ID
     * @param id
     * @return
     */
    private fun getCoachesPollRankingById(id: Int) = teamRepository.getCoachesPollRankingById(id)

    /**
     * Save a team
     * @param team
     */
    private fun saveTeam(team: Team) = teamRepository.save(team)

    /**
     * Delete a team
     * @param id
     */
    fun deleteTeam(id: Int): HttpStatus {
        teamRepository.findById(id) ?: return HttpStatus.NOT_FOUND
        if (!teamRepository.findById(id).isPresent) {
            return HttpStatus.NOT_FOUND
        }
        teamRepository.deleteById(id)
        return HttpStatus.OK
    }
}
