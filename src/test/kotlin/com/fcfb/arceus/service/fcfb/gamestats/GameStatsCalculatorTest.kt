package com.fcfb.arceus.service.fcfb.gamestats

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameStatsCalculatorTest {
    private fun play(
        call: PlayCall,
        result: ActualResult,
        possession: TeamSide = TeamSide.HOME,
    ) = Play().apply {
        playCall = call
        actualResult = result
        this.possession = possession
    }

    @Test
    fun `kick return touchdowns count normal and squib kickoffs`() {
        val plays =
            listOf(
                play(PlayCall.KICKOFF_NORMAL, ActualResult.RETURN_TOUCHDOWN),
                play(PlayCall.KICKOFF_SQUIB, ActualResult.RETURN_TOUCHDOWN),
                play(PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF),
                play(PlayCall.KICKOFF_NORMAL, ActualResult.KICKING_TEAM_TOUCHDOWN),
                play(PlayCall.PUNT, ActualResult.PUNT_RETURN_TOUCHDOWN),
            )

        assertEquals(2, GameStatsCalculator.calculateKickReturnTd(plays))
    }

    @Test
    fun `return touchdown scores for the receiving team and kicking team touchdown for the kicker`() {
        val plays =
            listOf(
                play(PlayCall.KICKOFF_NORMAL, ActualResult.RETURN_TOUCHDOWN, TeamSide.AWAY),
                play(PlayCall.KICKOFF_NORMAL, ActualResult.KICKING_TEAM_TOUCHDOWN, TeamSide.HOME),
            )

        assertEquals(2, GameStatsCalculator.calculateTouchdowns(plays, TeamSide.HOME))
        assertEquals(0, GameStatsCalculator.calculateTouchdowns(plays, TeamSide.AWAY))
    }
}
