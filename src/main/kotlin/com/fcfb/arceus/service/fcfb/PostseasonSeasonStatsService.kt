package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.PostseasonSeasonStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PostseasonSeasonStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.specification.PostseasonSeasonStatsSpecificationService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.POSTSEASON_START_WEEK
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class PostseasonSeasonStatsService(
    private val postseasonSeasonStatsRepository: PostseasonSeasonStatsRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val teamRepository: TeamRepository,
    private val seasonStatsService: SeasonStatsService,
    private val postseasonConferenceStatsService: PostseasonConferenceStatsService,
    private val postseasonSeasonStatsSpecificationService: PostseasonSeasonStatsSpecificationService,
    private val teamSeasonConferenceService: TeamSeasonConferenceService,
) {
    fun getFilteredPostseasonSeasonStats(
        team: String?,
        season: Int?,
        pageable: Pageable,
    ): Page<PostseasonSeasonStats> {
        val spec = postseasonSeasonStatsSpecificationService.createSpecification(team, season)
        val sortOrders = postseasonSeasonStatsSpecificationService.createSort()

        val sortedPageable =
            if (pageable.isPaged) {
                PageRequest.of(
                    pageable.pageNumber,
                    pageable.pageSize,
                    Sort.by(sortOrders),
                )
            } else {
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(sortOrders))
            }

        return postseasonSeasonStatsRepository.findAll(spec, sortedPageable)
    }

    private fun filterOutScrimmageGames(gameStatsList: List<GameStats>): List<GameStats> {
        return gameStatsList.filter { it.gameType != GameType.SCRIMMAGE }
    }

    private fun filterPostseasonGames(gameStatsList: List<GameStats>): List<GameStats> {
        return gameStatsList.filter { (it.week ?: 0) >= POSTSEASON_START_WEEK }
    }

    fun generateAllPostseasonSeasonStats() {
        Logger.info("Starting generation of all postseason season stats")

        postseasonSeasonStatsRepository.deleteAll()

        val allGameStats = filterPostseasonGames(filterOutScrimmageGames(gameStatsRepository.findAll().toList()))
        val gameStatsByGameId = allGameStats.groupBy { it.gameId }
        val gameStatsByTeamSeason = allGameStats.groupBy { "${it.team}_${it.season}" }
        val teamsByName = teamRepository.findAll().associateBy { it.name }

        val total = gameStatsByTeamSeason.size
        Logger.info("Generating postseason season stats for $total team-season combinations")
        gameStatsByTeamSeason.entries.forEachIndexed { index, (combination, teamGameStats) ->
            val (team, seasonStr) = combination.split("_")
            val season = seasonStr.toInt()
            Logger.info("Generating postseason season stats for $team in season $season (${index + 1}/$total)")
            val conference = teamSeasonConferenceService.getConference(team, season) ?: teamsByName[team]?.conference
            val seasonStats =
                seasonStatsService.aggregateGameStatsToSeasonStats(teamGameStats, team, season, gameStatsByGameId, conference)
            postseasonSeasonStatsRepository.save(seasonStats.toPostseason())
        }

        Logger.info("Completed generation of all postseason season stats")
    }

    fun generatePostseasonSeasonStatsForTeam(
        team: String,
        seasonNumber: Int,
    ) {
        Logger.info("Generating postseason season stats for $team in season $seasonNumber")

        postseasonSeasonStatsRepository.deleteByTeamAndSeasonNumber(team, seasonNumber)

        val teamGameStats =
            filterPostseasonGames(
                filterOutScrimmageGames(
                    gameStatsRepository.findAll()
                        .filter { it.team == team && it.season == seasonNumber },
                ),
            )

        if (teamGameStats.isEmpty()) {
            Logger.warn("No postseason game stats found for $team in season $seasonNumber")
            return
        }

        val gameIds = teamGameStats.map { it.gameId }.toSet()
        val gameStatsByGameId =
            gameStatsRepository.findAll()
                .filter { it.gameId in gameIds }
                .groupBy { it.gameId }

        val conference = teamSeasonConferenceService.getConference(team, seasonNumber) ?: teamRepository.findByName(team)?.conference

        val seasonStats =
            seasonStatsService.aggregateGameStatsToSeasonStats(teamGameStats, team, seasonNumber, gameStatsByGameId, conference)

        postseasonSeasonStatsRepository.save(seasonStats.toPostseason())
        Logger.info("Completed generating postseason season stats for $team in season $seasonNumber")
    }

    fun updateSeasonStatsForGame(gameStats: GameStats) {
        val team = gameStats.team ?: return
        val season = gameStats.season ?: return

        generatePostseasonSeasonStatsForTeam(team, season)

        postseasonConferenceStatsService.updateConferenceStatsForSeasonStats(
            postseasonSeasonStatsRepository.findByTeamAndSeasonNumber(team, season) ?: return,
        )
    }
}

