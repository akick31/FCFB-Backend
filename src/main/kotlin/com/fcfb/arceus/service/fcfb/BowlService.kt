package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.ScheduleEntry
import com.fcfb.arceus.model.Bowl
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.repositories.BowlRepository
import org.springframework.stereotype.Service

@Service
class BowlService(
    private val bowlRepository: BowlRepository,
) {
    fun getByName(name: String): Bowl? = bowlRepository.findById(name.trim()).orElse(null)

    fun upsertPreGame(entry: ScheduleEntry) {
        val name = entry.postseasonGameName?.trim()?.takeIf { it.isNotBlank() } ?: return
        val bowl = bowlRepository.findById(name).orElseGet { Bowl(name) }
        entry.postseasonGameLogo?.trim()?.takeIf { it.isNotBlank() }?.let { bowl.logo = it }
        bowl.lastSeason = entry.season
        bowl.lastHomeTeam = entry.homeTeam
        bowl.lastAwayTeam = entry.awayTeam
        entry.venue?.trim()?.takeIf { it.isNotBlank() }?.let { bowl.lastVenue = it }
        bowlRepository.save(bowl)
    }

    fun recordResult(
        schedule: Schedule,
        game: Game,
    ) {
        val name = schedule.postseasonGameName?.trim()?.takeIf { it.isNotBlank() } ?: return
        val bowl = bowlRepository.findById(name).orElseGet { Bowl(name) }
        bowl.lastSeason = game.season
        bowl.lastHomeTeam = game.homeTeam
        bowl.lastAwayTeam = game.awayTeam
        bowl.lastHomeScore = game.homeScore
        bowl.lastAwayScore = game.awayScore
        bowl.lastGameId = game.gameId
        game.venue?.trim()?.takeIf { it.isNotBlank() }?.let { bowl.lastVenue = it }
        schedule.postseasonGameLogo?.trim()?.takeIf { it.isNotBlank() }?.let { bowl.logo = it }
        bowlRepository.save(bowl)
    }
}
