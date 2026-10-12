package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.LeagueStats
import com.fcfb.arceus.model.PostseasonLeagueStats
import com.fcfb.arceus.repositories.LeagueStatsRepository
import com.fcfb.arceus.repositories.SeasonStatsRepository
import com.fcfb.arceus.service.specification.LeagueStatsSpecificationService
import com.fcfb.arceus.util.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeagueStatsService(
    private val leagueStatsRepository: LeagueStatsRepository,
    private val seasonStatsRepository: SeasonStatsRepository,
    private val leagueStatsSpecificationService: LeagueStatsSpecificationService,
    private val postseasonLeagueStatsService: PostseasonLeagueStatsService,
) {
    fun getFilteredLeagueStats(
        subdivision: Subdivision?,
        season: Int?,
        pageable: Pageable,
    ): Page<LeagueStats> {
        val spec = leagueStatsSpecificationService.createSpecification(subdivision, season)
        val sortOrders = leagueStatsSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return leagueStatsRepository.findAll(spec, sortedPageable)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun generateAllLeagueStats() {
        Logger.info("Starting generation of all league stats")

        val allSeasonStats = seasonStatsRepository.findAllByOrderBySeasonNumberDescTeamAsc()

        val groupedStats =
            allSeasonStats.groupBy {
                Pair(it.subdivision, it.seasonNumber)
            }.filterKeys { it.first != null }

        val total = groupedStats.size
        groupedStats.keys.forEachIndexed { index, subdivisionSeason ->
            val subdivision = subdivisionSeason.first!!
            val seasonNumber = subdivisionSeason.second

            Logger.info("Generating league stats for $subdivision in season $seasonNumber (${index + 1}/$total)")
            generateLeagueStatsForSubdivisionAndSeason(subdivision, seasonNumber)
        }

        Logger.info("Completed generation of all league stats")
    }

    private fun generateLeagueStatsForSubdivisionAndSeason(
        subdivision: Subdivision,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of league stats for $subdivision in season $seasonNumber")

        val seasonStatsList =
            seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
                .filter { seasonStats -> seasonStats.subdivision == subdivision }

        if (seasonStatsList.isEmpty()) {
            Logger.warn("No season stats found for $subdivision in season $seasonNumber")
            return
        }

        val leagueStats = LeagueStatsCalculator.aggregateSeasonStatsToLeagueStats(seasonStatsList, subdivision, seasonNumber)
        leagueStatsRepository.findBySubdivisionAndSeasonNumber(subdivision, seasonNumber)?.let {
            leagueStats.id = it.id
        }

        leagueStatsRepository.save(leagueStats)
        Logger.info("Completed generating league stats for $subdivision in season $seasonNumber")
    }

    fun getFilteredPostseasonLeagueStats(
        subdivision: Subdivision?,
        season: Int?,
        pageable: Pageable,
    ): Page<PostseasonLeagueStats> = postseasonLeagueStatsService.getFilteredPostseasonLeagueStats(subdivision, season, pageable)

    fun generateAllPostseasonLeagueStats() = postseasonLeagueStatsService.generateAllPostseasonLeagueStats()
}