private fun SeasonStats.toPostseason(): PostseasonSeasonStats {
    return PostseasonSeasonStats(
        team = team,
        seasonNumber = seasonNumber,
        wins = wins,
        losses = losses,
        subdivision = subdivision,
        conference = conference,
        offensivePlaybook = offensivePlaybook,
        defensivePlaybook = defensivePlaybook,
        passAttempts = passAttempts,
        passCompletions = passCompletions,
        passCompletionPercentage = passCompletionPercentage,
        passYards = passYards,
        longestPass = longestPass,
        passTouchdowns = passTouchdowns,
        passSuccesses = passSuccesses,
        passSuccessPercentage = passSuccessPercentage,
        rushAttempts = rushAttempts,
        rushSuccesses = rushSuccesses,
        rushSuccessPercentage = rushSuccessPercentage,
        rushYards = rushYards,
        longestRun = longestRun,
        rushTouchdowns = rushTouchdowns,
        totalYards = totalYards,
        averageYardsPerPlay = averageYardsPerPlay,
        firstDowns = firstDowns,
        sacksAllowed = sacksAllowed,
        sacksForced = sacksForced,
        interceptionsLost = interceptionsLost,
        interceptionsForced = interceptionsForced,
        fumblesLost = fumblesLost,
        fumblesForced = fumblesForced,
        turnoversLost = turnoversLost,
        turnoversForced = turnoversForced,
        turnoverDifferential = turnoverDifferential,
        turnoverTouchdownsLost = turnoverTouchdownsLost,
        turnoverTouchdownsForced = turnoverTouchdownsForced,
        pickSixesThrown = pickSixesThrown,
        pickSixesForced = pickSixesForced,
        fumbleReturnTdsCommitted = fumbleReturnTdsCommitted,
        fumbleReturnTdsForced = fumbleReturnTdsForced,
        fieldGoalMade = fieldGoalMade,
        fieldGoalAttempts = fieldGoalAttempts,
        fieldGoalPercentage = fieldGoalPercentage,
        longestFieldGoal = longestFieldGoal,
        blockedOpponentFieldGoals = blockedOpponentFieldGoals,
        fieldGoalTouchdown = fieldGoalTouchdown,
        puntsAttempted = puntsAttempted,
        longestPunt = longestPunt,
        averagePuntLength = averagePuntLength,
        blockedOpponentPunt = blockedOpponentPunt,
        puntReturnTd = puntReturnTd,
        puntReturnTdPercentage = puntReturnTdPercentage,
        numberOfKickoffs = numberOfKickoffs,
        onsideAttempts = onsideAttempts,
        onsideSuccess = onsideSuccess,
        onsideSuccessPercentage = onsideSuccessPercentage,
        normalKickoffAttempts = normalKickoffAttempts,
        touchbacks = touchbacks,
        touchbackPercentage = touchbackPercentage,
        kickReturnTd = kickReturnTd,
        kickReturnTdPercentage = kickReturnTdPercentage,
        numberOfDrives = numberOfDrives,
        timeOfPossession = timeOfPossession,
        touchdowns = touchdowns,
        thirdDownConversionSuccess = thirdDownConversionSuccess,
        thirdDownConversionAttempts = thirdDownConversionAttempts,
        thirdDownConversionPercentage = thirdDownConversionPercentage,
        fourthDownConversionSuccess = fourthDownConversionSuccess,
        fourthDownConversionAttempts = fourthDownConversionAttempts,
        fourthDownConversionPercentage = fourthDownConversionPercentage,
        largestLead = largestLead,
        largestDeficit = largestDeficit,
        redZoneAttempts = redZoneAttempts,
        redZoneSuccesses = redZoneSuccesses,
        redZoneSuccessPercentage = redZoneSuccessPercentage,
        redZonePercentage = redZonePercentage,
        safetiesForced = safetiesForced,
        safetiesCommitted = safetiesCommitted,
        averageOffensiveDiff = averageOffensiveDiff,
        averageDefensiveDiff = averageDefensiveDiff,
        averageOffensiveSpecialTeamsDiff = averageOffensiveSpecialTeamsDiff,
        averageDefensiveSpecialTeamsDiff = averageDefensiveSpecialTeamsDiff,
        averageDiff = averageDiff,
        averageResponseSpeed = averageResponseSpeed,
        opponentPassAttempts = opponentPassAttempts,
        opponentPassCompletions = opponentPassCompletions,
        opponentPassCompletionPercentage = opponentPassCompletionPercentage,
        opponentPassYards = opponentPassYards,
        opponentLongestPass = opponentLongestPass,
        opponentPassTouchdowns = opponentPassTouchdowns,
        opponentPassSuccesses = opponentPassSuccesses,
        opponentPassSuccessPercentage = opponentPassSuccessPercentage,
        opponentRushAttempts = opponentRushAttempts,
        opponentRushSuccesses = opponentRushSuccesses,
        opponentRushSuccessPercentage = opponentRushSuccessPercentage,
        opponentRushYards = opponentRushYards,
        opponentLongestRun = opponentLongestRun,
        opponentRushTouchdowns = opponentRushTouchdowns,
        opponentTotalYards = opponentTotalYards,
        opponentAverageYardsPerPlay = opponentAverageYardsPerPlay,
        opponentFirstDowns = opponentFirstDowns,
        opponentFieldGoalMade = opponentFieldGoalMade,
        opponentFieldGoalAttempts = opponentFieldGoalAttempts,
        opponentFieldGoalPercentage = opponentFieldGoalPercentage,
        opponentLongestFieldGoal = opponentLongestFieldGoal,
        opponentFieldGoalTouchdown = opponentFieldGoalTouchdown,
        opponentPuntsAttempted = opponentPuntsAttempted,
        opponentLongestPunt = opponentLongestPunt,
        opponentAveragePuntLength = opponentAveragePuntLength,
        opponentPuntReturnTd = opponentPuntReturnTd,
        opponentPuntReturnTdPercentage = opponentPuntReturnTdPercentage,
        opponentNumberOfKickoffs = opponentNumberOfKickoffs,
        opponentOnsideAttempts = opponentOnsideAttempts,
        opponentOnsideSuccess = opponentOnsideSuccess,
        opponentOnsideSuccessPercentage = opponentOnsideSuccessPercentage,
        opponentNormalKickoffAttempts = opponentNormalKickoffAttempts,
        opponentTouchbacks = opponentTouchbacks,
        opponentTouchbackPercentage = opponentTouchbackPercentage,
        opponentKickReturnTd = opponentKickReturnTd,
        opponentKickReturnTdPercentage = opponentKickReturnTdPercentage,
        opponentNumberOfDrives = opponentNumberOfDrives,
        opponentTimeOfPossession = opponentTimeOfPossession,
        opponentTouchdowns = opponentTouchdowns,
        opponentThirdDownConversionSuccess = opponentThirdDownConversionSuccess,
        opponentThirdDownConversionAttempts = opponentThirdDownConversionAttempts,
        opponentThirdDownConversionPercentage = opponentThirdDownConversionPercentage,
        opponentFourthDownConversionSuccess = opponentFourthDownConversionSuccess,
        opponentFourthDownConversionAttempts = opponentFourthDownConversionAttempts,
        opponentFourthDownConversionPercentage = opponentFourthDownConversionPercentage,
        opponentRedZoneAttempts = opponentRedZoneAttempts,
        opponentRedZoneSuccesses = opponentRedZoneSuccesses,
        opponentRedZoneSuccessPercentage = opponentRedZoneSuccessPercentage,
        opponentRedZonePercentage = opponentRedZonePercentage,
        lastModifiedTs = lastModifiedTs,
    )
}
