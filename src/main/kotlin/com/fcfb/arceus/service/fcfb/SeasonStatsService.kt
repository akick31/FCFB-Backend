package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.SeasonStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class SeasonStatsService(
    private val seasonStatsRepository: SeasonStatsRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val teamRepository: TeamRepository,
    private val conferenceStatsService: ConferenceStatsService,
) {
    /**
     * Filter out scrimmage games from game stats
     */
    private fun filterOutScrimmageGames(gameStatsList: List<GameStats>): List<GameStats> {
        return gameStatsList.filter { it.gameType != GameType.SCRIMMAGE }
    }

    /**
     * Get all season stats
     */
    fun getAllSeasonStats(): List<SeasonStats> {
        return seasonStatsRepository.findAllByOrderBySeasonNumberDescTeamAsc()
    }

    /**
     * Get season stats for a specific team and season
     */
    fun getSeasonStatsByTeamAndSeason(
        team: String,
        seasonNumber: Int,
    ): SeasonStats? {
        return seasonStatsRepository.findByTeamAndSeasonNumber(team, seasonNumber)
    }

    /**
     * Get all season stats for a specific team
     */
    fun getSeasonStatsByTeam(team: String): List<SeasonStats> {
        return seasonStatsRepository.findByTeamOrderBySeasonNumberDesc(team)
    }

    /**
     * Get all season stats for a specific season
     */
    fun getSeasonStatsBySeason(seasonNumber: Int): List<SeasonStats> {
        return seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
    }

    /**
     * Generate season stats for all seasons
     */
    fun generateAllSeasonStats() {
        Logger.info("Starting generation of all season stats")

        // Clear existing season stats
        seasonStatsRepository.deleteAll()

        // Get all unique team-season combinations (excluding scrimmage games)
        val allGameStats = filterOutScrimmageGames(gameStatsRepository.findAll().toList())
        val teamSeasonCombinations =
            allGameStats
                .map { "${it.team}_${it.season}" }
                .distinct()

        for (combination in teamSeasonCombinations) {
            val (team, seasonStr) = combination.split("_")
            val season = seasonStr.toInt()
            generateSeasonStatsForTeam(team, season)
        }

        Logger.info("Completed generation of all season stats")
    }

    /**
     * Generate season stats for a specific team and season
     */
    fun generateSeasonStatsForTeam(
        team: String,
        seasonNumber: Int,
    ) {
        Logger.info("Generating season stats for $team in season $seasonNumber")

        // Delete existing season stats for this team and season
        seasonStatsRepository.deleteByTeamAndSeasonNumber(team, seasonNumber)

        // Get all game stats for this team and season (excluding scrimmage games)
        val teamGameStats =
            filterOutScrimmageGames(
                gameStatsRepository.findAll()
                    .filter { it.team == team && it.season == seasonNumber },
            )

        if (teamGameStats.isEmpty()) {
            Logger.warn("No game stats found for $team in season $seasonNumber")
            return
        }

        // Create season stats by aggregating game stats
        val seasonStats = aggregateGameStatsToSeasonStats(teamGameStats, team, seasonNumber)

        seasonStatsRepository.save(seasonStats)
        Logger.info("Completed generating season stats for $team in season $seasonNumber")
    }

    /**
     * Update season stats when a game ends
     */
    fun updateSeasonStatsForGame(gameStats: GameStats) {
        val team = gameStats.team ?: return
        val season = gameStats.season ?: return
        val subdivision = gameStats.subdivision ?: return

        // Regenerate season stats for this team and season
        generateSeasonStatsForTeam(team, season)

        // Update conference stats for this subdivision and season
        conferenceStatsService.updateConferenceStatsForSeasonStats(
            seasonStatsRepository.findByTeamAndSeasonNumber(team, season) ?: return,
        )
    }

    /**
     * Aggregate game stats into season stats
     */
    private fun aggregateGameStatsToSeasonStats(
        gameStatsList: List<GameStats>,
        team: String,
        seasonNumber: Int,
    ): SeasonStats {
        val firstGameStats = gameStatsList.first()

        // Get conference from Team entity instead of GameStats
        val teamEntity = teamRepository.findByName(team)
        val conference = teamEntity?.conference

        return SeasonStats(
            team = team,
            seasonNumber = seasonNumber,
            // This is a simplified win calculation
            wins = gameStatsList.count { it.score > 0 },
            // This is a simplified loss calculation
            losses = gameStatsList.count { it.score <= 0 },
            subdivision = firstGameStats.subdivision,
            conference = conference,
            offensivePlaybook = firstGameStats.offensivePlaybook,
            defensivePlaybook = firstGameStats.defensivePlaybook,
            // Aggregate all the stats
            passAttempts = gameStatsList.sumOf { it.passAttempts },
            passCompletions = gameStatsList.sumOf { it.passCompletions },
            passCompletionPercentage = calculateAverage(gameStatsList.mapNotNull { it.passCompletionPercentage }),
            passYards = gameStatsList.sumOf { it.passYards },
            longestPass = gameStatsList.maxOfOrNull { it.longestPass } ?: 0,
            passTouchdowns = gameStatsList.sumOf { it.passTouchdowns },
            passSuccesses = gameStatsList.sumOf { it.passSuccesses },
            passSuccessPercentage = calculateAverage(gameStatsList.mapNotNull { it.passSuccessPercentage }),
            rushAttempts = gameStatsList.sumOf { it.rushAttempts },
            rushSuccesses = gameStatsList.sumOf { it.rushSuccesses },
            rushSuccessPercentage = calculateAverage(gameStatsList.mapNotNull { it.rushSuccessPercentage }),
            rushYards = gameStatsList.sumOf { it.rushYards },
            longestRun = gameStatsList.maxOfOrNull { it.longestRun } ?: 0,
            rushTouchdowns = gameStatsList.sumOf { it.rushTouchdowns },
            totalYards = gameStatsList.sumOf { it.totalYards },
            averageYardsPerPlay = calculateAverage(gameStatsList.mapNotNull { it.averageYardsPerPlay }),
            firstDowns = gameStatsList.sumOf { it.firstDowns },
            sacksAllowed = gameStatsList.sumOf { it.sacksAllowed },
            sacksForced = gameStatsList.sumOf { it.sacksForced },
            interceptionsLost = gameStatsList.sumOf { it.interceptionsLost },
            interceptionsForced = gameStatsList.sumOf { it.interceptionsForced },
            fumblesLost = gameStatsList.sumOf { it.fumblesLost },
            fumblesForced = gameStatsList.sumOf { it.fumblesForced },
            turnoversLost = gameStatsList.sumOf { it.turnoversLost },
            turnoversForced = gameStatsList.sumOf { it.turnoversForced },
            turnoverDifferential = gameStatsList.sumOf { it.turnoverDifferential },
            turnoverTouchdownsLost = gameStatsList.sumOf { it.turnoverTouchdownsLost },
            turnoverTouchdownsForced = gameStatsList.sumOf { it.turnoverTouchdownsForced },
            pickSixesThrown = gameStatsList.sumOf { it.pickSixesThrown },
            pickSixesForced = gameStatsList.sumOf { it.pickSixesForced },
            fumbleReturnTdsCommitted = gameStatsList.sumOf { it.fumbleReturnTdsCommitted },
            fumbleReturnTdsForced = gameStatsList.sumOf { it.fumbleReturnTdsForced },
            fieldGoalMade = gameStatsList.sumOf { it.fieldGoalMade },
            fieldGoalAttempts = gameStatsList.sumOf { it.fieldGoalAttempts },
            fieldGoalPercentage = calculateAverage(gameStatsList.mapNotNull { it.fieldGoalPercentage }),
            longestFieldGoal = gameStatsList.maxOfOrNull { it.longestFieldGoal } ?: 0,
            blockedOpponentFieldGoals = gameStatsList.sumOf { it.blockedOpponentFieldGoals },
            fieldGoalTouchdown = gameStatsList.sumOf { it.fieldGoalTouchdown },
            puntsAttempted = gameStatsList.sumOf { it.puntsAttempted },
            longestPunt = gameStatsList.maxOfOrNull { it.longestPunt } ?: 0,
            averagePuntLength = calculateAverage(gameStatsList.mapNotNull { it.averagePuntLength }),
            blockedOpponentPunt = gameStatsList.sumOf { it.blockedOpponentPunt },
            puntReturnTd = gameStatsList.sumOf { it.puntReturnTd },
            puntReturnTdPercentage = calculateAverage(gameStatsList.mapNotNull { it.puntReturnTdPercentage }),
            numberOfKickoffs = gameStatsList.sumOf { it.numberOfKickoffs },
            onsideAttempts = gameStatsList.sumOf { it.onsideAttempts },
            onsideSuccess = gameStatsList.sumOf { it.onsideSuccess },
            onsideSuccessPercentage = calculateAverage(gameStatsList.mapNotNull { it.onsideSuccessPercentage }),
            normalKickoffAttempts = gameStatsList.sumOf { it.normalKickoffAttempts },
            touchbacks = gameStatsList.sumOf { it.touchbacks },
            touchbackPercentage = calculateAverage(gameStatsList.mapNotNull { it.touchbackPercentage }),
            kickReturnTd = gameStatsList.sumOf { it.kickReturnTd },
            kickReturnTdPercentage = calculateAverage(gameStatsList.mapNotNull { it.kickReturnTdPercentage }),
            numberOfDrives = gameStatsList.sumOf { it.numberOfDrives },
            timeOfPossession = gameStatsList.sumOf { it.timeOfPossession },
            touchdowns = gameStatsList.sumOf { it.touchdowns },
            thirdDownConversionSuccess = gameStatsList.sumOf { it.thirdDownConversionSuccess },
            thirdDownConversionAttempts = gameStatsList.sumOf { it.thirdDownConversionAttempts },
            thirdDownConversionPercentage = calculateAverage(gameStatsList.mapNotNull { it.thirdDownConversionPercentage }),
            fourthDownConversionSuccess = gameStatsList.sumOf { it.fourthDownConversionSuccess },
            fourthDownConversionAttempts = gameStatsList.sumOf { it.fourthDownConversionAttempts },
            fourthDownConversionPercentage = calculateAverage(gameStatsList.mapNotNull { it.fourthDownConversionPercentage }),
            largestLead = gameStatsList.maxOfOrNull { it.largestLead } ?: 0,
            largestDeficit = gameStatsList.maxOfOrNull { it.largestDeficit } ?: 0,
            redZoneAttempts = gameStatsList.sumOf { it.redZoneAttempts },
            redZoneSuccesses = gameStatsList.sumOf { it.redZoneSuccesses },
            redZoneSuccessPercentage = calculateAverage(gameStatsList.mapNotNull { it.redZoneSuccessPercentage }),
            redZonePercentage = calculateAverage(gameStatsList.mapNotNull { it.redZonePercentage }),
            safetiesForced = gameStatsList.sumOf { it.safetiesForced },
            safetiesCommitted = gameStatsList.sumOf { it.safetiesCommitted },
            averageOffensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveDiff }),
            averageDefensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveDiff }),
            averageOffensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveSpecialTeamsDiff }),
            averageDefensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveSpecialTeamsDiff }),
            averageDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDiff }),
            averageResponseSpeed = calculateAverage(gameStatsList.mapNotNull { it.averageResponseSpeed }),
            lastModifiedTs =
                ZonedDateTime.now(ZoneId.of("America/New_York"))
                    .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
        )
    }

    /**
     * Calculate average of a list of doubles, returning null if empty
     */
    private fun calculateAverage(values: List<Double>): Double? {
        return if (values.isNotEmpty()) {
            values.average()
        } else {
            null
        }
    }

    // ==================== COMPREHENSIVE CRUD METHODS ====================

    /**
     * Generate season stats for all teams in a specific season
     */
    fun generateSeasonStatsForSeason(seasonNumber: Int) {
        Logger.info("Generating season stats for all teams in season $seasonNumber")

        // Get all unique teams for this season (excluding scrimmage games)
        val allGameStats = filterOutScrimmageGames(gameStatsRepository.findAll().toList())
        val teams =
            allGameStats
                .filter { it.season == seasonNumber }
                .mapNotNull { it.team }
                .distinct()

        for (team in teams) {
            generateSeasonStatsForTeam(team, seasonNumber)
        }

        Logger.info("Completed generating season stats for all teams in season $seasonNumber")
    }

    /**
     * Generate season stats for all seasons for a specific team
     */
    fun generateSeasonStatsForAllSeasons(team: String) {
        Logger.info("Generating season stats for $team across all seasons")

        // Get all unique seasons for this team (excluding scrimmage games)
        val allGameStats = filterOutScrimmageGames(gameStatsRepository.findAll().toList())
        val seasons =
            allGameStats
                .filter { it.team == team }
                .mapNotNull { it.season }
                .distinct()
                .sorted()

        for (season in seasons) {
            generateSeasonStatsForTeam(team, season)
        }

        Logger.info("Completed generating season stats for $team across all seasons")
    }

    /**
     * Get season stats for a specific stat across all teams and seasons
     */
    fun getSeasonStatsByStat(statName: String): List<SeasonStats> {
        // This would require a custom repository method to filter by specific stat fields
        // For now, return all season stats and let the caller filter
        return getAllSeasonStats()
    }

    /**
     * Get season stats for a specific stat for a specific team
     */
    fun getSeasonStatsByTeamAndStat(
        team: String,
        statName: String,
    ): List<SeasonStats> {
        // This would require a custom repository method to filter by specific stat fields
        // For now, return all season stats for the team and let the caller filter
        return getSeasonStatsByTeam(team)
    }

    /**
     * Get season stats for a specific stat for a specific team and season
     */
    fun getSeasonStatsByTeamSeasonAndStat(
        team: String,
        seasonNumber: Int,
        statName: String,
    ): SeasonStats? {
        // This would require a custom repository method to filter by specific stat fields
        // For now, return the season stats for the team and season and let the caller filter
        return getSeasonStatsByTeamAndSeason(team, seasonNumber)
    }

    /**
     * Get season stats for a specific stat across all teams in a specific season
     */
    fun getSeasonStatsBySeasonAndStat(
        seasonNumber: Int,
        statName: String,
    ): List<SeasonStats> {
        // This would require a custom repository method to filter by specific stat fields
        // For now, return all season stats for the season and let the caller filter
        return getSeasonStatsBySeason(seasonNumber)
    }

    /**
     * Delete season stats for a specific team and season
     */
    fun deleteSeasonStatsForTeam(
        team: String,
        seasonNumber: Int,
    ) {
        Logger.info("Deleting season stats for $team in season $seasonNumber")
        seasonStatsRepository.deleteByTeamAndSeasonNumber(team, seasonNumber)
        Logger.info("Completed deleting season stats for $team in season $seasonNumber")
    }

    /**
     * Delete all season stats for a specific team
     */
    fun deleteSeasonStatsForTeam(team: String) {
        Logger.info("Deleting all season stats for $team")
        seasonStatsRepository.deleteByTeam(team)
        Logger.info("Completed deleting all season stats for $team")
    }

    /**
     * Delete all season stats for a specific season
     */
    fun deleteSeasonStatsForSeason(seasonNumber: Int) {
        Logger.info("Deleting all season stats for season $seasonNumber")
        seasonStatsRepository.deleteBySeasonNumber(seasonNumber)
        Logger.info("Completed deleting all season stats for season $seasonNumber")
    }

    /**
     * Get all unique teams that have season stats
     */
    fun getAllTeamsWithSeasonStats(): List<String> {
        return seasonStatsRepository.findAll()
            .map { it.team }
            .distinct()
            .sorted()
    }

    /**
     * Get all unique seasons that have season stats
     */
    fun getAllSeasonsWithSeasonStats(): List<Int> {
        return seasonStatsRepository.findAll()
            .map { it.seasonNumber }
            .distinct()
            .sorted()
    }

    /**
     * Get count of season stats records
     */
    fun getSeasonStatsCount(): Long {
        return seasonStatsRepository.count()
    }

    /**
     * Get count of season stats records for a specific team
     */
    fun getSeasonStatsCountForTeam(team: String): Long {
        return seasonStatsRepository.countByTeam(team)
    }

    /**
     * Get count of season stats records for a specific season
     */
    fun getSeasonStatsCountForSeason(seasonNumber: Int): Long {
        return seasonStatsRepository.countBySeasonNumber(seasonNumber)
    }

    /**
     * Check if season stats exist for a specific team and season
     */
    fun seasonStatsExistForTeam(
        team: String,
        seasonNumber: Int,
    ): Boolean {
        return seasonStatsRepository.existsByTeamAndSeasonNumber(team, seasonNumber)
    }

    /**
     * Check if season stats exist for a specific team
     */
    fun seasonStatsExistForTeam(team: String): Boolean {
        return seasonStatsRepository.existsByTeam(team)
    }

    /**
     * Check if season stats exist for a specific season
     */
    fun seasonStatsExistForSeason(seasonNumber: Int): Boolean {
        return seasonStatsRepository.existsBySeasonNumber(seasonNumber)
    }

    /**
     * Get leaderboard for a specific stat
     */
    fun getLeaderboard(
        statName: String,
        seasonNumber: Int? = null,
        subdivision: String? = null,
        conference: String? = null,
        limit: Int = 10,
        ascending: Boolean = false,
    ): List<SeasonStats> {
        val allStats = getAllSeasonStats()

        val filteredStats =
            allStats.filter { stats ->
                seasonNumber?.let { stats.seasonNumber == it } ?: true &&
                    subdivision?.let { stats.subdivision?.name.equals(it, ignoreCase = true) } ?: true &&
                    conference?.let { stats.conference?.name.equals(it, ignoreCase = true) } ?: true
            }

        return when (statName.lowercase()) {
            "wins" -> filteredStats.sortedByDescending { it.wins }
            "losses" -> filteredStats.sortedByDescending { it.losses }
            "passattempts" -> filteredStats.sortedByDescending { it.passAttempts }
            "passcompletions" -> filteredStats.sortedByDescending { it.passCompletions }
            "passcompletionpercentage" -> filteredStats.sortedByDescending { it.passCompletionPercentage ?: 0.0 }
            "passyards" -> filteredStats.sortedByDescending { it.passYards }
            "longestpass" -> filteredStats.sortedByDescending { it.longestPass }
            "passtouchdowns" -> filteredStats.sortedByDescending { it.passTouchdowns }
            "passsuccesses" -> filteredStats.sortedByDescending { it.passSuccesses }
            "passsuccesspercentage" -> filteredStats.sortedByDescending { it.passSuccessPercentage ?: 0.0 }
            "rushattempts" -> filteredStats.sortedByDescending { it.rushAttempts }
            "rushsuccesses" -> filteredStats.sortedByDescending { it.rushSuccesses }
            "rushsuccesspercentage" -> filteredStats.sortedByDescending { it.rushSuccessPercentage ?: 0.0 }
            "rushyards" -> filteredStats.sortedByDescending { it.rushYards }
            "longestrun" -> filteredStats.sortedByDescending { it.longestRun }
            "rushtouchdowns" -> filteredStats.sortedByDescending { it.rushTouchdowns }
            "totalyards" -> filteredStats.sortedByDescending { it.totalYards }
            "averageyardsperplay" -> filteredStats.sortedByDescending { it.averageYardsPerPlay ?: 0.0 }
            "firstdowns" -> filteredStats.sortedByDescending { it.firstDowns }
            "sacksallowed" -> filteredStats.sortedByDescending { it.sacksAllowed }
            "sacksforced" -> filteredStats.sortedByDescending { it.sacksForced }
            "interceptionslost" -> filteredStats.sortedByDescending { it.interceptionsLost }
            "interceptionsforced" -> filteredStats.sortedByDescending { it.interceptionsForced }
            "fumbleslost" -> filteredStats.sortedByDescending { it.fumblesLost }
            "fumblesforced" -> filteredStats.sortedByDescending { it.fumblesForced }
            "turnoverslost" -> filteredStats.sortedByDescending { it.turnoversLost }
            "turnoversforced" -> filteredStats.sortedByDescending { it.turnoversForced }
            "turnoverdifferential" -> filteredStats.sortedByDescending { it.turnoverDifferential }
            "turnovertouchdownslost" -> filteredStats.sortedByDescending { it.turnoverTouchdownsLost }
            "turnovertouchdownsforced" -> filteredStats.sortedByDescending { it.turnoverTouchdownsForced }
            "picksixesthrown" -> filteredStats.sortedByDescending { it.pickSixesThrown }
            "picksixesforced" -> filteredStats.sortedByDescending { it.pickSixesForced }
            "fumblereturntdscommitted" -> filteredStats.sortedByDescending { it.fumbleReturnTdsCommitted }
            "fumblereturntdsforced" -> filteredStats.sortedByDescending { it.fumbleReturnTdsForced }
            "fieldgoalmade" -> filteredStats.sortedByDescending { it.fieldGoalMade }
            "fieldgoalattempts" -> filteredStats.sortedByDescending { it.fieldGoalAttempts }
            "fieldgoalpercentage" -> filteredStats.sortedByDescending { it.fieldGoalPercentage ?: 0.0 }
            "longestfieldgoal" -> filteredStats.sortedByDescending { it.longestFieldGoal }
            "blockedopponentfieldgoals" -> filteredStats.sortedByDescending { it.blockedOpponentFieldGoals }
            "fieldgoaltouchdown" -> filteredStats.sortedByDescending { it.fieldGoalTouchdown }
            "puntsattempted" -> filteredStats.sortedByDescending { it.puntsAttempted }
            "longestpunt" -> filteredStats.sortedByDescending { it.longestPunt }
            "averagepuntlength" -> filteredStats.sortedByDescending { it.averagePuntLength ?: 0.0 }
            "blockedopponentpunt" -> filteredStats.sortedByDescending { it.blockedOpponentPunt }
            "puntreturntd" -> filteredStats.sortedByDescending { it.puntReturnTd }
            "puntreturntdpercentage" -> filteredStats.sortedByDescending { it.puntReturnTdPercentage ?: 0.0 }
            "numberofkickoffs" -> filteredStats.sortedByDescending { it.numberOfKickoffs }
            "onsideattempts" -> filteredStats.sortedByDescending { it.onsideAttempts }
            "onsidesuccess" -> filteredStats.sortedByDescending { it.onsideSuccess }
            "onsidesuccesspercentage" -> filteredStats.sortedByDescending { it.onsideSuccessPercentage ?: 0.0 }
            "normalkickoffattempts" -> filteredStats.sortedByDescending { it.normalKickoffAttempts }
            "touchbacks" -> filteredStats.sortedByDescending { it.touchbacks }
            "touchbackpercentage" -> filteredStats.sortedByDescending { it.touchbackPercentage ?: 0.0 }
            "kickreturntd" -> filteredStats.sortedByDescending { it.kickReturnTd }
            "kickreturntdpercentage" -> filteredStats.sortedByDescending { it.kickReturnTdPercentage ?: 0.0 }
            "numberofdrives" -> filteredStats.sortedByDescending { it.numberOfDrives }
            "timeofpossession" -> filteredStats.sortedByDescending { it.timeOfPossession }
            "touchdowns" -> filteredStats.sortedByDescending { it.touchdowns }
            "thirddownconversionsuccess" -> filteredStats.sortedByDescending { it.thirdDownConversionSuccess }
            "thirddownconversionattempts" -> filteredStats.sortedByDescending { it.thirdDownConversionAttempts }
            "thirddownconversionpercentage" -> filteredStats.sortedByDescending { it.thirdDownConversionPercentage ?: 0.0 }
            "fourthdownconversionsuccess" -> filteredStats.sortedByDescending { it.fourthDownConversionSuccess }
            "fourthdownconversionattempts" -> filteredStats.sortedByDescending { it.fourthDownConversionAttempts }
            "fourthdownconversionpercentage" -> filteredStats.sortedByDescending { it.fourthDownConversionPercentage ?: 0.0 }
            "largestlead" -> filteredStats.sortedByDescending { it.largestLead }
            "largestdeficit" -> filteredStats.sortedByDescending { it.largestDeficit }
            "redzoneattempts" -> filteredStats.sortedByDescending { it.redZoneAttempts }
            "redzonesuccesses" -> filteredStats.sortedByDescending { it.redZoneSuccesses }
            "redzonesuccesspercentage" -> filteredStats.sortedByDescending { it.redZoneSuccessPercentage ?: 0.0 }
            "redzonepercentage" -> filteredStats.sortedByDescending { it.redZonePercentage ?: 0.0 }
            "safetiesforced" -> filteredStats.sortedByDescending { it.safetiesForced }
            "safetiescommitted" -> filteredStats.sortedByDescending { it.safetiesCommitted }
            "averageoffensivediff" -> filteredStats.sortedByDescending { it.averageOffensiveDiff ?: 0.0 }
            "averagedefensivediff" -> filteredStats.sortedByDescending { it.averageDefensiveDiff ?: 0.0 }
            "averageoffensivespecialteamsdiff" -> filteredStats.sortedByDescending { it.averageOffensiveSpecialTeamsDiff ?: 0.0 }
            "averagedefensivespecialteamsdiff" -> filteredStats.sortedByDescending { it.averageDefensiveSpecialTeamsDiff ?: 0.0 }
            "averagediff" -> filteredStats.sortedByDescending { it.averageDiff ?: 0.0 }
            "averageresponsespeed" -> filteredStats.sortedByDescending { it.averageResponseSpeed ?: 0.0 }
            else -> filteredStats
        }.let { sortedStats ->
            if (ascending) {
                sortedStats.reversed()
            } else {
                sortedStats
            }
        }.take(limit)
    }
}
