package com.fcfb.arceus.service.fcfb.gamestats

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
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

    @Test
    fun `offensive totals include sacks and turnovers but exclude special teams kneels and spikes`() {
        val plays =
            listOf(
                play(PlayCall.PASS, ActualResult.FIRST_DOWN).apply { yards = 20 },
                play(PlayCall.PASS, ActualResult.LOSS).apply { yards = -10 },
                play(PlayCall.RUN, ActualResult.TURNOVER).apply { yards = -5 },
                play(PlayCall.RUN, ActualResult.SAFETY).apply { yards = -5 },
                play(PlayCall.PASS, ActualResult.NO_GAIN).apply { yards = 0 },
                play(PlayCall.KNEEL, ActualResult.LOSS).apply { yards = -1 },
                play(PlayCall.SPIKE, ActualResult.NO_GAIN),
                play(PlayCall.PUNT, ActualResult.PUNT).apply { yards = 40 },
            )

        assertEquals(5, GameStatsCalculator.calculateOffensivePlayCount(plays))
        assertEquals(0, GameStatsCalculator.calculateOffensivePlayYards(plays))
        assertEquals(0.0, GameStatsCalculator.calculateAverageYardsPerPlay(plays))
    }

    @Test
    fun `punt totals exclude attempts without a recorded punt length`() {
        val plays =
            listOf(
                play(PlayCall.PUNT, ActualResult.PUNT).apply { result = Scenario.FORTY_YARD_PUNT },
                play(PlayCall.PUNT, ActualResult.PUNT).apply { result = Scenario.SIXTY_YARD_PUNT },
                play(PlayCall.PUNT, ActualResult.BLOCKED).apply { result = Scenario.BLOCKED_PUNT },
                play(PlayCall.PUNT, ActualResult.PUNT_RETURN_TOUCHDOWN).apply { result = Scenario.PUNT_RETURN_TOUCHDOWN },
                play(PlayCall.RUN, ActualResult.FIRST_DOWN).apply { yards = 40 },
            )

        assertEquals(100, GameStatsCalculator.calculatePuntYards(plays))
        assertEquals(2, GameStatsCalculator.calculatePuntCount(plays))
        assertEquals(50.0, GameStatsCalculator.calculateAveragePuntLength(plays))
    }

    @Test
    fun `no eligible plays or punts produce finite zero averages`() {
        assertEquals(0.0, GameStatsCalculator.calculateAverageYardsPerPlay(emptyList()))
        assertEquals(0.0, GameStatsCalculator.calculateAveragePuntLength(emptyList()))
    }
}
