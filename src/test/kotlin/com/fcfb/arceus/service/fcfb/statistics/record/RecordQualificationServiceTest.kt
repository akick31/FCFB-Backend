package com.fcfb.arceus.service.fcfb.statistics.record

import com.fcfb.arceus.domain.season.SeasonStructure
import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordQualificationServiceTest {
    private val recordStatValueService = RecordStatValueService(mockk<GameStatsRepository>(), mockk<GameRepository>())
    private val recordQualificationService = RecordQualificationService(recordStatValueService)

    private fun game(
        plays: Int = 0,
        passAttempts: Int = 0,
        rushAttempts: Int = 0,
        fourthDownAttempts: Int = 0,
        season: Int = 11,
        week: Int = 1,
    ) = GameStats(gameId = 1, team = "Arkansas", season = season).apply {
        this.week = week
        offensivePlayCount = plays
        this.passAttempts = passAttempts
        this.rushAttempts = rushAttempts
        fourthDownConversionAttempts = fourthDownAttempts
    }

    @Test
    fun `counting stats are never held to a minimum`() {
        assertFalse(recordQualificationService.isVolumeSensitive(Stats.PASS_YARDS))
        assertTrue(recordQualificationService.qualifiesForGameRecord(Stats.PASS_YARDS, game(plays = 4)))
        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.PASS_YARDS, listOf(game(plays = 4)), false))
    }

    @Test
    fun `a single game percentage needs a full game's worth of plays`() {
        assertTrue(
            recordQualificationService.qualifiesForGameRecord(
                Stats.PASS_COMPLETION_PERCENTAGE,
                game(plays = RecordQualificationService.MIN_SINGLE_GAME_PLAYS),
            ),
        )
        assertFalse(
            recordQualificationService.qualifiesForGameRecord(
                Stats.PASS_COMPLETION_PERCENTAGE,
                game(plays = RecordQualificationService.MIN_SINGLE_GAME_PLAYS - 1),
            ),
        )
    }

    @Test
    fun `the season minimum scales with the number of games played`() {
        val fourGames = List(4) { game(passAttempts = 10) }
        val twelveGames = List(12) { game(passAttempts = 10) }
        val twelveThinGames = List(12) { game(passAttempts = 9) }

        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, fourGames, true))
        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, twelveGames, true))
        assertFalse(recordQualificationService.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, twelveThinGames, true))
    }

    @Test
    fun `a floor keeps a thin sample from setting a fourth down record`() {
        val onePostseasonGame = listOf(game(fourthDownAttempts = 1))
        val sparseSeason = List(12) { game(fourthDownAttempts = 1) }

        assertFalse(recordQualificationService.qualifiesForSeasonRecord(Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE, onePostseasonGame, true))
        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE, sparseSeason, true))
    }

    @Test
    fun `per play averages fall back to the team's play count`() {
        assertTrue(recordQualificationService.isVolumeSensitive(Stats.AVERAGE_YARDS_PER_PLAY))
        assertFalse(recordQualificationService.qualifiesForSeasonRecord(Stats.AVERAGE_YARDS_PER_PLAY, listOf(game(plays = 20)), true))
        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.AVERAGE_YARDS_PER_PLAY, listOf(game(plays = 40)), true))
    }

    @Test
    fun `an empty season never qualifies`() {
        assertFalse(recordQualificationService.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, emptyList(), true))
    }

    @Test
    fun `a rate cannot be a season record until the season is over`() {
        val fullSeason = List(12) { game(passAttempts = 25) }

        assertFalse(recordQualificationService.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, fullSeason, false))
        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, fullSeason, true))
    }

    @Test
    fun `season 10 started mid season so it cannot hold a regular season rate record`() {
        val partialSeason = List(12) { game(passAttempts = 25, season = 10) }

        assertFalse(recordQualificationService.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, partialSeason, true))
        assertFalse(
            recordQualificationService.qualifiesForSeasonRecord(
                Stats.AVERAGE_YARDS_PER_PLAY,
                List(12) {
                    game(plays = 40, season = 10)
                },
                true,
            ),
        )
    }

    @Test
    fun `season 10 still holds its postseason and counting records`() {
        val postseason = List(2) { game(plays = 40, season = 10, week = SeasonStructure.POSTSEASON_START_WEEK) }

        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.AVERAGE_YARDS_PER_PLAY, postseason, true))
        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.PASS_YARDS, List(8) { game(season = 10) }, true))
    }

    @Test
    fun `counting stats still set season records while the season is live`() {
        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.TOTAL_YARDS, List(3) { game(plays = 40) }, false))
    }

    @Test
    fun `conversion and special teams rates only hold a full season record`() {
        assertTrue(recordQualificationService.allowsRecordType(Stats.THIRD_DOWN_CONVERSION_PERCENTAGE, RecordType.SINGLE_SEASON))
        assertTrue(recordQualificationService.allowsRecordType(Stats.THIRD_DOWN_CONVERSION_PERCENTAGE, RecordType.SINGLE_SEASON_LOWEST))
        assertFalse(recordQualificationService.allowsRecordType(Stats.THIRD_DOWN_CONVERSION_PERCENTAGE, RecordType.SINGLE_GAME))
        assertFalse(recordQualificationService.allowsRecordType(Stats.RED_ZONE_SUCCESS_PERCENTAGE, RecordType.SINGLE_POSTSEASON))
        assertFalse(
            recordQualificationService.allowsRecordType(Stats.RED_ZONE_SUCCESS_PERCENTAGE_AGAINST, RecordType.SINGLE_POSTSEASON_GAME),
        )
        assertFalse(recordQualificationService.allowsRecordType(Stats.AVERAGE_PUNT_LENGTH, RecordType.SINGLE_GAME))
    }

    @Test
    fun `counting stats and per play averages still hold single game records`() {
        assertTrue(recordQualificationService.allowsRecordType(Stats.PASS_YARDS, RecordType.SINGLE_GAME))
        assertTrue(recordQualificationService.allowsRecordType(Stats.AVERAGE_YARDS_PER_PLAY, RecordType.SINGLE_GAME))
        assertTrue(recordQualificationService.allowsRecordType(Stats.AVERAGE_OFFENSIVE_DIFF, RecordType.SINGLE_POSTSEASON_GAME))
    }

    @Test
    fun `yards per attempt is a full season record held to the attempt minimum`() {
        assertTrue(recordQualificationService.allowsRecordType(Stats.AVERAGE_PASS_YARDS_PER_ATTEMPT, RecordType.SINGLE_SEASON))
        assertFalse(recordQualificationService.allowsRecordType(Stats.AVERAGE_PASS_YARDS_PER_ATTEMPT, RecordType.SINGLE_GAME))
        val carriedSeason = List(12) { game(rushAttempts = 12) }
        val thinSeason = List(12) { game(rushAttempts = 8) }

        assertTrue(recordQualificationService.qualifiesForSeasonRecord(Stats.AVERAGE_RUSH_YARDS_PER_ATTEMPT, carriedSeason, true))
        assertFalse(recordQualificationService.qualifiesForSeasonRecord(Stats.AVERAGE_RUSH_YARDS_PER_ATTEMPT, thinSeason, true))
    }
}
