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

    private fun redZonePlay(
        id: Int,
        call: PlayCall,
        result: ActualResult,
        location: Int,
        possession: TeamSide = TeamSide.HOME,
    ) = play(call, result, possession).apply {
        playId = id
        ballLocation = location
    }

    @Test
    fun `a drive that starts inside the red zone counts as a trip, not just a score`() {
        val plays =
            listOf(
                redZonePlay(1, PlayCall.RUN, ActualResult.GAIN, 85),
                redZonePlay(2, PlayCall.RUN, ActualResult.TOUCHDOWN, 92),
                redZonePlay(3, PlayCall.PAT, ActualResult.GOOD, 97),
            )

        val trips = GameStatsCalculator.calculateRedZoneTrips(plays, TeamSide.HOME)

        assertEquals(1, trips.attempts)
        assertEquals(1, trips.successes)
    }

    @Test
    fun `a touchdown from outside the red zone is neither a trip nor a score`() {
        val plays =
            listOf(
                redZonePlay(1, PlayCall.RUN, ActualResult.GAIN, 30),
                redZonePlay(2, PlayCall.PASS, ActualResult.TOUCHDOWN, 45),
            )

        val trips = GameStatsCalculator.calculateRedZoneTrips(plays, TeamSide.HOME)

        assertEquals(0, trips.attempts)
        assertEquals(0, trips.successes)
    }

    @Test
    fun `successes never exceed attempts across several short field drives`() {
        val plays =
            listOf(
                redZonePlay(1, PlayCall.RUN, ActualResult.TOUCHDOWN, 88),
                redZonePlay(2, PlayCall.PAT, ActualResult.GOOD, 97),
                redZonePlay(3, PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF, 35),
                redZonePlay(4, PlayCall.RUN, ActualResult.GAIN, 20, TeamSide.AWAY),
                redZonePlay(5, PlayCall.PASS, ActualResult.TOUCHDOWN, 83),
                redZonePlay(6, PlayCall.PAT, ActualResult.GOOD, 97),
            )

        val trips = GameStatsCalculator.calculateRedZoneTrips(plays, TeamSide.HOME)

        assertEquals(2, trips.attempts)
        assertEquals(2, trips.successes)
    }

    @Test
    fun `a trip that ends without a touchdown counts an attempt and no success`() {
        val plays =
            listOf(
                redZonePlay(1, PlayCall.RUN, ActualResult.GAIN, 55),
                redZonePlay(2, PlayCall.PASS, ActualResult.FIRST_DOWN, 82),
                redZonePlay(3, PlayCall.RUN, ActualResult.NO_GAIN, 88),
                redZonePlay(4, PlayCall.FIELD_GOAL, ActualResult.GOOD, 88),
            )

        val trips = GameStatsCalculator.calculateRedZoneTrips(plays, TeamSide.HOME)

        assertEquals(1, trips.attempts)
        assertEquals(0, trips.successes)
    }

    @Test
    fun `reaching the red zone twice on one drive still counts one trip`() {
        val plays =
            listOf(
                redZonePlay(1, PlayCall.PASS, ActualResult.FIRST_DOWN, 81),
                redZonePlay(2, PlayCall.RUN, ActualResult.LOSS, 85),
                redZonePlay(3, PlayCall.PASS, ActualResult.TOUCHDOWN, 90),
            )

        val trips = GameStatsCalculator.calculateRedZoneTrips(plays, TeamSide.HOME)

        assertEquals(1, trips.attempts)
        assertEquals(1, trips.successes)
    }

    @Test
    fun `punt return touchdowns and blocked punts belong to the receiving team`() {
        val ourPunt = play(PlayCall.PUNT, ActualResult.PUNT_RETURN_TOUCHDOWN, TeamSide.HOME)
        val theirPunt = play(PlayCall.PUNT, ActualResult.PUNT_RETURN_TOUCHDOWN, TeamSide.AWAY)
        val ourBlockedPunt = play(PlayCall.PUNT, ActualResult.BLOCKED, TeamSide.HOME)
        val theirBlockedPunt = play(PlayCall.PUNT, ActualResult.BLOCKED, TeamSide.AWAY)
        val plays = listOf(ourPunt, theirPunt, ourBlockedPunt, theirBlockedPunt)
        val awayPlays = plays.filter { it.possession == TeamSide.AWAY }

        assertEquals(1, GameStatsCalculator.calculatePuntReturnTd(awayPlays))
        assertEquals(1, GameStatsCalculator.calculateBlockedOpponentPunt(awayPlays))
    }
}
