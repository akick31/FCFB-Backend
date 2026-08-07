package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.LeagueStats
import com.fcfb.arceus.model.PostseasonLeagueStats
import com.fcfb.arceus.model.PostseasonSeasonStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.PostseasonLeagueStatsRepository
import com.fcfb.arceus.repositories.PostseasonSeasonStatsRepository
import com.fcfb.arceus.service.specification.PostseasonLeagueStatsSpecificationService
import com.fcfb.arceus.util.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class PostseasonLeagueStatsService(
    private val postseasonLeagueStatsRepository: PostseasonLeagueStatsRepository,
    private val postseasonSeasonStatsRepository: PostseasonSeasonStatsRepository,
    private val leagueStatsService: LeagueStatsService,
    private val postseasonLeagueStatsSpecificationService: PostseasonLeagueStatsSpecificationService,
) {
    fun getFilteredPostseasonLeagueStats(
        subdivision: Subdivision?,
        season: Int?,
        pageable: Pageable,
    ): Page<PostseasonLeagueStats> {
        val spec = postseasonLeagueStatsSpecificationService.createSpecification(subdivision, season)
        val sortOrders = postseasonLeagueStatsSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return postseasonLeagueStatsRepository.findAll(spec, sortedPageable)
    }

    fun generateAllPostseasonLeagueStats() {
        Logger.info("Starting generation of all postseason league stats")

        val allSeasonStats = postseasonSeasonStatsRepository.findAllByOrderBySeasonNumberDescTeamAsc()

        val groupedStats =
            allSeasonStats.groupBy {
                Pair(it.subdivision, it.seasonNumber)
            }.filterKeys { it.first != null }

        val total = groupedStats.size
        groupedStats.keys.forEachIndexed { index, subdivisionSeason ->
            val subdivision = subdivisionSeason.first!!
            val seasonNumber = subdivisionSeason.second

            Logger.info("Generating postseason league stats for $subdivision in season $seasonNumber (${index + 1}/$total)")
            generatePostseasonLeagueStatsForSubdivisionAndSeason(subdivision, seasonNumber)
        }

        Logger.info("Completed generation of all postseason league stats")
    }

    private fun generatePostseasonLeagueStatsForSubdivisionAndSeason(
        subdivision: Subdivision,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of postseason league stats for $subdivision in season $seasonNumber")

        val seasonStatsList =
            postseasonSeasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
                .filter { seasonStats -> seasonStats.subdivision == subdivision }

        if (seasonStatsList.isEmpty()) {
            Logger.warn("No postseason season stats found for $subdivision in season $seasonNumber")
            return
        }

        postseasonLeagueStatsRepository.findBySubdivisionAndSeasonNumber(subdivision, seasonNumber)?.let {
            postseasonLeagueStatsRepository.delete(it)
        }

        val leagueStats =
            leagueStatsService.aggregateSeasonStatsToLeagueStats(
                seasonStatsList.map { it.toSeasonStats() },
                subdivision,
                seasonNumber,
            )

        postseasonLeagueStatsRepository.save(leagueStats.toPostseason())
        Logger.info("Completed generating postseason league stats for $subdivision in season $seasonNumber")
    }
}

private fun PostseasonSeasonStats.toSeasonStats(): SeasonStats {
    return SeasonStats(
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

private fun LeagueStats.toPostseason(): PostseasonLeagueStats {
    return PostseasonLeagueStats(
        subdivision = subdivision,
        seasonNumber = seasonNumber,
        totalTeams = totalTeams,
        totalGames = totalGames,
        passAttempts = passAttempts,
        passCompletions = passCompletions,
        passCompletionPercentage = passCompletionPercentage,
        passYards = passYards,
        passTouchdowns = passTouchdowns,
        passInterceptions = passInterceptions,
        passSuccesses = passSuccesses,
        passSuccessPercentage = passSuccessPercentage,
        longestPass = longestPass,
        rushAttempts = rushAttempts,
        rushSuccesses = rushSuccesses,
        rushSuccessPercentage = rushSuccessPercentage,
        rushYards = rushYards,
        rushTouchdowns = rushTouchdowns,
        longestRun = longestRun,
        totalYards = totalYards,
        averageYardsPerPlay = averageYardsPerPlay,
        firstDowns = firstDowns,
        sacksAllowed = sacksAllowed,
        sacksForced = sacksForced,
        interceptionsForced = interceptionsForced,
        fumblesForced = fumblesForced,
        fumblesRecovered = fumblesRecovered,
        defensiveTouchdowns = defensiveTouchdowns,
        fieldGoalsAttempted = fieldGoalsAttempted,
        fieldGoalsMade = fieldGoalsMade,
        fieldGoalPercentage = fieldGoalPercentage,
        longestFieldGoal = longestFieldGoal,
        punts = punts,
        longestPunt = longestPunt,
        kickoffReturnTouchdowns = kickoffReturnTouchdowns,
        puntReturnTouchdowns = puntReturnTouchdowns,
        averageOffensiveDiff = averageOffensiveDiff,
        averageDefensiveDiff = averageDefensiveDiff,
        averageOffensiveSpecialTeamsDiff = averageOffensiveSpecialTeamsDiff,
        averageDefensiveSpecialTeamsDiff = averageDefensiveSpecialTeamsDiff,
        averageDiff = averageDiff,
        averageResponseSpeed = averageResponseSpeed,
        lastModifiedTs = lastModifiedTs,
    )
}
