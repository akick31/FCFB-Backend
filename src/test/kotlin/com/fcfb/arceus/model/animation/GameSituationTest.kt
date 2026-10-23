package com.fcfb.arceus.model.animation

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.play.Play
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameSituationTest {
    private val madeFieldGoal =
        Play().apply {
            homeScore = 17
            awayScore = 10
            quarter = 2
            clock = 522
            down = 4
            yardsToGo = 7
        }

    @Test
    fun `the board shows the score going into the play, not the result of it`() {
        val previousPlay =
            Play().apply {
                homeScore = 14
                awayScore = 10
            }

        val situation = GameSituation.beforeSnap(madeFieldGoal, previousPlay, "Wyoming", "Boise State")

        assertEquals(14, situation.homeScore)
        assertEquals(10, situation.awayScore)
        assertEquals(2, situation.quarter)
        assertEquals(522, situation.clockSeconds)
        assertEquals(4, situation.down)
        assertEquals(7, situation.yardsToGo)
    }

    @Test
    fun `the first play of a game starts from zero to zero`() {
        val situation = GameSituation.beforeSnap(madeFieldGoal, null, "Wyoming", "Boise State")

        assertEquals(0, situation.homeScore)
        assertEquals(0, situation.awayScore)
    }

    @Test
    fun `wide boards get possession, ball spot and timeouts from the pre-snap play`() {
        val play =
            Play().apply {
                possession = TeamSide.AWAY
                ballLocation = 68
                homeTimeouts = 1
                awayTimeouts = 3
            }

        val situation = GameSituation.beforeSnap(play, null, "WYO", "BSU")

        assertEquals(TeamSide.AWAY, situation.possession)
        assertEquals(32, situation.ballOn)
        assertEquals(1, situation.homeTimeouts)
        assertEquals(3, situation.awayTimeouts)
    }
}
