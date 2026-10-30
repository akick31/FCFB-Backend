package com.fcfb.arceus.service.fcfb.play

import com.fcfb.arceus.dto.response.WinProbabilitySwingResponse
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import org.springframework.stereotype.Service
import kotlin.math.abs

@Service
class WinProbabilitySwingService(
    private val playRepository: PlayRepository,
    private val gameRepository: GameRepository,
) {
    fun biggestSwing(
        season: Int,
        week: Int,
    ): WinProbabilitySwingResponse? {
        val play = playRepository.findBiggestSwingInWeek(season, week) ?: return null
        val game = gameRepository.getGameById(play.gameId) ?: return null
        val swing = play.winProbabilityAdded ?: return null
        val offense = if (play.possession == TeamSide.HOME) game.homeTeam else game.awayTeam
        val defense = if (play.possession == TeamSide.HOME) game.awayTeam else game.homeTeam
        return WinProbabilitySwingResponse(
            gameId = play.gameId,
            playId = play.playId,
            team = if (swing >= 0) offense else defense,
            opponent = if (swing >= 0) defense else offense,
            swing = abs(swing),
            quarter = play.quarter,
            clock = play.clock,
            homeTeam = game.homeTeam,
            awayTeam = game.awayTeam,
            homeScore = play.homeScore,
            awayScore = play.awayScore,
            result = play.actualResult?.name,
        )
    }
}
