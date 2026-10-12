package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.PostseasonSeasonStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.SeasonStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.fcfb.team.TeamSeasonConferenceService
import com.fcfb.arceus.service.specification.SeasonStatsSpecificationService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.POSTSEASON_START_WEEK
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class SeasonStatsService(
    private val seasonStatsRepository: SeasonStatsRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val teamRepository: TeamRepository,
    private val conferenceStatsService: ConferenceStatsService,
    private val seasonStatsSpecificationService: SeasonStatsSpecificationService,
    private val teamSeasonConferenceService: TeamSeasonConferenceService,
    private val postseasonSeasonStatsService: PostseasonSeasonStatsService,
) {
    fun getFilteredSeasonStats(
        team: String?,
        season: Int?,
        pageable: Pageable,
    ): Page<SeasonStats> {
        val spec = seasonStatsSpecificationService.createSpecification(team, season)
        val sortOrders = seasonStatsSpecificationService.createSort()

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

        return seasonStatsRepository.findAll(spec, sortedPageable)
    }

    private fun filterOutScrimmageGames(gameStatsList: List<GameStats>): List<GameStats> {
        return gameStatsList.filter { it.gameType != GameType.SCRIMMAGE }
    }

    private fun filterRegularSeasonGames(gameStatsList: List<GameStats>): List<GameStats> {
        return gameStatsList.filter { (it.week ?: 0) < POSTSEASON_START_WEEK }
    }

    fun generateAllSeasonStats() {
        Logger.info("Starting generation of all season stats")

        val allGameStats = filterRegularSeasonGames(filterOutScrimmageGames(gameStatsRepository.findAll().toList()))
        val gameStatsByGameId = allGameStats.groupBy { it.gameId }
        val gameStatsByTeamSeason = allGameStats.groupBy { "${it.team}_${it.season}" }
        val teamsByName = teamRepository.findAll().associateBy { it.name }

        val total = gameStatsByTeamSeason.size
        Logger.info("Generating season stats for $total team-season combinations")
        gameStatsByTeamSeason.entries.forEachIndexed { index, (combination, teamGameStats) ->
            val (team, seasonStr) = combination.split("_")
            val season = seasonStr.toInt()
            Logger.info("Generating season stats for $team in season $season (${index + 1}/$total)")
            val conference = teamSeasonConferenceService.getConference(team, season) ?: teamsByName[team]?.conference
            val seasonStats =
                SeasonStatsCalculator.aggregateGameStatsToSeasonStats(teamGameStats, team, season, gameStatsByGameId, conference)
            seasonStatsRepository.findByTeamAndSeasonNumber(team, season)?.let { seasonStats.id = it.id }
            seasonStatsRepository.save(seasonStats)
        }

        val staleSeasonStats =
            seasonStatsRepository.findAll().filterNot { "${it.team}_${it.seasonNumber}" in gameStatsByTeamSeason.keys }
        if (staleSeasonStats.isNotEmpty()) {
            seasonStatsRepository.deleteAll(staleSeasonStats)
        }

        Logger.info("Completed generation of all season stats")
    }

    fun generateSeasonStatsForTeam(
        team: String,
        seasonNumber: Int,
    ) {
        Logger.info("Generating season stats for $team in season $seasonNumber")

        val teamGameStats =
            filterRegularSeasonGames(
                filterOutScrimmageGames(
                    gameStatsRepository.findAll()
                        .filter { it.team == team && it.season == seasonNumber },
                ),
            )

        if (teamGameStats.isEmpty()) {
            Logger.warn("No game stats found for $team in season $seasonNumber")
            seasonStatsRepository.deleteByTeamAndSeasonNumber(team, seasonNumber)
            return
        }

        val gameIds = teamGameStats.map { it.gameId }.toSet()
        val gameStatsByGameId =
            gameStatsRepository.findAll()
                .filter { it.gameId in gameIds }
                .groupBy { it.gameId }

        val conference = teamSeasonConferenceService.getConference(team, seasonNumber) ?: teamRepository.findByName(team)?.conference

        val seasonStats =
            SeasonStatsCalculator.aggregateGameStatsToSeasonStats(teamGameStats, team, seasonNumber, gameStatsByGameId, conference)
        seasonStatsRepository.findByTeamAndSeasonNumber(team, seasonNumber)?.let { seasonStats.id = it.id }

        seasonStatsRepository.save(seasonStats)
        Logger.info("Completed generating season stats for $team in season $seasonNumber")
    }

    fun updateSeasonStatsForGame(gameStats: GameStats) {
        val team = gameStats.team ?: return
        val season = gameStats.season ?: return
        val subdivision = gameStats.subdivision ?: return

        generateSeasonStatsForTeam(team, season)

        conferenceStatsService.updateConferenceStatsForSeasonStats(
            seasonStatsRepository.findByTeamAndSeasonNumber(team, season) ?: return,
        )
    }

    fun getLeaderboard(
        statName: String,
        season: Int? = null,
        subdivision: String? = null,
        conference: String? = null,
        limit: Int = 10,
        ascending: Boolean = false,
    ): List<SeasonStats> {
        val allStatsPage =
            getFilteredSeasonStats(
                team = null,
                season = null,
                pageable = Pageable.unpaged(),
            )

        val allStats = allStatsPage.content

        val filteredStats =
            allStats.filter { stats ->
                (season?.let { stats.seasonNumber == it } ?: true) &&
                    (subdivision?.let { stats.subdivision?.name.equals(it, ignoreCase = true) } ?: true) &&
                    (conference?.let { stats.conference.equals(it, ignoreCase = true) } ?: true)
            }

        val normalizedStatName = statName.lowercase().replace("_", "")

        return when (normalizedStatName) {
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
            "averageoffensivediff" -> {
                val sorted = filteredStats.sortedBy { it.averageOffensiveDiff ?: Double.MAX_VALUE }
                if (ascending) sorted.reversed() else sorted
            }
            "averagedefensivediff" -> filteredStats.sortedByDescending { it.averageDefensiveDiff ?: 0.0 }
            "averageoffensivespecialteamsdiff" -> filteredStats.sortedByDescending { it.averageOffensiveSpecialTeamsDiff ?: 0.0 }
            "averagedefensivespecialteamsdiff" -> filteredStats.sortedByDescending { it.averageDefensiveSpecialTeamsDiff ?: 0.0 }
            "averagediff" -> filteredStats.sortedByDescending { it.averageDiff ?: 0.0 }
            "averageresponsespeed" -> filteredStats.sortedByDescending { it.averageResponseSpeed ?: 0.0 }
            else -> {
                Logger.warn("Unknown stat name in leaderboard: $statName (normalized: $normalizedStatName)")
                filteredStats.sortedBy { it.team }
            }
        }.let { sortedStats ->
            if ("averageoffensivediff" != normalizedStatName && "pointdifferential" != normalizedStatName) {
                if (ascending) {
                    sortedStats.reversed()
                } else {
                    sortedStats
                }
            } else {
                sortedStats
            }
        }.take(limit)
    }

    fun getFilteredPostseasonSeasonStats(
        team: String?,
        season: Int?,
        pageable: Pageable,
    ): Page<PostseasonSeasonStats> = postseasonSeasonStatsService.getFilteredPostseasonSeasonStats(team, season, pageable)

    fun generateAllPostseasonSeasonStats() = postseasonSeasonStatsService.generateAllPostseasonSeasonStats()
}
