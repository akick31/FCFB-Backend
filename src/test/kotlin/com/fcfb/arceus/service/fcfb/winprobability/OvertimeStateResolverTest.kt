package com.fcfb.arceus.service.fcfb.winprobability

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.winprobability.OvertimeState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OvertimeStateResolverTest {
    private val touchdown =
        Play().apply {
            quarter = 5
            possession = TeamSide.HOME
            homeScore = 41
            awayScore = 35
        }

    @Test
    fun `following PAT marks the scoring first team as pending a conversion`() {
        val pat =
            Play().apply {
                quarter = 5
                possession = TeamSide.HOME
                playCall = PlayCall.PAT
                ballLocation = 97
                down = 1
                yardsToGo = 10
            }

        assertEquals(
            OvertimeState(TeamSide.HOME, 1, true, 6, true, 1, 10, 3),
            OvertimeStateResolver.fromFollowingPlay(touchdown, pat, TeamSide.HOME),
        )
    }

    @Test
    fun `following possession by the other team is the second possession of the period`() {
        val answer =
            Play().apply {
                quarter = 5
                possession = TeamSide.AWAY
                playCall = PlayCall.RUN
                ballLocation = 75
                down = 1
                yardsToGo = 10
            }

        assertEquals(
            OvertimeState(TeamSide.AWAY, 1, false, -6, false, 1, 10, 25),
            OvertimeStateResolver.fromFollowingPlay(touchdown, answer, TeamSide.HOME),
        )
    }

    @Test
    fun `live game state reflects the period half and pending conversion`() {
        val game =
            Game().apply {
                possession = TeamSide.HOME
                quarter = 6
                overtimeHalf = 2
                currentPlayType = PlayType.PAT
                ballLocation = 97
                down = 1
                yardsToGo = 10
            }

        assertEquals(
            OvertimeState(TeamSide.HOME, 2, false, 6, true, 1, 10, 3),
            OvertimeStateResolver.fromLiveGame(game, touchdown),
        )
    }
}
