package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.PlaybookStats
import com.fcfb.arceus.model.PostseasonPlaybookStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PlaybookStatsRepository
import com.fcfb.arceus.service.specification.PlaybookStatsSpecificationService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.POSTSEASON_START_WEEK
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlaybookStatsService(
    private val playbookStatsRepository: PlaybookStatsRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val playbookStatsSpecificationService: PlaybookStatsSpecificationService,
    private val postseasonPlaybookStatsService: PostseasonPlaybookStatsService,
) {
    fun getFilteredPlaybookStats(
        offensivePlaybook: OffensivePlaybook?,
        defensivePlaybook: DefensivePlaybook?,
        season: Int?,
        pageable: Pageable,
    ): Page<PlaybookStats> {
        val spec = playbookStatsSpecificationService.createSpecification(offensivePlaybook, defensivePlaybook, season)
        val sortOrders = playbookStatsSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return playbookStatsRepository.findAll(spec, sortedPageable)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun generateAllPlaybookStats() {
        Logger.info("Starting generation of all playbook stats")

        val allGameStats =
            gameStatsRepository.findAllByOrderBySeasonDescGameIdAsc()
                .filter { (it.week ?: 0) < POSTSEASON_START_WEEK }
        Logger.info("Found ${allGameStats.size} total game stats records")

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
                    "Generating playbook stats for $offensivePlaybook/$defensivePlaybook " +
                        "in season $seasonNumber with ${gameStatsList.size} games (${index + 1}/$total)",
                )
                generateByPlaybooksAndSeason(
                    offensivePlaybook,
                    defensivePlaybook,
                    seasonNumber,
                )
            }
        }

        Logger.info("Completed generation of all playbook stats")
    }

    private fun generateByPlaybooksAndSeason(
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of playbook stats for $offensivePlaybook/$defensivePlaybook in season $seasonNumber")

        val gameStatsList =
            gameStatsRepository.findBySeasonOrderByGameIdAsc(seasonNumber)
                .filter { gameStats ->
                    gameStats.offensivePlaybook == offensivePlaybook && gameStats.defensivePlaybook == defensivePlaybook
                }
                .filter { (it.week ?: 0) < POSTSEASON_START_WEEK }

        if (gameStatsList.isEmpty()) {
            Logger.warn("No game stats found for $offensivePlaybook/$defensivePlaybook in season $seasonNumber")
            return
        }

        val playbookStats =
            PlaybookStatsCalculator.aggregateGameStatsToPlaybookStats(
                gameStatsList,
                offensivePlaybook,
                defensivePlaybook,
                seasonNumber,
            )
        playbookStatsRepository.findByOffensivePlaybookAndDefensivePlaybookAndSeasonNumber(
            offensivePlaybook,
            defensivePlaybook,
            seasonNumber,
        )?.let { playbookStats.id = it.id }

        playbookStatsRepository.save(playbookStats)
        Logger.info("Completed generating playbook stats for $offensivePlaybook/$defensivePlaybook in season $seasonNumber")
    }

    fun getFilteredPostseasonPlaybookStats(
        offensivePlaybook: OffensivePlaybook?,
        defensivePlaybook: DefensivePlaybook?,
        season: Int?,
        pageable: Pageable,
    ): Page<PostseasonPlaybookStats> =
        postseasonPlaybookStatsService.getFilteredPostseasonPlaybookStats(
            offensivePlaybook,
            defensivePlaybook,
            season,
            pageable,
        )

    fun generateAllPostseasonPlaybookStats() = postseasonPlaybookStatsService.generateAllPostseasonPlaybookStats()
}
