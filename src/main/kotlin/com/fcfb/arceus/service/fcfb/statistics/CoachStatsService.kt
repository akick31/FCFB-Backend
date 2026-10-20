package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.domain.statistics.SeasonStatsCalculator
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.account.User
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.statistics.CoachSeasonStats
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.model.statistics.SeasonStats
import com.fcfb.arceus.repositories.account.UserRepository
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.statistics.CoachSeasonStatsRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.coach.CoachStintService
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.statistics.StatsAverageCalculator
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CoachStatsService(
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val coachStintService: CoachStintService,
    private val coachSeasonStatsRepository: CoachSeasonStatsRepository,
) {
    private val seasonStatsCalculator = SeasonStatsCalculator(StatsAverageCalculator())

    fun getCoachStats(coach: String): List<CoachSeasonStats> {
        val user = userRepository.findByUsername(coach) ?: return emptyList()
        return coachSeasonStatsRepository.findByUserIdOrderBySeasonNumberDescTeamAsc(user.id)
    }

    fun regenerateForCoach(coach: String) {
        AuthContext().requireAdminOrCommissioner()
        val user = userRepository.findByUsername(coach) ?: return
        regenerateForCoach(user)
    }

    fun regenerateForCoach(user: User) {
        val freshStats = computeSeasonStats(user).map { it.toCoachSeasonStats(user.id) }
        freshStats.forEach { stats ->
            coachSeasonStatsRepository.findByUserIdAndTeamAndSeasonNumber(user.id, stats.team, stats.seasonNumber)
                ?.let { stats.id = it.id }
            coachSeasonStatsRepository.save(stats)
        }

        val freshKeys = freshStats.map { "${it.team}_${it.seasonNumber}" }.toSet()
        val staleStats =
            coachSeasonStatsRepository.findByUserId(user.id)
                .filterNot { "${it.team}_${it.seasonNumber}" in freshKeys }
        if (staleStats.isNotEmpty()) {
            coachSeasonStatsRepository.deleteAll(staleStats)
        }
    }

    fun generateAllCoachSeasonStats() {
        AuthContext().requireAdminOrCommissioner()
        Logger().info("Starting generation of all coach season stats")
        val users = userRepository.findAll().filterNotNull()
        users.forEachIndexed { index, user ->
            Logger().info("Generating coach season stats for ${user.username} (${index + 1}/${users.size})")
            regenerateForCoach(user)
        }
        Logger().info("Completed generation of all coach season stats")
    }

    private fun computeSeasonStats(user: User): List<SeasonStats> {
        val stints = coachStintService.getStintsForCoach(user.username, user)
        val currentTeam = user.team
        val teams = (stints.map { it.team } + listOfNotNull(currentTeam)).toSet()
        if (teams.isEmpty()) {
            return emptyList()
        }

        val gamesByTeam = teams.associateWith { teamGames(it) }
        val gameStatsByTeam = teams.associateWith { gameStatsRepository.findByTeam(it) }
        coachStintService.backdateStartsToGames(stints, earliestGameByTeam(gamesByTeam))

        val coachGameStats = mutableListOf<GameStats>()
        val seen = mutableSetOf<String>()
        for (team in teams) {
            val statsByGameId = (gameStatsByTeam[team] ?: emptyList()).associateBy { it.gameId }
            for (game in gamesByTeam[team] ?: emptyList()) {
                val timestamp = coachStintService.parseGameTimestamp(game.timestamp) ?: continue
                if (!coachStintService.countsForCoach(stints, team, timestamp)) continue
                val stats = statsByGameId[game.gameId] ?: continue
                if (seen.add("${game.gameId}_$team")) {
                    coachGameStats.add(stats)
                }
            }
        }
        if (coachGameStats.isEmpty()) {
            return emptyList()
        }

        val gameStatsByGameId = gameStatsRepository.findByGameIdIn(coachGameStats.map { it.gameId }.toSet()).groupBy { it.gameId }
        val conferenceByTeam = teamRepository.findAll().mapNotNull { team -> team.name?.let { it to team.conference } }.toMap()

        return coachGameStats
            .filter { it.season != null && it.team != null }
            .groupBy { "${it.team}_${it.season}" }
            .map { (_, rows) ->
                val first = rows.first()
                seasonStatsCalculator.aggregateGameStatsToSeasonStats(
                    rows,
                    first.team!!,
                    first.season!!,
                    gameStatsByGameId,
                    conferenceByTeam[first.team],
                )
            }
    }

    private fun teamGames(team: String): List<Game> =
        (gameRepository.findByHomeTeam(team) + gameRepository.findByAwayTeam(team))
            .filter { it.gameType != GameType.SCRIMMAGE }

    private fun earliestGameByTeam(gamesByTeam: Map<String, List<Game>>): Map<String, LocalDateTime> =
        gamesByTeam.mapNotNull { (team, games) ->
            games.mapNotNull { coachStintService.parseGameTimestamp(it.timestamp) }.minOrNull()?.let { team to it }
        }.toMap()
}

private fun SeasonStats.toCoachSeasonStats(userId: Long): CoachSeasonStats {
    return CoachSeasonStats(
        userId = userId,
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
        offensivePlayYards = offensivePlayYards,
        offensivePlayCount = offensivePlayCount,
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
        puntYards = puntYards,
        puntCount = puntCount,
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
        opponentOffensivePlayYards = opponentOffensivePlayYards,
        opponentOffensivePlayCount = opponentOffensivePlayCount,
        opponentAverageYardsPerPlay = opponentAverageYardsPerPlay,
        opponentFirstDowns = opponentFirstDowns,
        opponentFieldGoalMade = opponentFieldGoalMade,
        opponentFieldGoalAttempts = opponentFieldGoalAttempts,
        opponentFieldGoalPercentage = opponentFieldGoalPercentage,
        opponentLongestFieldGoal = opponentLongestFieldGoal,
        opponentFieldGoalTouchdown = opponentFieldGoalTouchdown,
        opponentPuntsAttempted = opponentPuntsAttempted,
        opponentLongestPunt = opponentLongestPunt,
        opponentPuntYards = opponentPuntYards,
        opponentPuntCount = opponentPuntCount,
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
