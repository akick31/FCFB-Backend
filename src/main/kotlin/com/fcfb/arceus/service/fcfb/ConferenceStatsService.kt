package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.ConferenceStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.ConferenceStatsRepository
import com.fcfb.arceus.repositories.SeasonStatsRepository
import com.fcfb.arceus.service.specification.ConferenceStatsSpecificationService
import com.fcfb.arceus.util.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class ConferenceStatsService(
    private val conferenceStatsRepository: ConferenceStatsRepository,
    private val seasonStatsRepository: SeasonStatsRepository,
    private val conferenceStatsSpecificationService: ConferenceStatsSpecificationService,
) {
    /**
     * Get filtered conference stats with pagination
     */
    fun getFilteredConferenceStats(
        conference: Conference?,
        season: Int?,
        subdivision: Subdivision?,
        pageable: Pageable,
    ): Page<ConferenceStats> {
        val spec = conferenceStatsSpecificationService.createSpecification(conference, season, subdivision)
        val sortOrders = conferenceStatsSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return conferenceStatsRepository.findAll(spec, sortedPageable)
    }

    /**
     * Generate all conference stats (recalculate all conference stats)
     */
    fun generateAllConferenceStats() {
        Logger.info("Starting generation of all conference stats")

        // Get all season stats
        val allSeasonStats = seasonStatsRepository.findAllByOrderBySeasonNumberDescTeamAsc()
        Logger.info("Found ${allSeasonStats.size} total season stats records")

        // Group by subdivision, conference, and season
        val groupedStats =
            allSeasonStats.groupBy {
                Triple(it.subdivision, it.conference, it.seasonNumber)
            }.filterKeys { it.first != null && it.second != null }

        // Generate conference stats for each subdivision/conference/season combination
        for ((subdivisionConferenceSeason, seasonStatsList) in groupedStats) {
            val subdivision = subdivisionConferenceSeason.first!!
            val conference = subdivisionConferenceSeason.second!!
            val seasonNumber = subdivisionConferenceSeason.third

            Logger.info(
                "Generating conference stats for $subdivision/$conference in season $seasonNumber with ${seasonStatsList.size} teams",
            )
            generateConferenceStatsForSubdivisionAndConferenceAndSeason(subdivision, conference, seasonNumber)
        }

        Logger.info("Completed generation of all conference stats")
    }

    /**
     * Generate conference stats for a specific subdivision, conference, and season
     */
    private fun generateConferenceStatsForSubdivisionAndConferenceAndSeason(
        subdivision: Subdivision,
        conference: Conference,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of conference stats for $subdivision/$conference in season $seasonNumber")

        // Get all season stats for this subdivision, conference, and season
        val seasonStatsList =
            seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
                .filter { seasonStats ->
                    seasonStats.subdivision == subdivision && seasonStats.conference == conference
                }

        if (seasonStatsList.isEmpty()) {
            Logger.warn("No season stats found for $subdivision/$conference in season $seasonNumber")
            return
        }

        // Delete existing conference stats for this subdivision, conference, and season
        conferenceStatsRepository.findBySubdivisionAndConferenceAndSeasonNumber(subdivision, conference, seasonNumber)?.let {
            conferenceStatsRepository.delete(it)
        }

        // Create new conference stats
        val conferenceStats = aggregateSeasonStatsToConferenceStats(seasonStatsList, subdivision, conference, seasonNumber)

        conferenceStatsRepository.save(conferenceStats)
        Logger.info("Completed generating conference stats for $subdivision/$conference in season $seasonNumber")
    }

    /**
     * Update conference stats when season stats are updated
     */
    fun updateConferenceStatsForSeasonStats(seasonStats: SeasonStats) {
        val subdivision = seasonStats.subdivision ?: return
        val conference = seasonStats.conference ?: return
        val season = seasonStats.seasonNumber

        // Regenerate conference stats for this subdivision, conference, and season
        generateConferenceStatsForSubdivisionAndConferenceAndSeason(subdivision, conference, season)
    }

    /**
     * Aggregate season stats into conference stats
     */
    private fun aggregateSeasonStatsToConferenceStats(
        seasonStatsList: List<SeasonStats>,
        subdivision: Subdivision,
        conference: Conference,
        seasonNumber: Int,
    ): ConferenceStats {
        val totalTeams = seasonStatsList.size
        val totalGames = seasonStatsList.sumOf { it.wins + it.losses }

        return ConferenceStats(
            subdivision = subdivision,
            conference = conference,
            seasonNumber = seasonNumber,
            totalTeams = totalTeams,
            totalGames = totalGames,
            // Aggregate all the stats
            passAttempts = seasonStatsList.sumOf { it.passAttempts },
            passCompletions = seasonStatsList.sumOf { it.passCompletions },
            passCompletionPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.passCompletions },
                    seasonStatsList.sumOf { it.passAttempts },
                ),
            passYards = seasonStatsList.sumOf { it.passYards },
            longestPass = seasonStatsList.maxOfOrNull { it.longestPass } ?: 0,
            passTouchdowns = seasonStatsList.sumOf { it.passTouchdowns },
            passSuccesses = seasonStatsList.sumOf { it.passSuccesses },
            passSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.passSuccesses },
                    seasonStatsList.sumOf { it.passAttempts },
                ),
            rushAttempts = seasonStatsList.sumOf { it.rushAttempts },
            rushSuccesses = seasonStatsList.sumOf { it.rushSuccesses },
            rushSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.rushSuccesses },
                    seasonStatsList.sumOf { it.rushAttempts },
                ),
            rushYards = seasonStatsList.sumOf { it.rushYards },
            longestRun = seasonStatsList.maxOfOrNull { it.longestRun } ?: 0,
            rushTouchdowns = seasonStatsList.sumOf { it.rushTouchdowns },
            totalYards = seasonStatsList.sumOf { it.totalYards },
            averageYardsPerPlay = calculateAverage(seasonStatsList.mapNotNull { it.averageYardsPerPlay }),
            firstDowns = seasonStatsList.sumOf { it.firstDowns },
            sacksAllowed = seasonStatsList.sumOf { it.sacksAllowed },
            sacksForced = seasonStatsList.sumOf { it.sacksForced },
            interceptionsLost = seasonStatsList.sumOf { it.interceptionsLost },
            interceptionsForced = seasonStatsList.sumOf { it.interceptionsForced },
            fumblesLost = seasonStatsList.sumOf { it.fumblesLost },
            fumblesForced = seasonStatsList.sumOf { it.fumblesForced },
            turnoversLost = seasonStatsList.sumOf { it.turnoversLost },
            turnoversForced = seasonStatsList.sumOf { it.turnoversForced },
            turnoverDifferential = seasonStatsList.sumOf { it.turnoverDifferential },
            turnoverTouchdownsLost = seasonStatsList.sumOf { it.turnoverTouchdownsLost },
            turnoverTouchdownsForced = seasonStatsList.sumOf { it.turnoverTouchdownsForced },
            pickSixesThrown = seasonStatsList.sumOf { it.pickSixesThrown },
            pickSixesForced = seasonStatsList.sumOf { it.pickSixesForced },
            fumbleReturnTdsCommitted = seasonStatsList.sumOf { it.fumbleReturnTdsCommitted },
            fumbleReturnTdsForced = seasonStatsList.sumOf { it.fumbleReturnTdsForced },
            fieldGoalMade = seasonStatsList.sumOf { it.fieldGoalMade },
            fieldGoalAttempts = seasonStatsList.sumOf { it.fieldGoalAttempts },
            fieldGoalPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.fieldGoalMade },
                    seasonStatsList.sumOf { it.fieldGoalAttempts },
                ),
            longestFieldGoal = seasonStatsList.maxOfOrNull { it.longestFieldGoal } ?: 0,
            blockedOpponentFieldGoals = seasonStatsList.sumOf { it.blockedOpponentFieldGoals },
            fieldGoalTouchdown = seasonStatsList.sumOf { it.fieldGoalTouchdown },
            puntsAttempted = seasonStatsList.sumOf { it.puntsAttempted },
            longestPunt = seasonStatsList.maxOfOrNull { it.longestPunt } ?: 0,
            averagePuntLength = calculateAverage(seasonStatsList.mapNotNull { it.averagePuntLength }),
            blockedOpponentPunt = seasonStatsList.sumOf { it.blockedOpponentPunt },
            puntReturnTd = seasonStatsList.sumOf { it.puntReturnTd },
            puntReturnTdPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.puntReturnTd },
                    seasonStatsList.sumOf { it.puntsAttempted },
                ),
            numberOfKickoffs = seasonStatsList.sumOf { it.numberOfKickoffs },
            onsideAttempts = seasonStatsList.sumOf { it.onsideAttempts },
            onsideSuccess = seasonStatsList.sumOf { it.onsideSuccess },
            onsideSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.onsideSuccess },
                    seasonStatsList.sumOf { it.onsideAttempts },
                ),
            normalKickoffAttempts = seasonStatsList.sumOf { it.normalKickoffAttempts },
            touchbacks = seasonStatsList.sumOf { it.touchbacks },
            touchbackPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.touchbacks },
                    seasonStatsList.sumOf { it.normalKickoffAttempts },
                ),
            kickReturnTd = seasonStatsList.sumOf { it.kickReturnTd },
            kickReturnTdPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.kickReturnTd },
                    seasonStatsList.sumOf { it.numberOfKickoffs },
                ),
            numberOfDrives = seasonStatsList.sumOf { it.numberOfDrives },
            timeOfPossession = seasonStatsList.sumOf { it.timeOfPossession },
            touchdowns = seasonStatsList.sumOf { it.touchdowns },
            thirdDownConversionSuccess = seasonStatsList.sumOf { it.thirdDownConversionSuccess },
            thirdDownConversionAttempts = seasonStatsList.sumOf { it.thirdDownConversionAttempts },
            thirdDownConversionPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.thirdDownConversionSuccess },
                    seasonStatsList.sumOf { it.thirdDownConversionAttempts },
                ),
            fourthDownConversionSuccess = seasonStatsList.sumOf { it.fourthDownConversionSuccess },
            fourthDownConversionAttempts = seasonStatsList.sumOf { it.fourthDownConversionAttempts },
            fourthDownConversionPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.fourthDownConversionSuccess },
                    seasonStatsList.sumOf { it.fourthDownConversionAttempts },
                ),
            largestLead = seasonStatsList.maxOfOrNull { it.largestLead } ?: 0,
            largestDeficit = seasonStatsList.maxOfOrNull { it.largestDeficit } ?: 0,
            redZoneAttempts = seasonStatsList.sumOf { it.redZoneAttempts },
            redZoneSuccesses = seasonStatsList.sumOf { it.redZoneSuccesses },
            redZoneSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.redZoneSuccesses },
                    seasonStatsList.sumOf { it.redZoneAttempts },
                ),
            redZonePercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.redZoneAttempts },
                    seasonStatsList.sumOf { it.numberOfDrives },
                ),
            safetiesForced = seasonStatsList.sumOf { it.safetiesForced },
            safetiesCommitted = seasonStatsList.sumOf { it.safetiesCommitted },
            // Performance metrics are averages of team averages
            averageOffensiveDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageOffensiveDiff }),
            averageDefensiveDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDefensiveDiff }),
            averageOffensiveSpecialTeamsDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageOffensiveSpecialTeamsDiff }),
            averageDefensiveSpecialTeamsDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDefensiveSpecialTeamsDiff }),
            averageDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDiff }),
            averageResponseSpeed = calculateAverage(seasonStatsList.mapNotNull { it.averageResponseSpeed }),
            // Opponent Stats (what teams allowed opponents to do)
            opponentPassAttempts = seasonStatsList.sumOf { it.opponentPassAttempts },
            opponentPassCompletions = seasonStatsList.sumOf { it.opponentPassCompletions },
            opponentPassCompletionPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentPassCompletions },
                    seasonStatsList.sumOf { it.opponentPassAttempts },
                ),
            opponentPassYards = seasonStatsList.sumOf { it.opponentPassYards },
            opponentLongestPass = seasonStatsList.maxOfOrNull { it.opponentLongestPass } ?: 0,
            opponentPassTouchdowns = seasonStatsList.sumOf { it.opponentPassTouchdowns },
            opponentPassSuccesses = seasonStatsList.sumOf { it.opponentPassSuccesses },
            opponentPassSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentPassSuccesses },
                    seasonStatsList.sumOf { it.opponentPassAttempts },
                ),
            opponentRushAttempts = seasonStatsList.sumOf { it.opponentRushAttempts },
            opponentRushSuccesses = seasonStatsList.sumOf { it.opponentRushSuccesses },
            opponentRushSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentRushSuccesses },
                    seasonStatsList.sumOf { it.opponentRushAttempts },
                ),
            opponentRushYards = seasonStatsList.sumOf { it.opponentRushYards },
            opponentLongestRun = seasonStatsList.maxOfOrNull { it.opponentLongestRun } ?: 0,
            opponentRushTouchdowns = seasonStatsList.sumOf { it.opponentRushTouchdowns },
            opponentTotalYards = seasonStatsList.sumOf { it.opponentTotalYards },
            opponentAverageYardsPerPlay = calculateAverage(seasonStatsList.mapNotNull { it.opponentAverageYardsPerPlay }),
            opponentFirstDowns = seasonStatsList.sumOf { it.opponentFirstDowns },
            opponentFieldGoalMade = seasonStatsList.sumOf { it.opponentFieldGoalMade },
            opponentFieldGoalAttempts = seasonStatsList.sumOf { it.opponentFieldGoalAttempts },
            opponentFieldGoalPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentFieldGoalMade },
                    seasonStatsList.sumOf { it.opponentFieldGoalAttempts },
                ),
            opponentLongestFieldGoal = seasonStatsList.maxOfOrNull { it.opponentLongestFieldGoal } ?: 0,
            opponentFieldGoalTouchdown = seasonStatsList.sumOf { it.opponentFieldGoalTouchdown },
            opponentPuntsAttempted = seasonStatsList.sumOf { it.opponentPuntsAttempted },
            opponentLongestPunt = seasonStatsList.maxOfOrNull { it.opponentLongestPunt } ?: 0,
            opponentAveragePuntLength = calculateAverage(seasonStatsList.mapNotNull { it.opponentAveragePuntLength }),
            opponentPuntReturnTd = seasonStatsList.sumOf { it.opponentPuntReturnTd },
            opponentPuntReturnTdPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentPuntReturnTd },
                    seasonStatsList.sumOf { it.opponentPuntsAttempted },
                ),
            opponentNumberOfKickoffs = seasonStatsList.sumOf { it.opponentNumberOfKickoffs },
            opponentOnsideAttempts = seasonStatsList.sumOf { it.opponentOnsideAttempts },
            opponentOnsideSuccess = seasonStatsList.sumOf { it.opponentOnsideSuccess },
            opponentOnsideSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentOnsideSuccess },
                    seasonStatsList.sumOf { it.opponentOnsideAttempts },
                ),
            opponentNormalKickoffAttempts = seasonStatsList.sumOf { it.opponentNormalKickoffAttempts },
            opponentTouchbacks = seasonStatsList.sumOf { it.opponentTouchbacks },
            opponentTouchbackPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentTouchbacks },
                    seasonStatsList.sumOf { it.opponentNormalKickoffAttempts },
                ),
            opponentKickReturnTd = seasonStatsList.sumOf { it.opponentKickReturnTd },
            opponentKickReturnTdPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentKickReturnTd },
                    seasonStatsList.sumOf { it.opponentNumberOfKickoffs },
                ),
            opponentNumberOfDrives = seasonStatsList.sumOf { it.opponentNumberOfDrives },
            opponentTimeOfPossession = seasonStatsList.sumOf { it.opponentTimeOfPossession },
            opponentTouchdowns = seasonStatsList.sumOf { it.opponentTouchdowns },
            opponentThirdDownConversionSuccess = seasonStatsList.sumOf { it.opponentThirdDownConversionSuccess },
            opponentThirdDownConversionAttempts = seasonStatsList.sumOf { it.opponentThirdDownConversionAttempts },
            opponentThirdDownConversionPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentThirdDownConversionSuccess },
                    seasonStatsList.sumOf { it.opponentThirdDownConversionAttempts },
                ),
            opponentFourthDownConversionSuccess = seasonStatsList.sumOf { it.opponentFourthDownConversionSuccess },
            opponentFourthDownConversionAttempts = seasonStatsList.sumOf { it.opponentFourthDownConversionAttempts },
            opponentFourthDownConversionPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentFourthDownConversionSuccess },
                    seasonStatsList.sumOf { it.opponentFourthDownConversionAttempts },
                ),
            opponentRedZoneAttempts = seasonStatsList.sumOf { it.opponentRedZoneAttempts },
            opponentRedZoneSuccesses = seasonStatsList.sumOf { it.opponentRedZoneSuccesses },
            opponentRedZoneSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentRedZoneSuccesses },
                    seasonStatsList.sumOf { it.opponentRedZoneAttempts },
                ),
            opponentRedZonePercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.opponentRedZoneAttempts },
                    seasonStatsList.sumOf { it.opponentNumberOfDrives },
                ),
            lastModifiedTs = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT),
        )
    }

    /**
     * Calculate average of a list of doubles
     */
    private fun calculateAverage(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        return values.average()
    }

    /**
     * Calculate percentage from totals (successes/attempts * 100)
     */
    private fun calculatePercentage(
        successes: Int,
        attempts: Int,
    ): Double? {
        if (attempts == 0) return null
        return (successes.toDouble() / attempts.toDouble()) * 100.0
    }
}
