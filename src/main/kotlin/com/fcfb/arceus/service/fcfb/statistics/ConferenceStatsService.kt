package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.ConferenceStats
import com.fcfb.arceus.model.PostseasonConferenceStats
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
import org.springframework.transaction.annotation.Transactional

@Service
class ConferenceStatsService(
    private val conferenceStatsRepository: ConferenceStatsRepository,
    private val seasonStatsRepository: SeasonStatsRepository,
    private val conferenceStatsSpecificationService: ConferenceStatsSpecificationService,
    private val postseasonConferenceStatsService: PostseasonConferenceStatsService,
) {
    fun getFilteredConferenceStats(
        conference: String?,
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

    @Transactional(rollbackFor = [Exception::class])
    fun generateAllConferenceStats() {
        Logger.info("Starting generation of all conference stats")

        val allSeasonStats = seasonStatsRepository.findAllByOrderBySeasonNumberDescTeamAsc()
        Logger.info("Found ${allSeasonStats.size} total season stats records")

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
                "Generating conference stats for $subdivision/$conference in season $seasonNumber " +
                    "with ${seasonStatsList.size} teams (${index + 1}/$total)",
            )
            generateConferenceStatsForSubdivisionAndConferenceAndSeason(subdivision, conference, seasonNumber)
        }

        Logger.info("Completed generation of all conference stats")
    }

    private fun generateConferenceStatsForSubdivisionAndConferenceAndSeason(
        subdivision: Subdivision,
        conference: String,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of conference stats for $subdivision/$conference in season $seasonNumber")

        val seasonStatsList =
            seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
                .filter { seasonStats ->
                    seasonStats.subdivision == subdivision && seasonStats.conference == conference
                }

        if (seasonStatsList.isEmpty()) {
            Logger.warn("No season stats found for $subdivision/$conference in season $seasonNumber")
            return
        }

        val conferenceStats =
            ConferenceStatsCalculator.aggregateSeasonStatsToConferenceStats(
                seasonStatsList,
                subdivision,
                conference,
                seasonNumber,
            )
        conferenceStatsRepository.findBySubdivisionAndConferenceAndSeasonNumber(subdivision, conference, seasonNumber)?.let {
            conferenceStats.id = it.id
        }

        conferenceStatsRepository.save(conferenceStats)
        Logger.info("Completed generating conference stats for $subdivision/$conference in season $seasonNumber")
    }

    fun updateConferenceStatsForSeasonStats(seasonStats: SeasonStats) {
        val subdivision = seasonStats.subdivision ?: return
        val conference = seasonStats.conference ?: return
        val season = seasonStats.seasonNumber

        generateConferenceStatsForSubdivisionAndConferenceAndSeason(subdivision, conference, season)
    }

    fun getFilteredPostseasonConferenceStats(
        conference: String?,
        season: Int?,
        subdivision: Subdivision?,
        pageable: Pageable,
    ): Page<PostseasonConferenceStats> =
        postseasonConferenceStatsService.getFilteredPostseasonConferenceStats(
            conference,
            season,
            subdivision,
            pageable,
        )

    fun generateAllPostseasonConferenceStats() = postseasonConferenceStatsService.generateAllPostseasonConferenceStats()
}
