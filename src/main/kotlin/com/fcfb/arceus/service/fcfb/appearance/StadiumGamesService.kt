package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.domain.appearance.StadiumRecords
import com.fcfb.arceus.dto.response.StadiumGameResponse
import com.fcfb.arceus.dto.response.StadiumGamesResponse
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.schedule.Schedule
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.schedule.ScheduleRepository
import org.springframework.stereotype.Service

@Service
class StadiumGamesService(
    private val gameRepository: GameRepository,
    private val scheduleRepository: ScheduleRepository,
) {
    fun games(venue: String): StadiumGamesResponse {
        val games = gameRepository.findAllAtVenue(venue.trim()).filter { it.gameType != GameType.SCRIMMAGE }.map { it.toResponse() }
        return StadiumGamesResponse(games, StadiumRecords.of(games))
    }

    fun upcoming(venue: String): List<StadiumGameResponse> =
        scheduleRepository.findUpcomingAtVenue(venue.trim()).filter { it.gameType != GameType.SCRIMMAGE }.map { it.toResponse() }

    private fun Game.toResponse() =
        StadiumGameResponse(
            gameId = gameId,
            season = season,
            week = week,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = homeScore,
            awayScore = awayScore,
            final = gameStatus == GameStatus.FINAL,
            gameType = gameType,
            postseasonGameName = postseasonGameName,
        )

    private fun Schedule.toResponse() =
        StadiumGameResponse(
            gameId = gameId,
            season = season,
            week = week,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = null,
            awayScore = null,
            final = false,
            gameType = gameType,
            postseasonGameName = postseasonGameName,
        )
}
