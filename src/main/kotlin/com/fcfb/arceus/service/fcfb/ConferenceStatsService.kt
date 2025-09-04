package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.ConferenceStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.ConferenceStatsRepository
import com.fcfb.arceus.repositories.SeasonStatsRepository
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class ConferenceStatsService(
    private val conferenceStatsRepository: ConferenceStatsRepository,
    private val seasonStatsRepository: SeasonStatsRepository
) {

    /**
     * Get all conference stats
     */
    fun getAllConferenceStats(): List<ConferenceStats> {
        return conferenceStatsRepository.findAllByOrderBySeasonNumberDescSubdivisionAsc()
    }

    /**
     * Get conference stats for a specific subdivision, conference, and season
     */
    fun getConferenceStatsBySubdivisionAndConferenceAndSeason(subdivision: Subdivision, conference: Conference, seasonNumber: Int): ConferenceStats? {
        return conferenceStatsRepository.findBySubdivisionAndConferenceAndSeasonNumber(subdivision, conference, seasonNumber)
    }

    /**
     * Get all conference stats for a specific subdivision and season
     */
    fun getConferenceStatsBySubdivisionAndSeason(subdivision: Subdivision, seasonNumber: Int): List<ConferenceStats> {
        return conferenceStatsRepository.findBySubdivisionAndSeasonNumber(subdivision, seasonNumber)
    }

    /**
     * Get all conference stats for a specific conference and season
     */
    fun getConferenceStatsByConferenceAndSeason(conference: Conference, seasonNumber: Int): List<ConferenceStats> {
        return conferenceStatsRepository.findByConferenceAndSeasonNumber(conference, seasonNumber)
    }

    /**
     * Get all conference stats for a specific subdivision
     */
    fun getConferenceStatsBySubdivision(subdivision: Subdivision): List<ConferenceStats> {
        return conferenceStatsRepository.findBySubdivisionOrderBySeasonNumberDesc(subdivision)
    }

    /**
     * Get all conference stats for a specific conference
     */
    fun getConferenceStatsByConference(conference: Conference): List<ConferenceStats> {
        return conferenceStatsRepository.findByConferenceOrderBySeasonNumberDesc(conference)
    }

    /**
     * Get all conference stats for a specific season
     */
    fun getConferenceStatsBySeason(seasonNumber: Int): List<ConferenceStats> {
        return conferenceStatsRepository.findBySeasonNumberOrderBySubdivisionAsc(seasonNumber)
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
        val groupedStats = allSeasonStats.groupBy { 
            Triple(it.subdivision, it.conference, it.seasonNumber) 
        }.filterKeys { it.first != null && it.second != null }
        
        // Generate conference stats for each subdivision/conference/season combination
        for ((subdivisionConferenceSeason, seasonStatsList) in groupedStats) {
            val subdivision = subdivisionConferenceSeason.first!!
            val conference = subdivisionConferenceSeason.second!!
            val seasonNumber = subdivisionConferenceSeason.third
            
            Logger.info("Generating conference stats for $subdivision/$conference in season $seasonNumber with ${seasonStatsList.size} teams")
            generateConferenceStatsForSubdivisionAndConferenceAndSeason(subdivision, conference, seasonNumber)
        }
        
        Logger.info("Completed generation of all conference stats")
    }

    /**
     * Generate conference stats for a specific subdivision and season (all conferences)
     */
    fun generateConferenceStatsForSubdivisionAndSeason(subdivision: Subdivision, seasonNumber: Int) {
        Logger.info("Starting generation of conference stats for $subdivision in season $seasonNumber")
        
        // Get all season stats for this subdivision and season
        val seasonStatsList = seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
            .filter { seasonStats -> seasonStats.subdivision == subdivision }
        
        if (seasonStatsList.isEmpty()) {
            Logger.warn("No season stats found for $subdivision in season $seasonNumber")
            return
        }
        
        // Group by conference and generate conference stats for each conference
        val groupedByConference = seasonStatsList.groupBy { it.conference }
        
        for ((conference, conferenceSeasonStats) in groupedByConference) {
            if (conference != null) {
                generateConferenceStatsForSubdivisionAndConferenceAndSeason(subdivision, conference, seasonNumber)
            }
        }
        
        Logger.info("Completed generating conference stats for $subdivision in season $seasonNumber")
    }

    /**
     * Generate conference stats for a specific subdivision, conference, and season
     */
    fun generateConferenceStatsForSubdivisionAndConferenceAndSeason(subdivision: Subdivision, conference: Conference, seasonNumber: Int) {
        Logger.info("Starting generation of conference stats for $subdivision/$conference in season $seasonNumber")
        
        // Get all season stats for this subdivision, conference, and season
        val seasonStatsList = seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
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
        seasonNumber: Int
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
            passCompletionPercentage = calculateAverage(seasonStatsList.mapNotNull { it.passCompletionPercentage }),
            passYards = seasonStatsList.sumOf { it.passYards },
            longestPass = seasonStatsList.maxOfOrNull { it.longestPass } ?: 0,
            passTouchdowns = seasonStatsList.sumOf { it.passTouchdowns },
            passSuccesses = seasonStatsList.sumOf { it.passSuccesses },
            passSuccessPercentage = calculateAverage(seasonStatsList.mapNotNull { it.passSuccessPercentage }),
            
            rushAttempts = seasonStatsList.sumOf { it.rushAttempts },
            rushSuccesses = seasonStatsList.sumOf { it.rushSuccesses },
            rushSuccessPercentage = calculateAverage(seasonStatsList.mapNotNull { it.rushSuccessPercentage }),
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
            fieldGoalPercentage = calculateAverage(seasonStatsList.mapNotNull { it.fieldGoalPercentage }),
            longestFieldGoal = seasonStatsList.maxOfOrNull { it.longestFieldGoal } ?: 0,
            blockedOpponentFieldGoals = seasonStatsList.sumOf { it.blockedOpponentFieldGoals },
            fieldGoalTouchdown = seasonStatsList.sumOf { it.fieldGoalTouchdown },
            
            puntsAttempted = seasonStatsList.sumOf { it.puntsAttempted },
            longestPunt = seasonStatsList.maxOfOrNull { it.longestPunt } ?: 0,
            averagePuntLength = calculateAverage(seasonStatsList.mapNotNull { it.averagePuntLength }),
            blockedOpponentPunt = seasonStatsList.sumOf { it.blockedOpponentPunt },
            puntReturnTd = seasonStatsList.sumOf { it.puntReturnTd },
            puntReturnTdPercentage = calculateAverage(seasonStatsList.mapNotNull { it.puntReturnTdPercentage }),
            
            numberOfKickoffs = seasonStatsList.sumOf { it.numberOfKickoffs },
            onsideAttempts = seasonStatsList.sumOf { it.onsideAttempts },
            onsideSuccess = seasonStatsList.sumOf { it.onsideSuccess },
            onsideSuccessPercentage = calculateAverage(seasonStatsList.mapNotNull { it.onsideSuccessPercentage }),
            normalKickoffAttempts = seasonStatsList.sumOf { it.normalKickoffAttempts },
            touchbacks = seasonStatsList.sumOf { it.touchbacks },
            touchbackPercentage = calculateAverage(seasonStatsList.mapNotNull { it.touchbackPercentage }),
            kickReturnTd = seasonStatsList.sumOf { it.kickReturnTd },
            kickReturnTdPercentage = calculateAverage(seasonStatsList.mapNotNull { it.kickReturnTdPercentage }),
            
            numberOfDrives = seasonStatsList.sumOf { it.numberOfDrives },
            timeOfPossession = seasonStatsList.sumOf { it.timeOfPossession },
            
            touchdowns = seasonStatsList.sumOf { it.touchdowns },
            
            thirdDownConversionSuccess = seasonStatsList.sumOf { it.thirdDownConversionSuccess },
            thirdDownConversionAttempts = seasonStatsList.sumOf { it.thirdDownConversionAttempts },
            thirdDownConversionPercentage = calculateAverage(seasonStatsList.mapNotNull { it.thirdDownConversionPercentage }),
            fourthDownConversionSuccess = seasonStatsList.sumOf { it.fourthDownConversionSuccess },
            fourthDownConversionAttempts = seasonStatsList.sumOf { it.fourthDownConversionAttempts },
            fourthDownConversionPercentage = calculateAverage(seasonStatsList.mapNotNull { it.fourthDownConversionPercentage }),
            
            largestLead = seasonStatsList.maxOfOrNull { it.largestLead } ?: 0,
            largestDeficit = seasonStatsList.maxOfOrNull { it.largestDeficit } ?: 0,
            
            redZoneAttempts = seasonStatsList.sumOf { it.redZoneAttempts },
            redZoneSuccesses = seasonStatsList.sumOf { it.redZoneSuccesses },
            redZoneSuccessPercentage = calculateAverage(seasonStatsList.mapNotNull { it.redZoneSuccessPercentage }),
            redZonePercentage = calculateAverage(seasonStatsList.mapNotNull { it.redZonePercentage }),
            
            safetiesForced = seasonStatsList.sumOf { it.safetiesForced },
            safetiesCommitted = seasonStatsList.sumOf { it.safetiesCommitted },
            
            // Performance metrics are averages of team averages
            averageOffensiveDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageOffensiveDiff }),
            averageDefensiveDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDefensiveDiff }),
            averageOffensiveSpecialTeamsDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageOffensiveSpecialTeamsDiff }),
            averageDefensiveSpecialTeamsDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDefensiveSpecialTeamsDiff }),
            averageDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDiff }),
            averageResponseSpeed = calculateAverage(seasonStatsList.mapNotNull { it.averageResponseSpeed }),
            
            lastModifiedTs = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
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
     * Check if conference stats exist for a specific subdivision, conference, and season
     */
    fun conferenceStatsExistForSubdivisionAndConferenceAndSeason(subdivision: Subdivision, conference: Conference, seasonNumber: Int): Boolean {
        return conferenceStatsRepository.existsBySubdivisionAndConferenceAndSeasonNumber(subdivision, conference, seasonNumber)
    }

    /**
     * Check if conference stats exist for a specific subdivision and season
     */
    fun conferenceStatsExistForSubdivisionAndSeason(subdivision: Subdivision, seasonNumber: Int): Boolean {
        return conferenceStatsRepository.existsBySubdivisionAndSeasonNumber(subdivision, seasonNumber)
    }

    /**
     * Check if conference stats exist for a specific conference and season
     */
    fun conferenceStatsExistForConferenceAndSeason(conference: Conference, seasonNumber: Int): Boolean {
        return conferenceStatsRepository.existsByConferenceAndSeasonNumber(conference, seasonNumber)
    }

    /**
     * Check if conference stats exist for a specific subdivision
     */
    fun conferenceStatsExistForSubdivision(subdivision: Subdivision): Boolean {
        return conferenceStatsRepository.countBySubdivision(subdivision) > 0
    }

    /**
     * Check if conference stats exist for a specific conference
     */
    fun conferenceStatsExistForConference(conference: Conference): Boolean {
        return conferenceStatsRepository.countByConference(conference) > 0
    }

    /**
     * Check if conference stats exist for a specific season
     */
    fun conferenceStatsExistForSeason(seasonNumber: Int): Boolean {
        return conferenceStatsRepository.countBySeasonNumber(seasonNumber) > 0
    }

    /**
     * Diagnostic method to check data availability
     */
    fun diagnoseDataAvailability(): Map<String, Any> {
        val allSeasonStats = seasonStatsRepository.findAllByOrderBySeasonNumberDescTeamAsc()
        val seasonStatsWithSubdivision = allSeasonStats.count { it.subdivision != null }
        val seasonStatsWithConference = allSeasonStats.count { it.conference != null }
        val seasonStatsWithBoth = allSeasonStats.count { it.subdivision != null && it.conference != null }
        
        val groupedStats = allSeasonStats.groupBy { 
            Triple(it.subdivision, it.conference, it.seasonNumber) 
        }.filterKeys { it.first != null && it.second != null }
        
        return mapOf(
            "totalSeasonStats" to allSeasonStats.size,
            "seasonStatsWithSubdivision" to seasonStatsWithSubdivision,
            "seasonStatsWithConference" to seasonStatsWithConference,
            "seasonStatsWithBoth" to seasonStatsWithBoth,
            "validGroupings" to groupedStats.size,
            "sampleSeasonStats" to allSeasonStats.take(3).map { 
                mapOf(
                    "team" to it.team,
                    "season" to it.seasonNumber,
                    "subdivision" to it.subdivision,
                    "conference" to it.conference
                )
            }
        )
    }
}
