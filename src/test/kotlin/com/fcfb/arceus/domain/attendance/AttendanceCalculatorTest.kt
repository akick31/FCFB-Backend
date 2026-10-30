package com.fcfb.arceus.domain.attendance

import com.fcfb.arceus.enums.game.GameType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AttendanceCalculatorTest {
    @ParameterizedTest
    @CsvSource(
        "0, 5, 6, CONFERENCE_GAME, 1.0",
        "3, 3, 9, CONFERENCE_GAME, 1.0",
        "5, 2, 9, OUT_OF_CONFERENCE, 1.0",
        "2, 4, 7, CONFERENCE_GAME, 0.76",
        "1, 6, 9, CONFERENCE_GAME, 0.45",
        "0, 9, 11, CONFERENCE_GAME, 0.45",
        "1, 7, 14, BOWL, 1.0",
        "6, 7, 13, CONFERENCE_CHAMPIONSHIP, 1.0",
        "8, 4, 15, PLAYOFFS, 1.0",
    )
    fun `crowds thin out for losing teams late in the regular season only`(
        wins: Int,
        losses: Int,
        week: Int,
        gameType: GameType,
        expected: Double,
    ) {
        assertEquals(expected, AttendanceCalculator.fill(wins, losses, week, gameType), 1e-9)
    }
}
