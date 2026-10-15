package com.fcfb.arceus.service.fcfb.team

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.exception.team.TeamNotFoundException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.conference.ConferenceService
import com.fcfb.arceus.service.fcfb.schedule.TeamScheduleCleanupService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val conferenceService: ConferenceService,
    private val teamScheduleCleanupService: TeamScheduleCleanupService,
) {
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

    fun getTeamById(id: Int) =
        teamRepository.findById(id).orElse(null)
            ?: throw TeamNotFoundException("Team not found with ID: $id")

    fun getAllTeams() =
        teamRepository.getAllActiveTeams().ifEmpty {
            throw TeamNotFoundException("No active teams found")
        }

    fun getAllTeamsIncludingInactive() = teamRepository.findAll().toList()

    fun getTeamByName(name: String?) =
        teamRepository.getTeamByName(name)
            ?: throw TeamNotFoundException("Team not found with name: $name")

    fun createTeam(team: Team): Team {
        team.conference?.let { conferenceService.requireExists(it) }
        return teamRepository.save(
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
                team.logoDark,
                team.division,
                team.stadium,
            ),
        )
    }

    fun updateTeam(team: Team): Team {
        team.conference?.let { conferenceService.requireExists(it) }
        val existingTeam = getTeamByName(team.name)
        val wasActive = existingTeam.active

        existingTeam.apply {
            this.name = team.name
            shortName = team.shortName
            abbreviation = team.abbreviation
            logo = team.logo
            logoDark = team.logoDark
            scorebugLogo = team.scorebugLogo
            stadium = team.stadium
            coachUsernames = team.coachUsernames
            coachNames = team.coachNames
            coachDiscordTags = team.coachDiscordTags
            coachDiscordIds = team.coachDiscordIds
            subdivision = team.subdivision
            conference = team.conference
            division = team.division
            primaryColor = team.primaryColor
            secondaryColor = team.secondaryColor
            offensivePlaybook = team.offensivePlaybook
            defensivePlaybook = team.defensivePlaybook
            isTaken = team.isTaken
            active = team.active
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
        if (wasActive && !existingTeam.active) {
            existingTeam.name?.let { teamScheduleCleanupService.removeTeamFromUpcomingSchedule(it) }
        }
        return existingTeam
    }

    fun updateCurrentElo(team: Team) {
        val existingTeam = getTeamByName(team.name)
        existingTeam.currentElo = team.currentElo
        teamRepository.save(existingTeam)
    }

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

    fun getOpenTeams() = teamRepository.getOpenTeams()

    fun getTeamsInConference(conference: String) = teamRepository.getTeamsInConference(conference)

    fun resetWinsAndLosses() = teamRepository.resetWinsAndLosses()

    fun saveTeam(team: Team) = teamRepository.save(team)

    fun deleteTeam(id: Int): HttpStatus {
        teamRepository.findById(id) ?: return HttpStatus.NOT_FOUND
        if (!teamRepository.findById(id).isPresent) {
            return HttpStatus.NOT_FOUND
        }
        teamRepository.deleteById(id)
        return HttpStatus.OK
    }
}
