package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.ConferenceStats
import com.fcfb.arceus.model.PostseasonConferenceStats
import com.fcfb.arceus.model.PostseasonSeasonStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.PostseasonConferenceStatsRepository
import com.fcfb.arceus.repositories.PostseasonSeasonStatsRepository
import com.fcfb.arceus.service.specification.PostseasonConferenceStatsSpecificationService
import com.fcfb.arceus.util.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostseasonConferenceStatsService(
    private val postseasonConferenceStatsRepository: PostseasonConferenceStatsRepository,
    private val postseasonSeasonStatsRepository: PostseasonSeasonStatsRepository,
    private val conferenceStatsService: ConferenceStatsService,
    private val postseasonConferenceStatsSpecificationService: PostseasonConferenceStatsSpecificationService,
) {
    fun getFilteredPostseasonConferenceStats(
        conference: String?,
        season: Int?,
        subdivision: Subdivision?,
        pageable: Pageable,
    ): Page<PostseasonConferenceStats> {
        val spec = postseasonConferenceStatsSpecificationService.createSpecification(conference, season, subdivision)
        val sortOrders = postseasonConferenceStatsSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return postseasonConferenceStatsRepository.findAll(spec, sortedPageable)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun generateAllPostseasonConferenceStats() {
        Logger.info("Starting generation of all postseason conference stats")

        val allSeasonStats = postseasonSeasonStatsRepository.findAllByOrderBySeasonNumberDescTeamAsc()
        Logger.info("Found ${allSeasonStats.size} total postseason season stats records")

        val groupedStats =
            allSeasonStats.groupBy {
                Triple(it.subdivision, it.conference, it.seasonNumber)
            }.filterKeys { it.first != null && it.second != null }

        val total = groupedStats.size
        groupedStats.entries.forEachIndexed { index, (subdivisionConferenceSeason, seasonStatsList) ->
            val subdivision = subdivisionConferenceSeason.first!!
            val conference = subdivisionConferenceSeason.second!!
            val seasonNumber = subdivisionConferenceSeason.third

            Logger.info(
                "Generating postseason conference stats for $subdivision/$conference in season $seasonNumber " +
                    "with ${seasonStatsList.size} teams (${index + 1}/$total)",
            )
            generatePostseasonConferenceStatsForSubdivisionAndConferenceAndSeason(subdivision, conference, seasonNumber)
        }

        Logger.info("Completed generation of all postseason conference stats")
    }

    private fun generatePostseasonConferenceStatsForSubdivisionAndConferenceAndSeason(
        subdivision: Subdivision,
        conference: String,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of postseason conference stats for $subdivision/$conference in season $seasonNumber")

        val seasonStatsList =
            postseasonSeasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
                .filter { seasonStats ->
                    seasonStats.subdivision == subdivision && seasonStats.conference == conference
                }

        if (seasonStatsList.isEmpty()) {
            Logger.warn("No postseason season stats found for $subdivision/$conference in season $seasonNumber")
            return
        }

        postseasonConferenceStatsRepository.findBySubdivisionAndConferenceAndSeasonNumber(subdivision, conference, seasonNumber)?.let {
            postseasonConferenceStatsRepository.delete(it)
        }

        val conferenceStats =
            conferenceStatsService.aggregateSeasonStatsToConferenceStats(
                seasonStatsList.map { it.toSeasonStats() },
                subdivision,
                conference,
                seasonNumber,
            )

        postseasonConferenceStatsRepository.save(conferenceStats.toPostseason())
        Logger.info("Completed generating postseason conference stats for $subdivision/$conference in season $seasonNumber")
    }

    fun updateConferenceStatsForSeasonStats(seasonStats: PostseasonSeasonStats) {
        val subdivision = seasonStats.subdivision ?: return
        val conference = seasonStats.conference ?: return
        val season = seasonStats.seasonNumber

        generatePostseasonConferenceStatsForSubdivisionAndConferenceAndSeason(subdivision, conference, season)
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

private fun ConferenceStats.toPostseason(): PostseasonConferenceStats {
    return PostseasonConferenceStats(
        subdivision = subdivision,
        conference = conference,
        seasonNumber = seasonNumber,
        totalTeams = totalTeams,
        totalGames = totalGames,
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
