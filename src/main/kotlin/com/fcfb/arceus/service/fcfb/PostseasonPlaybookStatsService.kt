package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.PlaybookStats
import com.fcfb.arceus.model.PostseasonPlaybookStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PostseasonPlaybookStatsRepository
import com.fcfb.arceus.service.specification.PostseasonPlaybookStatsSpecificationService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.POSTSEASON_START_WEEK
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostseasonPlaybookStatsService(
    private val postseasonPlaybookStatsRepository: PostseasonPlaybookStatsRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val playbookStatsService: PlaybookStatsService,
    private val postseasonPlaybookStatsSpecificationService: PostseasonPlaybookStatsSpecificationService,
) {
    fun getFilteredPostseasonPlaybookStats(
        offensivePlaybook: OffensivePlaybook?,
        defensivePlaybook: DefensivePlaybook?,
        season: Int?,
        pageable: Pageable,
    ): Page<PostseasonPlaybookStats> {
        val spec = postseasonPlaybookStatsSpecificationService.createSpecification(offensivePlaybook, defensivePlaybook, season)
        val sortOrders = postseasonPlaybookStatsSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return postseasonPlaybookStatsRepository.findAll(spec, sortedPageable)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun generateAllPostseasonPlaybookStats() {
        Logger.info("Starting generation of all postseason playbook stats")

        val allGameStats =
            gameStatsRepository.findAllByOrderBySeasonDescGameIdAsc()
                .filter { (it.week ?: 0) >= POSTSEASON_START_WEEK }
        Logger.info("Found ${allGameStats.size} total postseason game stats records")

        val groupedStats =
            allGameStats.groupBy {
                Triple(it.offensivePlaybook, it.defensivePlaybook, it.season)
            }.filterKeys { it.first != null && it.second != null }

        Logger.info("Found ${groupedStats.size} valid offensive/defensive playbook/season combinations")

        val total = groupedStats.size
        groupedStats.entries.forEachIndexed { index, (playbookSeason, gameStatsList) ->
            val offensivePlaybook = playbookSeason.first!!
            val defensivePlaybook = playbookSeason.second!!
            val seasonNumber = playbookSeason.third

            if (seasonNumber != null) {
                Logger.info(
                    "Generating postseason playbook stats for $offensivePlaybook/$defensivePlaybook " +
                        "in season $seasonNumber with ${gameStatsList.size} games (${index + 1}/$total)",
                )
                generatePostseasonByPlaybooksAndSeason(
                    offensivePlaybook,
                    defensivePlaybook,
                    seasonNumber,
                )
            }
        }

        Logger.info("Completed generation of all postseason playbook stats")
    }

    private fun generatePostseasonByPlaybooksAndSeason(
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of postseason playbook stats for $offensivePlaybook/$defensivePlaybook in season $seasonNumber")

        val gameStatsList =
            gameStatsRepository.findBySeasonOrderByGameIdAsc(seasonNumber)
                .filter { gameStats ->
                    gameStats.offensivePlaybook == offensivePlaybook && gameStats.defensivePlaybook == defensivePlaybook
                }
                .filter { (it.week ?: 0) >= POSTSEASON_START_WEEK }

        if (gameStatsList.isEmpty()) {
            Logger.warn("No postseason game stats found for $offensivePlaybook/$defensivePlaybook in season $seasonNumber")
            return
        }

        postseasonPlaybookStatsRepository.findByOffensivePlaybookAndDefensivePlaybookAndSeasonNumber(
            offensivePlaybook,
            defensivePlaybook,
            seasonNumber,
        )?.let {
            postseasonPlaybookStatsRepository.delete(it)
        }

        val playbookStats =
            playbookStatsService.aggregateGameStatsToPlaybookStats(gameStatsList, offensivePlaybook, defensivePlaybook, seasonNumber)

        postseasonPlaybookStatsRepository.save(playbookStats.toPostseason())
        Logger.info("Completed generating postseason playbook stats for $offensivePlaybook/$defensivePlaybook in season $seasonNumber")
    }
}

private fun PlaybookStats.toPostseason(): PostseasonPlaybookStats {
    return PostseasonPlaybookStats(
        offensivePlaybook = offensivePlaybook,
        defensivePlaybook = defensivePlaybook,
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
