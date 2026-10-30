package com.fcfb.arceus.domain.appearance

import com.fcfb.arceus.dto.response.StadiumGameResponse
import com.fcfb.arceus.dto.response.StadiumTeamRecordResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StadiumRecordsTest {
    @Test
    fun `records count only finished games with a winner`() {
        val games =
            listOf(
                game("Wyoming", "Boise State", 31, 17),
                game("Wyoming", "Utah State", 10, 24),
                game("Wyoming", "Air Force", 14, 14),
                game("Wyoming", "Colorado State", null, null, final = false),
            )

        assertEquals(
            listOf(
                StadiumTeamRecordResponse("Wyoming", 1, 1),
                StadiumTeamRecordResponse("Boise State", 0, 1),
                StadiumTeamRecordResponse("Utah State", 1, 0),
            ),
            StadiumRecords.of(games),
        )
    }

    private fun game(
        home: String,
        away: String,
        homeScore: Int?,
        awayScore: Int?,
        final: Boolean = true,
    ) = StadiumGameResponse(null, 12, 1, home, away, homeScore, awayScore, final, null, null)
}
