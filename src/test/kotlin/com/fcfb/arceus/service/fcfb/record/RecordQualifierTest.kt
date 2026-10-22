package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordQualifierTest {
    private val recordStatUtils = RecordStatUtils(mockk<GameStatsRepository>(), mockk<GameRepository>())
    private val recordQualifier = RecordQualifier(recordStatUtils)

    private fun game(
        plays: Int = 0,
        passAttempts: Int = 0,
        rushAttempts: Int = 0,
        fourthDownAttempts: Int = 0,
    ) = GameStats(gameId = 1, team = "Arkansas", season = 11).apply {
        offensivePlayCount = plays
        this.passAttempts = passAttempts
        this.rushAttempts = rushAttempts
        fourthDownConversionAttempts = fourthDownAttempts
    }

    @Test
    fun `counting stats are never held to a minimum`() {
        assertFalse(recordQualifier.isVolumeSensitive(Stats.PASS_YARDS))
        assertTrue(recordQualifier.qualifiesForGameRecord(Stats.PASS_YARDS, game(plays = 4)))
        assertTrue(recordQualifier.qualifiesForSeasonRecord(Stats.PASS_YARDS, listOf(game(plays = 4)), false))
    }

    @Test
    fun `a single game percentage needs a full game's worth of plays`() {
        assertTrue(
            recordQualifier.qualifiesForGameRecord(
                Stats.PASS_COMPLETION_PERCENTAGE,
                game(plays = RecordQualifier.MIN_SINGLE_GAME_PLAYS),
            ),
        )
        assertFalse(
            recordQualifier.qualifiesForGameRecord(
                Stats.PASS_COMPLETION_PERCENTAGE,
                game(plays = RecordQualifier.MIN_SINGLE_GAME_PLAYS - 1),
            ),
        )
    }

    @Test
    fun `the season minimum scales with the number of games played`() {
        val fourGames = List(4) { game(passAttempts = 10) }
        val twelveGames = List(12) { game(passAttempts = 10) }
        val twelveThinGames = List(12) { game(passAttempts = 9) }

        assertTrue(recordQualifier.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, fourGames, true))
        assertTrue(recordQualifier.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, twelveGames, true))
        assertFalse(recordQualifier.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, twelveThinGames, true))
    }

    @Test
    fun `a floor keeps a thin sample from setting a fourth down record`() {
        val onePostseasonGame = listOf(game(fourthDownAttempts = 1))
        val sparseSeason = List(12) { game(fourthDownAttempts = 1) }

        assertFalse(recordQualifier.qualifiesForSeasonRecord(Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE, onePostseasonGame, true))
        assertTrue(recordQualifier.qualifiesForSeasonRecord(Stats.FOURTH_DOWN_CONVERSION_PERCENTAGE, sparseSeason, true))
    }

    @Test
    fun `per play averages fall back to the team's play count`() {
        assertTrue(recordQualifier.isVolumeSensitive(Stats.AVERAGE_YARDS_PER_PLAY))
        assertFalse(recordQualifier.qualifiesForSeasonRecord(Stats.AVERAGE_YARDS_PER_PLAY, listOf(game(plays = 20)), true))
        assertTrue(recordQualifier.qualifiesForSeasonRecord(Stats.AVERAGE_YARDS_PER_PLAY, listOf(game(plays = 40)), true))
    }

    @Test
    fun `an empty season never qualifies`() {
        assertFalse(recordQualifier.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, emptyList(), true))
    }

    @Test
    fun `a rate cannot be a season record until the season is over`() {
        val fullSeason = List(12) { game(passAttempts = 25) }

        assertFalse(recordQualifier.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, fullSeason, false))
        assertTrue(recordQualifier.qualifiesForSeasonRecord(Stats.PASS_SUCCESS_PERCENTAGE, fullSeason, true))
    }

    @Test
    fun `counting stats still set season records while the season is live`() {
        assertTrue(recordQualifier.qualifiesForSeasonRecord(Stats.TOTAL_YARDS, List(3) { game(plays = 40) }, false))
    }

    @Test
    fun `conversion and special teams rates only hold a full season record`() {
        assertTrue(recordQualifier.allowsRecordType(Stats.THIRD_DOWN_CONVERSION_PERCENTAGE, RecordType.SINGLE_SEASON))
        assertTrue(recordQualifier.allowsRecordType(Stats.THIRD_DOWN_CONVERSION_PERCENTAGE, RecordType.SINGLE_SEASON_LOWEST))
        assertFalse(recordQualifier.allowsRecordType(Stats.THIRD_DOWN_CONVERSION_PERCENTAGE, RecordType.SINGLE_GAME))
        assertFalse(recordQualifier.allowsRecordType(Stats.RED_ZONE_SUCCESS_PERCENTAGE, RecordType.SINGLE_POSTSEASON))
        assertFalse(recordQualifier.allowsRecordType(Stats.RED_ZONE_SUCCESS_PERCENTAGE_AGAINST, RecordType.SINGLE_POSTSEASON_GAME))
        assertFalse(recordQualifier.allowsRecordType(Stats.AVERAGE_PUNT_LENGTH, RecordType.SINGLE_GAME))
    }

    @Test
    fun `counting stats and per play averages still hold single game records`() {
        assertTrue(recordQualifier.allowsRecordType(Stats.PASS_YARDS, RecordType.SINGLE_GAME))
        assertTrue(recordQualifier.allowsRecordType(Stats.AVERAGE_YARDS_PER_PLAY, RecordType.SINGLE_GAME))
        assertTrue(recordQualifier.allowsRecordType(Stats.AVERAGE_OFFENSIVE_DIFF, RecordType.SINGLE_POSTSEASON_GAME))
    }

    @Test
    fun `yards per attempt is a full season record held to the attempt minimum`() {
        assertTrue(recordQualifier.allowsRecordType(Stats.AVERAGE_PASS_YARDS_PER_ATTEMPT, RecordType.SINGLE_SEASON))
        assertFalse(recordQualifier.allowsRecordType(Stats.AVERAGE_PASS_YARDS_PER_ATTEMPT, RecordType.SINGLE_GAME))
        val carriedSeason = List(12) { game(rushAttempts = 12) }
        val thinSeason = List(12) { game(rushAttempts = 8) }

        assertTrue(recordQualifier.qualifiesForSeasonRecord(Stats.AVERAGE_RUSH_YARDS_PER_ATTEMPT, carriedSeason, true))
        assertFalse(recordQualifier.qualifiesForSeasonRecord(Stats.AVERAGE_RUSH_YARDS_PER_ATTEMPT, thinSeason, true))
    }
}
