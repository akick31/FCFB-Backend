package com.fcfb.arceus.service.fcfb.elo

import com.fcfb.arceus.dto.response.EloRebuildResult
import com.fcfb.arceus.dto.response.EloTeamChange
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.fcfb.gamestats.GameStatsCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs

@Service
class EloRebuildService(
    private val gameRepository: GameRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val teamRepository: TeamRepository,
) {
    @Transactional(rollbackFor = [Exception::class])
    fun rebuild(apply: Boolean): EloRebuildResult {
        val games = gameRepository.getAllGames()
        val gamesById = games.associateBy { it.gameId }
        val replay = EloReplay.replay(games)
        val staleTeams = teamRepository.findAll().filter { isStale(it.currentElo, replay.finalElo[it.name]) }
        val staleSpreadGames = games.filter { isSpreadStale(it, replay) }
        val staleStats = gameStatsRepository.findAll().filter { isStatsStale(it, gamesById[it.gameId], replay) }
        if (apply) {
            staleTeams.forEach { it.currentElo = replay.finalElo.getValue(it.name!!) }
            staleSpreadGames.forEach { applySpreads(it, replay) }
            staleStats.forEach { refreshStats(it, gamesById.getValue(it.gameId), replay) }
            teamRepository.saveAll(staleTeams)
            gameRepository.saveAll(staleSpreadGames)
            gameStatsRepository.saveAll(staleStats)
        }
        return EloRebuildResult(
            applied = apply,
            gamesReplayed = games.size,
            teamsChanged = staleTeams.map { toChange(it, replay) },
            gameSpreadsChanged = staleSpreadGames.size,
            gameStatsRowsChanged = staleStats.size,
        )
    }

    private fun isStale(
        stored: Double,
        rebuilt: Double?,
    ) = rebuilt != null && abs(stored - rebuilt) > TOLERANCE

    private fun homeSpread(
        game: Game,
        replay: EloReplayResult,
    ) = VegasSpreadCalculator.homeSpread(
        replay.preGameElo.getValue(game.gameId to game.homeTeam),
        replay.preGameElo.getValue(game.gameId to game.awayTeam),
    )

    private fun isSpreadStale(
        game: Game,
        replay: EloReplayResult,
    ): Boolean {
        val expected = homeSpread(game, replay)
        val stored = game.homeVegasSpread
        return stored == null || game.awayVegasSpread == null || abs(stored - expected) > TOLERANCE
    }

    private fun applySpreads(
        game: Game,
        replay: EloReplayResult,
    ) {
        val spread = homeSpread(game, replay)
        game.homeVegasSpread = spread
        game.awayVegasSpread = -spread
    }

    private fun teamSpread(
        stats: GameStats,
        game: Game,
        replay: EloReplayResult,
    ) = homeSpread(game, replay).let { if (stats.team == game.homeTeam) it else -it }

    private fun teamWon(
        stats: GameStats,
        game: Game,
    ) = if (stats.team == game.homeTeam) game.homeScore > game.awayScore else game.awayScore > game.homeScore

    private fun isStatsStale(
        stats: GameStats,
        game: Game?,
        replay: EloReplayResult,
    ): Boolean {
        val teamName = stats.team
        if (game == null || teamName == null) return false
        val preGameElo = replay.preGameElo[game.gameId to teamName] ?: return false
        val spread = teamSpread(stats, game, replay)
        return isStale(stats.teamElo, preGameElo) ||
            differs(stats.favoredMargin, GameStatsCalculator.calculateFavoredMargin(spread)) ||
            differs(stats.upsetMargin, GameStatsCalculator.calculateUpsetMargin(spread, teamWon(stats, game)))
    }

    private fun differs(
        stored: Double?,
        expected: Double?,
    ) = if (stored == null || expected == null) stored != expected else abs(stored - expected) > TOLERANCE

    private fun refreshStats(
        stats: GameStats,
        game: Game,
        replay: EloReplayResult,
    ) {
        val spread = teamSpread(stats, game, replay)
        stats.teamElo = replay.preGameElo.getValue(game.gameId to stats.team!!)
        stats.favoredMargin = GameStatsCalculator.calculateFavoredMargin(spread)
        stats.upsetMargin = GameStatsCalculator.calculateUpsetMargin(spread, teamWon(stats, game))
    }

    private fun toChange(
        team: Team,
        replay: EloReplayResult,
    ) = EloTeamChange(team.name!!, team.currentElo, replay.finalElo.getValue(team.name!!))

    private companion object {
        const val TOLERANCE = 0.01
    }
}
