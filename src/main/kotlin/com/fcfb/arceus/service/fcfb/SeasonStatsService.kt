package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.SeasonStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.specification.SeasonStatsSpecificationService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.POSTSEASON_START_WEEK
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
    private val seasonStatsSpecificationService: SeasonStatsSpecificationService,
    private val teamSeasonConferenceService: TeamSeasonConferenceService,
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
                aggregateGameStatsToSeasonStats(teamGameStats, team, season, gameStatsByGameId, conference)
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
            aggregateGameStatsToSeasonStats(teamGameStats, team, seasonNumber, gameStatsByGameId, conference)
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

    fun aggregateGameStatsToSeasonStats(
        gameStatsList: List<GameStats>,
        team: String,
        seasonNumber: Int,
        gameStatsByGameId: Map<Int, List<GameStats>>,
        conference: String?,
    ): SeasonStats {
        val firstGameStats = gameStatsList.first()

        var wins = 0
        var losses = 0

        for (teamGameStats in gameStatsList) {
            val gameId = teamGameStats.gameId
            val teamScore = teamGameStats.score

            val opponentGameStats =
                gameStatsByGameId[gameId]
                    ?.firstOrNull { it.team != team }

            if (opponentGameStats != null) {
                val opponentScore = opponentGameStats.score
                if (teamScore > opponentScore) {
                    wins++
                } else if (teamScore < opponentScore) {
                    losses++
                }
            }
        }

        return SeasonStats(
            team = team,
            seasonNumber = seasonNumber,
            wins = wins,
            losses = losses,
            subdivision = firstGameStats.subdivision,
            conference = conference,
            offensivePlaybook = firstGameStats.offensivePlaybook,
            defensivePlaybook = firstGameStats.defensivePlaybook,
            passAttempts = gameStatsList.sumOf { it.passAttempts },
            passCompletions = gameStatsList.sumOf { it.passCompletions },
            passCompletionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.passCompletions },
                    gameStatsList.sumOf { it.passAttempts },
                ),
            passYards = gameStatsList.sumOf { it.passYards },
            longestPass = gameStatsList.maxOfOrNull { it.longestPass } ?: 0,
            passTouchdowns = gameStatsList.sumOf { it.passTouchdowns },
            passSuccesses = gameStatsList.sumOf { it.passSuccesses },
            passSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.passSuccesses },
                    gameStatsList.sumOf { it.passAttempts },
                ),
            rushAttempts = gameStatsList.sumOf { it.rushAttempts },
            rushSuccesses = gameStatsList.sumOf { it.rushSuccesses },
            rushSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.rushSuccesses },
                    gameStatsList.sumOf { it.rushAttempts },
                ),
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
            fieldGoalPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.fieldGoalMade },
                    gameStatsList.sumOf { it.fieldGoalAttempts },
                ),
            longestFieldGoal = gameStatsList.maxOfOrNull { it.longestFieldGoal } ?: 0,
            blockedOpponentFieldGoals = gameStatsList.sumOf { it.blockedOpponentFieldGoals },
            fieldGoalTouchdown = gameStatsList.sumOf { it.fieldGoalTouchdown },
            puntsAttempted = gameStatsList.sumOf { it.puntsAttempted },
            longestPunt = gameStatsList.maxOfOrNull { it.longestPunt } ?: 0,
            averagePuntLength = calculateAverage(gameStatsList.mapNotNull { it.averagePuntLength }),
            blockedOpponentPunt = gameStatsList.sumOf { it.blockedOpponentPunt },
            puntReturnTd = gameStatsList.sumOf { it.puntReturnTd },
            puntReturnTdPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.puntReturnTd },
                    gameStatsList.sumOf { it.puntsAttempted },
                ),
            numberOfKickoffs = gameStatsList.sumOf { it.numberOfKickoffs },
            onsideAttempts = gameStatsList.sumOf { it.onsideAttempts },
            onsideSuccess = gameStatsList.sumOf { it.onsideSuccess },
            onsideSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.onsideSuccess },
                    gameStatsList.sumOf { it.onsideAttempts },
                ),
            normalKickoffAttempts = gameStatsList.sumOf { it.normalKickoffAttempts },
            touchbacks = gameStatsList.sumOf { it.touchbacks },
            touchbackPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.touchbacks },
                    gameStatsList.sumOf { it.normalKickoffAttempts },
                ),
            kickReturnTd = gameStatsList.sumOf { it.kickReturnTd },
            kickReturnTdPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.kickReturnTd },
                    gameStatsList.sumOf { it.numberOfKickoffs },
                ),
            numberOfDrives = gameStatsList.sumOf { it.numberOfDrives },
            timeOfPossession = gameStatsList.sumOf { it.timeOfPossession },
            touchdowns = gameStatsList.sumOf { it.touchdowns },
            thirdDownConversionSuccess = gameStatsList.sumOf { it.thirdDownConversionSuccess },
            thirdDownConversionAttempts = gameStatsList.sumOf { it.thirdDownConversionAttempts },
            thirdDownConversionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.thirdDownConversionSuccess },
                    gameStatsList.sumOf { it.thirdDownConversionAttempts },
                ),
            fourthDownConversionSuccess = gameStatsList.sumOf { it.fourthDownConversionSuccess },
            fourthDownConversionAttempts = gameStatsList.sumOf { it.fourthDownConversionAttempts },
            fourthDownConversionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.fourthDownConversionSuccess },
                    gameStatsList.sumOf { it.fourthDownConversionAttempts },
                ),
            largestLead = gameStatsList.maxOfOrNull { it.largestLead } ?: 0,
            largestDeficit = gameStatsList.maxOfOrNull { it.largestDeficit } ?: 0,
            redZoneAttempts = gameStatsList.sumOf { it.redZoneAttempts },
            redZoneSuccesses = gameStatsList.sumOf { it.redZoneSuccesses },
            redZoneSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.redZoneSuccesses },
                    gameStatsList.sumOf { it.redZoneAttempts },
                ),
            redZonePercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.redZoneAttempts },
                    gameStatsList.sumOf { it.numberOfDrives },
                ),
            safetiesForced = gameStatsList.sumOf { it.safetiesForced },
            safetiesCommitted = gameStatsList.sumOf { it.safetiesCommitted },
            averageOffensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveDiff }),
            averageDefensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveDiff }),
            averageOffensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveSpecialTeamsDiff }),
            averageDefensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveSpecialTeamsDiff }),
            averageDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDiff }),
            averageResponseSpeed = calculateAverage(gameStatsList.mapNotNull { it.averageResponseSpeed }),
            opponentPassAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passAttempts ?: 0
                },
            opponentPassCompletions =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passCompletions ?: 0
                },
            opponentPassCompletionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passCompletions ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passAttempts ?: 0 },
                ),
            opponentPassYards =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passYards ?: 0
                },
            opponentLongestPass =
                gameStatsList.mapNotNull {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.longestPass
                }.maxOrNull() ?: 0,
            opponentPassTouchdowns =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passTouchdowns ?: 0
                },
            opponentPassSuccesses =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passSuccesses ?: 0
                },
            opponentPassSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passSuccesses ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passAttempts ?: 0 },
                ),
            opponentRushAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushAttempts ?: 0
                },
            opponentRushSuccesses =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushSuccesses ?: 0
                },
            opponentRushSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushSuccesses ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushAttempts ?: 0 },
                ),
            opponentRushYards =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushYards ?: 0
                },
            opponentLongestRun =
                gameStatsList.mapNotNull {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.longestRun
                }.maxOrNull() ?: 0,
            opponentRushTouchdowns =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushTouchdowns ?: 0
                },
            opponentTotalYards =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.totalYards ?: 0
                },
            opponentAverageYardsPerPlay =
                calculateAverage(
                    gameStatsList.mapNotNull {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.averageYardsPerPlay
                    },
                ),
            opponentFirstDowns =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.firstDowns ?: 0
                },
            opponentFieldGoalMade =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalMade ?: 0
                },
            opponentFieldGoalAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalAttempts ?: 0
                },
            opponentFieldGoalPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalMade ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalAttempts ?: 0 },
                ),
            opponentLongestFieldGoal =
                gameStatsList.mapNotNull {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.longestFieldGoal
                }.maxOrNull() ?: 0,
            opponentFieldGoalTouchdown =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalTouchdown ?: 0
                },
            opponentPuntsAttempted =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.puntsAttempted ?: 0
                },
            opponentLongestPunt =
                gameStatsList.mapNotNull {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.longestPunt
                }.maxOrNull() ?: 0,
            opponentAveragePuntLength =
                calculateAverage(
                    gameStatsList.mapNotNull {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.averagePuntLength
                    },
                ),
            opponentPuntReturnTd =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.puntReturnTd ?: 0
                },
            opponentPuntReturnTdPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.puntReturnTd ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.puntsAttempted ?: 0 },
                ),
            opponentNumberOfKickoffs =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.numberOfKickoffs ?: 0
                },
            opponentOnsideAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.onsideAttempts ?: 0
                },
            opponentOnsideSuccess =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.onsideSuccess ?: 0
                },
            opponentOnsideSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.onsideSuccess ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.onsideAttempts ?: 0 },
                ),
            opponentNormalKickoffAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.normalKickoffAttempts ?: 0
                },
            opponentTouchbacks =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.touchbacks ?: 0
                },
            opponentTouchbackPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.touchbacks ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.normalKickoffAttempts ?: 0 },
                ),
            opponentKickReturnTd =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.kickReturnTd ?: 0
                },
            opponentKickReturnTdPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.kickReturnTd ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.numberOfKickoffs ?: 0 },
                ),
            opponentNumberOfDrives =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.numberOfDrives ?: 0
                },
            opponentTimeOfPossession =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.timeOfPossession ?: 0
                },
            opponentTouchdowns =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.touchdowns ?: 0
                },
            opponentThirdDownConversionSuccess =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.thirdDownConversionSuccess ?: 0
                },
            opponentThirdDownConversionAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.thirdDownConversionAttempts ?: 0
                },
            opponentThirdDownConversionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.thirdDownConversionSuccess ?: 0
                    },
                    gameStatsList.sumOf {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.thirdDownConversionAttempts ?: 0
                    },
                ),
            opponentFourthDownConversionSuccess =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fourthDownConversionSuccess ?: 0
                },
            opponentFourthDownConversionAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fourthDownConversionAttempts ?: 0
                },
            opponentFourthDownConversionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fourthDownConversionSuccess ?: 0
                    },
                    gameStatsList.sumOf {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fourthDownConversionAttempts ?: 0
                    },
                ),
            opponentRedZoneAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneAttempts ?: 0
                },
            opponentRedZoneSuccesses =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneSuccesses ?: 0
                },
            opponentRedZoneSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneSuccesses ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneAttempts ?: 0 },
                ),
            opponentRedZonePercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneAttempts ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.numberOfDrives ?: 0 },
                ),
            lastModifiedTs =
                ZonedDateTime.now(ZoneId.of("America/New_York"))
                    .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
        )
    }

    private fun calculateAverage(values: List<Double>): Double? {
        return if (values.isNotEmpty()) {
            values.average()
        } else {
            null
        }
    }

    private fun getOpponentGameStatsByGameId(
        gameId: Int,
        gameStatsByGameId: Map<Int, List<GameStats>>,
        team: String,
    ): GameStats? {
        return gameStatsByGameId[gameId]
            ?.firstOrNull { it.team != team }
    }

    fun getLeaderboard(
        statName: String,
        season: Int? = null,
        subdivision: String? = null,
        conference: String? = null,
        limit: Int = 10,
        ascending: Boolean = false,
    ): List<SeasonStats> {
        try {
            val allStatsPage =
                getFilteredSeasonStats(
                    team = null,
                    season = null,
                    pageable = Pageable.unpaged(),
                )

            val allStats = allStatsPage.content

            val filteredStats =
                allStats.filter { stats ->
                    season?.let { stats.seasonNumber == it } ?: true &&
                        subdivision?.let { stats.subdivision?.name.equals(it, ignoreCase = true) } ?: true &&
                        conference?.let { stats.conference.equals(it, ignoreCase = true) } ?: true
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
                val normalizedName = normalizedStatName
                if ("averageoffensivediff" != normalizedName && "pointdifferential" != normalizedName) {
                    if (ascending) {
                        sortedStats.reversed()
                    } else {
                        sortedStats
                    }
                } else {
                    sortedStats
                }
            }.take(limit)
        } catch (e: Exception) {
            Logger.error("Error in getLeaderboard: ${e.message}", e)
            throw e
        }
    }

    private fun calculatePercentage(
        successes: Int,
        attempts: Int,
    ): Double? {
        if (attempts == 0) return null
        return (successes.toDouble() / attempts.toDouble()) * 100.0
    }
}
