package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.response.TeamResumeMetricResponse
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamResumeMetric
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.repositories.TeamResumeMetricBatchRepository
import com.fcfb.arceus.repositories.TeamResumeMetricRepository
import org.springframework.stereotype.Service

@Service
class TeamResumeMetricService(
    private val teamResumeMetricRepository: TeamResumeMetricRepository,
    private val teamResumeMetricBatchRepository: TeamResumeMetricBatchRepository,
    private val teamRepository: TeamRepository,
) {
    private data class MutableResume(
        var conferenceWins: Int = 0,
        var conferenceLosses: Int = 0,
        var q1Wins: Int = 0,
        var q1Losses: Int = 0,
        var thWins: Int = 0,
        var thLosses: Int = 0,
        var q4Wins: Int = 0,
        var q4Losses: Int = 0,
        var t25Wins: Int = 0,
        var t25Losses: Int = 0,
        var t50Wins: Int = 0,
        var t50Losses: Int = 0,
        var t100Wins: Int = 0,
        var t100Losses: Int = 0,
        var opponentRankSum: Int = 0,
        var opponentRankCount: Int = 0,
        var opponentCompositeSum: Double = 0.0,
        var opponentCompositeCount: Int = 0,
    )

    /**
     * Opponent-quality buckets (quartile/T25/T50/T100) are evaluated against each opponent's Composite rank
     * for THIS week, not the week the game was played — avoids the circularity of a team's rank depending on
     * a game that's also used to classify that same game, and matches how NCAA NET quadrants work.
     */
    internal fun computeAndPersist(
        season: Int,
        week: Int,
        games: List<Game>,
        aggregates: Map<Int, RankingMetricService.TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
        compositeRatings: Map<Int, Double>,
    ) {
        if (aggregates.isEmpty()) return

        val compositeRankByTeamId =
            compositeRatings.entries
                .sortedByDescending { it.value }
                .mapIndexed { index, entry -> entry.key to (index + 1) }
                .toMap()
        val n = compositeRankByTeamId.size
        val quartileSize = Math.ceil(n / 4.0).toInt()

        val byTeamId = mutableMapOf<Int, MutableResume>()

        fun record(
            teamId: Int,
            opponentTeamId: Int,
            won: Boolean,
            isConferenceGame: Boolean,
        ) {
            val resume = byTeamId.getOrPut(teamId) { MutableResume() }
            if (isConferenceGame) {
                if (won) resume.conferenceWins++ else resume.conferenceLosses++
            }

            val opponentRank = compositeRankByTeamId[opponentTeamId] ?: return
            val opponentComposite = compositeRatings.getValue(opponentTeamId)
            resume.opponentRankSum += opponentRank
            resume.opponentRankCount += 1
            resume.opponentCompositeSum += opponentComposite
            resume.opponentCompositeCount += 1

            when {
                opponentRank <= quartileSize -> if (won) resume.q1Wins++ else resume.q1Losses++
                opponentRank > 2 * quartileSize && opponentRank <= 3 * quartileSize -> if (won) resume.thWins++ else resume.thLosses++
                opponentRank > 3 * quartileSize -> if (won) resume.q4Wins++ else resume.q4Losses++
            }
            if (opponentRank <= 25) {
                if (won) resume.t25Wins++ else resume.t25Losses++
            }
            if (opponentRank <= 50) {
                if (won) resume.t50Wins++ else resume.t50Losses++
            }
            if (opponentRank <= 100) {
                if (won) resume.t100Wins++ else resume.t100Losses++
            }
        }

        games.forEach { game ->
            val homeTeam = teamsByName[game.homeTeam] ?: return@forEach
            val awayTeam = teamsByName[game.awayTeam] ?: return@forEach
            val isConferenceGame = game.gameType == GameType.CONFERENCE_GAME
            record(homeTeam.id, awayTeam.id, game.homeScore > game.awayScore, isConferenceGame)
            record(awayTeam.id, homeTeam.id, game.awayScore > game.homeScore, isConferenceGame)
        }

        val rows =
            aggregates.map { (teamId, aggregate) ->
                val resume = byTeamId[teamId] ?: MutableResume()
                TeamResumeMetric(
                    season = season,
                    week = week,
                    teamId = teamId,
                    overallWins = aggregate.wins,
                    overallLosses = aggregate.losses,
                    conferenceWins = resume.conferenceWins,
                    conferenceLosses = resume.conferenceLosses,
                    q1Wins = resume.q1Wins,
                    q1Losses = resume.q1Losses,
                    thWins = resume.thWins,
                    thLosses = resume.thLosses,
                    q4Wins = resume.q4Wins,
                    q4Losses = resume.q4Losses,
                    t25Wins = resume.t25Wins,
                    t25Losses = resume.t25Losses,
                    t50Wins = resume.t50Wins,
                    t50Losses = resume.t50Losses,
                    t100Wins = resume.t100Wins,
                    t100Losses = resume.t100Losses,
                    avgOpponentCompositeRank =
                        if (resume.opponentRankCount > 0) resume.opponentRankSum.toDouble() / resume.opponentRankCount else null,
                    compositeSos =
                        if (resume.opponentCompositeCount > 0) resume.opponentCompositeSum / resume.opponentCompositeCount else null,
                )
            }

        teamResumeMetricRepository.deleteBySeasonAndWeek(season, week)
        teamResumeMetricBatchRepository.batchInsert(rows)
    }

    fun getMetrics(
        season: Int,
        week: Int,
    ): List<TeamResumeMetricResponse> {
        val metrics = teamResumeMetricRepository.findBySeasonAndWeek(season, week)
        val teamNamesById = teamRepository.findAllById(metrics.map { it.teamId }.distinct()).associate { it.id to it.name }
        return metrics.map { metric ->
            TeamResumeMetricResponse(
                season = metric.season,
                week = metric.week,
                teamId = metric.teamId,
                teamName = teamNamesById[metric.teamId],
                overallWins = metric.overallWins,
                overallLosses = metric.overallLosses,
                conferenceWins = metric.conferenceWins,
                conferenceLosses = metric.conferenceLosses,
                q1Wins = metric.q1Wins,
                q1Losses = metric.q1Losses,
                thWins = metric.thWins,
                thLosses = metric.thLosses,
                q4Wins = metric.q4Wins,
                q4Losses = metric.q4Losses,
                t25Wins = metric.t25Wins,
                t25Losses = metric.t25Losses,
                t50Wins = metric.t50Wins,
                t50Losses = metric.t50Losses,
                t100Wins = metric.t100Wins,
                t100Losses = metric.t100Losses,
                avgOpponentCompositeRank = metric.avgOpponentCompositeRank,
                compositeSos = metric.compositeSos,
            )
        }
    }
}
