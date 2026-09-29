package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.response.RankingResponse
import com.fcfb.arceus.enums.ranking.PollType
import com.fcfb.arceus.model.Ranking
import com.fcfb.arceus.repositories.RankingRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.util.InvalidRankingsException
import org.springframework.stereotype.Service

@Service
class RankingService(
    private val rankingRepository: RankingRepository,
    private val teamRepository: TeamRepository,
) {
    fun getRankings(
        season: Int,
        week: Int,
        pollType: String,
    ): List<RankingResponse> {
        val parsedPollType = parsePollType(pollType)
        return rankingRepository.findBySeasonWeekAndPollType(season, week, parsedPollType.name).map { ranking ->
            RankingResponse(
                season = ranking.season,
                week = ranking.week,
                pollType = parsedPollType.name,
                rank = ranking.rank,
                teamId = ranking.teamId,
                teamName = teamRepository.findById(ranking.teamId).orElse(null)?.name,
                wins = ranking.wins,
                losses = ranking.losses,
            )
        }
    }

    fun getLatestRankings(pollType: String): List<RankingResponse> {
        val latest = rankingRepository.findLatest(parsePollType(pollType).name) ?: return emptyList()
        return getRankings(latest.season, latest.week, pollType)
    }

    fun getAvailableWeeks(
        season: Int,
        pollType: String,
    ): List<Int> = rankingRepository.findWeeks(season, parsePollType(pollType).name)

    fun areRankingsUploaded(
        season: Int,
        week: Int,
        pollType: PollType,
    ): Boolean = rankingRepository.existsForWeek(season, week, pollType.name) > 0

    fun uploadRankings(
        season: Int,
        week: Int,
        pollType: String,
        teamNames: List<String>,
    ): List<RankingResponse> {
        val parsedPollType = parsePollType(pollType)
        val cleanedNames = teamNames.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanedNames.isEmpty()) {
            throw InvalidRankingsException("No teams were provided.")
        }

        val duplicates = cleanedNames.groupingBy { it.lowercase() }.eachCount().filter { it.value > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw InvalidRankingsException("Duplicate teams in upload: ${duplicates.joinToString(", ")}")
        }

        val teams =
            cleanedNames.map { name ->
                teamRepository.getTeamByName(name)
                    ?: throw InvalidRankingsException("Unknown team: $name")
            }

        rankingRepository.deleteBySeasonWeekAndPollType(season, week, parsedPollType.name)
        teams.forEachIndexed { index, team ->
            rankingRepository.save(Ranking(season, week, parsedPollType, index + 1, team.id, team.currentWins, team.currentLosses))
        }

        return getRankings(season, week, pollType)
    }

    fun getTeamRanks(
        season: Int,
        week: Int,
        homeTeamId: Int,
        awayTeamId: Int,
    ): Pair<Int?, Int?> {
        val pollType =
            if (areRankingsUploaded(season, week, PollType.PLAYOFF_COMMITTEE)) PollType.PLAYOFF_COMMITTEE else PollType.COACHES_POLL
        val ranksByTeamId =
            rankingRepository.findBySeasonWeekAndPollType(season, week, pollType.name).associate { it.teamId to it.rank }
        return ranksByTeamId[homeTeamId] to ranksByTeamId[awayTeamId]
    }

    private fun parsePollType(pollType: String): PollType =
        PollType.fromString(pollType) ?: throw InvalidRankingsException("Unknown poll type: $pollType")
}
