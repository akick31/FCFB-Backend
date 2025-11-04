package com.fcfb.arceus.model

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayTest {
    @Test
    fun `test Play entity annotations`() {
        val play = Play()

        // Test that the class has the correct JPA annotations
        val entityAnnotation = Play::class.java.getAnnotation(javax.persistence.Entity::class.java)
        assertNotNull(entityAnnotation)

        val tableAnnotation = Play::class.java.getAnnotation(javax.persistence.Table::class.java)
        assertNotNull(tableAnnotation)
        assertEquals("play", tableAnnotation.name)
    }

    @Test
    fun `test Play default constructor`() {
        val play = Play()

        assertEquals(0, play.playId)
        assertEquals(0, play.gameId)
        assertEquals(0, play.playNumber)
        assertEquals(0, play.homeScore)
        assertEquals(0, play.awayScore)
        assertEquals(1, play.quarter)
        assertEquals(420, play.clock)
        assertEquals(0, play.ballLocation)
        assertEquals(TeamSide.HOME, play.possession)
        assertEquals(1, play.down)
        assertEquals(100, play.yardsToGo)
        assertNull(play.defensiveNumber)
        assertNull(play.offensiveNumber)
        assertNull(play.defensiveSubmitter)
        assertNull(play.offensiveSubmitter)
        assertNull(play.playCall)
        assertNull(play.result)
        assertNull(play.difference)
        assertNull(play.actualResult)
        assertEquals(0, play.yards)
        assertEquals(0, play.playTime)
        assertEquals(0, play.runoffTime)
        assertNull(play.winProbability)
        assertNull(play.winProbabilityAdded)
        assertFalse(play.timeoutUsed)
        assertFalse(play.offensiveTimeoutCalled)
        assertFalse(play.defensiveTimeoutCalled)
        assertEquals(3, play.homeTimeouts)
        assertEquals(3, play.awayTimeouts)
        assertFalse(play.playFinished)
        assertNull(play.offensiveResponseSpeed)
        assertNull(play.defensiveResponseSpeed)
    }

    @Test
    fun `test Play parameterized constructor`() {
        val play =
            Play(
                gameId = 123,
                playNumber = 5,
                homeScore = 14,
                awayScore = 7,
                quarter = 2,
                clock = 1200,
                ballLocation = 45,
                possession = TeamSide.AWAY,
                down = 3,
                yardsToGo = 8,
                defensiveNumber = "4-3",
                offensiveNumber = "11",
                offensiveSubmitter = "offensive_coach",
                offensiveSubmitterId = "offensive_coach_id",
                defensiveSubmitter = "defensive_coach",
                defensiveSubmitterId = "defensive_coach_id",
                playCall = PlayCall.PASS,
                result = Scenario.GAIN_OF_12_YARDS,
                actualResult = ActualResult.FIRST_DOWN,
                yards = 12,
                playTime = 25,
                runoffTime = 5,
                winProbability = 65.5,
                winProbabilityAdded = 2.3,
                homeTeam = "Alabama",
                awayTeam = "Georgia",
                difference = 4,
                timeoutUsed = false,
                offensiveTimeoutCalled = false,
                defensiveTimeoutCalled = false,
                homeTimeouts = 3,
                awayTimeouts = 2,
                playFinished = true,
                offensiveResponseSpeed = 15000L,
                defensiveResponseSpeed = 12000L,
            )

        assertEquals(123, play.gameId)
        assertEquals(5, play.playNumber)
        assertEquals(14, play.homeScore)
        assertEquals(7, play.awayScore)
        assertEquals(2, play.quarter)
        assertEquals(1200, play.clock)
        assertEquals(45, play.ballLocation)
        assertEquals(TeamSide.AWAY, play.possession)
        assertEquals(3, play.down)
        assertEquals(8, play.yardsToGo)
        assertEquals("4-3", play.defensiveNumber)
        assertEquals("11", play.offensiveNumber)
        assertEquals("offensive_coach", play.offensiveSubmitter)
        assertEquals("defensive_coach", play.defensiveSubmitter)
        assertEquals(PlayCall.PASS, play.playCall)
        assertEquals(Scenario.GAIN_OF_12_YARDS, play.result)
        assertEquals(ActualResult.FIRST_DOWN, play.actualResult)
        assertEquals(12, play.yards)
        assertEquals(25, play.playTime)
        assertEquals(5, play.runoffTime)
        assertEquals(65.5, play.winProbability)
        assertEquals(2.3, play.winProbabilityAdded)
        assertEquals("Alabama", play.homeTeam)
        assertEquals("Georgia", play.awayTeam)
        assertEquals(4, play.difference)
        assertFalse(play.timeoutUsed)
        assertFalse(play.offensiveTimeoutCalled)
        assertFalse(play.defensiveTimeoutCalled)
        assertEquals(3, play.homeTimeouts)
        assertEquals(2, play.awayTimeouts)
        assertTrue(play.playFinished)
        assertEquals(15000L, play.offensiveResponseSpeed)
        assertEquals(12000L, play.defensiveResponseSpeed)
    }

    @Test
    fun `test Play property mutability`() {
        val play = Play()

        // Test property mutability
        play.playId = 1
        play.gameId = 456
        play.playNumber = 10
        play.homeScore = 21
        play.awayScore = 14
        play.quarter = 3
        play.clock = 800
        play.ballLocation = 30
        play.possession = TeamSide.HOME
        play.down = 2
        play.yardsToGo = 15
        play.defensiveNumber = "3-4"
        play.offensiveNumber = "22"
        play.offensiveSubmitter = "new_offensive_coach"
        play.defensiveSubmitter = "new_defensive_coach"
        play.playCall = PlayCall.RUN
        play.result = Scenario.GAIN_OF_8_YARDS
        play.actualResult = ActualResult.GAIN
        play.yards = 8
        play.playTime = 30
        play.runoffTime = 8
        play.winProbability = 75.2
        play.winProbabilityAdded = -1.5
        play.homeTeam = "Michigan"
        play.awayTeam = "Ohio State"
        play.difference = -3
        play.timeoutUsed = true
        play.offensiveTimeoutCalled = true
        play.defensiveTimeoutCalled = false
        play.homeTimeouts = 2
        play.awayTimeouts = 1
        play.playFinished = true
        play.offensiveResponseSpeed = 20000L
        play.defensiveResponseSpeed = 18000L

        assertEquals(1, play.playId)
        assertEquals(456, play.gameId)
        assertEquals(10, play.playNumber)
        assertEquals(21, play.homeScore)
        assertEquals(14, play.awayScore)
        assertEquals(3, play.quarter)
        assertEquals(800, play.clock)
        assertEquals(30, play.ballLocation)
        assertEquals(TeamSide.HOME, play.possession)
        assertEquals(2, play.down)
        assertEquals(15, play.yardsToGo)
        assertEquals("3-4", play.defensiveNumber)
        assertEquals("22", play.offensiveNumber)
        assertEquals("new_offensive_coach", play.offensiveSubmitter)
        assertEquals("new_defensive_coach", play.defensiveSubmitter)
        assertEquals(PlayCall.RUN, play.playCall)
        assertEquals(Scenario.GAIN_OF_8_YARDS, play.result)
        assertEquals(ActualResult.GAIN, play.actualResult)
        assertEquals(8, play.yards)
        assertEquals(30, play.playTime)
        assertEquals(8, play.runoffTime)
        assertEquals(75.2, play.winProbability)
        assertEquals(-1.5, play.winProbabilityAdded)
        assertEquals("Michigan", play.homeTeam)
        assertEquals("Ohio State", play.awayTeam)
        assertEquals(-3, play.difference)
        assertTrue(play.timeoutUsed)
        assertTrue(play.offensiveTimeoutCalled)
        assertFalse(play.defensiveTimeoutCalled)
        assertEquals(2, play.homeTimeouts)
        assertEquals(1, play.awayTimeouts)
        assertTrue(play.playFinished)
        assertEquals(20000L, play.offensiveResponseSpeed)
        assertEquals(18000L, play.defensiveResponseSpeed)
    }

    @Test
    fun `test Play with null optional fields`() {
        val play = Play()
        play.defensiveNumber = null
        play.offensiveNumber = null
        play.defensiveSubmitter = null
        play.offensiveSubmitter = null
        play.playCall = null
        play.result = null
        play.difference = null
        play.actualResult = null
        play.winProbability = null
        play.winProbabilityAdded = null
        play.offensiveResponseSpeed = null
        play.defensiveResponseSpeed = null

        assertNull(play.defensiveNumber)
        assertNull(play.offensiveNumber)
        assertNull(play.defensiveSubmitter)
        assertNull(play.offensiveSubmitter)
        assertNull(play.playCall)
        assertNull(play.result)
        assertNull(play.difference)
        assertNull(play.actualResult)
        assertNull(play.winProbability)
        assertNull(play.winProbabilityAdded)
        assertNull(play.offensiveResponseSpeed)
        assertNull(play.defensiveResponseSpeed)
    }

    @Test
    fun `test Play with all TeamSide values`() {
        val play = Play()

        // Test all TeamSide values
        TeamSide.entries.forEach { teamSide ->
            play.possession = teamSide
            assertEquals(teamSide, play.possession)
        }
    }

    @Test
    fun `test Play with all PlayCall values`() {
        val play = Play()

        // Test all PlayCall values
        PlayCall.entries.forEach { playCall ->
            play.playCall = playCall
            assertEquals(playCall, play.playCall)
        }
    }

    @Test
    fun `test Play with all ActualResult values`() {
        val play = Play()

        // Test all ActualResult values
        ActualResult.entries.forEach { actualResult ->
            play.actualResult = actualResult
            assertEquals(actualResult, play.actualResult)
        }
    }

    @Test
    fun `test Play score tracking`() {
        val play = Play()

        play.homeScore = 0
        play.awayScore = 0
        assertEquals(0, play.homeScore)
        assertEquals(0, play.awayScore)

        play.homeScore = 7
        play.awayScore = 3
        assertEquals(7, play.homeScore)
        assertEquals(3, play.awayScore)

        play.homeScore = 28
        play.awayScore = 21
        assertEquals(28, play.homeScore)
        assertEquals(21, play.awayScore)
    }

    @Test
    fun `test Play game situation tracking`() {
        val play = Play()

        // Test quarter tracking
        play.quarter = 1
        assertEquals(1, play.quarter)

        play.quarter = 4
        assertEquals(4, play.quarter)

        // Test clock tracking
        play.clock = 900
        assertEquals(900, play.clock)

        play.clock = 0
        assertEquals(0, play.clock)

        // Test ball location
        play.ballLocation = 20
        assertEquals(20, play.ballLocation)

        play.ballLocation = 80
        assertEquals(80, play.ballLocation)

        // Test down and distance
        play.down = 1
        play.yardsToGo = 10
        assertEquals(1, play.down)
        assertEquals(10, play.yardsToGo)

        play.down = 4
        play.yardsToGo = 2
        assertEquals(4, play.down)
        assertEquals(2, play.yardsToGo)
    }

    @Test
    fun `test Play timeout management`() {
        val play = Play()

        // Test timeout flags
        play.timeoutUsed = true
        assertTrue(play.timeoutUsed)

        play.timeoutUsed = false
        assertFalse(play.timeoutUsed)

        play.offensiveTimeoutCalled = true
        assertTrue(play.offensiveTimeoutCalled)

        play.offensiveTimeoutCalled = false
        assertFalse(play.offensiveTimeoutCalled)

        play.defensiveTimeoutCalled = true
        assertTrue(play.defensiveTimeoutCalled)

        play.defensiveTimeoutCalled = false
        assertFalse(play.defensiveTimeoutCalled)

        // Test timeout counts
        play.homeTimeouts = 3
        play.awayTimeouts = 3
        assertEquals(3, play.homeTimeouts)
        assertEquals(3, play.awayTimeouts)

        play.homeTimeouts = 0
        play.awayTimeouts = 1
        assertEquals(0, play.homeTimeouts)
        assertEquals(1, play.awayTimeouts)
    }

    @Test
    fun `test Play win probability tracking`() {
        val play = Play()

        play.winProbability = 50.0
        assertEquals(50.0, play.winProbability)

        play.winProbability = 75.5
        assertEquals(75.5, play.winProbability)

        play.winProbability = 25.0
        assertEquals(25.0, play.winProbability)

        play.winProbability = null
        assertNull(play.winProbability)

        // Test win probability added
        play.winProbabilityAdded = 2.5
        assertEquals(2.5, play.winProbabilityAdded)

        play.winProbabilityAdded = -1.8
        assertEquals(-1.8, play.winProbabilityAdded)

        play.winProbabilityAdded = null
        assertNull(play.winProbabilityAdded)
    }

    @Test
    fun `test Play response speed tracking`() {
        val play = Play()

        play.offensiveResponseSpeed = 15000L
        assertEquals(15000L, play.offensiveResponseSpeed)

        play.offensiveResponseSpeed = 25000L
        assertEquals(25000L, play.offensiveResponseSpeed)

        play.offensiveResponseSpeed = null
        assertNull(play.offensiveResponseSpeed)

        play.defensiveResponseSpeed = 12000L
        assertEquals(12000L, play.defensiveResponseSpeed)

        play.defensiveResponseSpeed = 18000L
        assertEquals(18000L, play.defensiveResponseSpeed)

        play.defensiveResponseSpeed = null
        assertNull(play.defensiveResponseSpeed)
    }

    @Test
    fun `test Play time tracking`() {
        val play = Play()

        play.playTime = 25
        assertEquals(25, play.playTime)

        play.playTime = 45
        assertEquals(45, play.playTime)

        play.runoffTime = 5
        assertEquals(5, play.runoffTime)

        play.runoffTime = 15
        assertEquals(15, play.runoffTime)
    }

    @Test
    fun `test Play yards tracking`() {
        val play = Play()

        play.yards = 0
        assertEquals(0, play.yards)

        play.yards = 5
        assertEquals(5, play.yards)

        play.yards = -2
        assertEquals(-2, play.yards)

        play.yards = 25
        assertEquals(25, play.yards)
    }

    @Test
    fun `test Play completion status`() {
        val play = Play()

        play.playFinished = false
        assertFalse(play.playFinished)

        play.playFinished = true
        assertTrue(play.playFinished)

        play.playFinished = false
        assertFalse(play.playFinished)
    }

    @Test
    fun `test Play team information`() {
        val play = Play()

        play.homeTeam = "Alabama"
        play.awayTeam = "Georgia"

        assertEquals("Alabama", play.homeTeam)
        assertEquals("Georgia", play.awayTeam)

        play.homeTeam = "Michigan"
        play.awayTeam = "Ohio State"

        assertEquals("Michigan", play.homeTeam)
        assertEquals("Ohio State", play.awayTeam)
    }

    @Test
    fun `test Play difference tracking`() {
        val play = Play()

        play.difference = 0
        assertEquals(0, play.difference)

        play.difference = 5
        assertEquals(5, play.difference)

        play.difference = -3
        assertEquals(-3, play.difference)

        play.difference = null
        assertNull(play.difference)
    }

    @Test
    fun `test Play complete game scenario`() {
        val play = Play()

        // Set up a complete play scenario
        play.gameId = 123
        play.playNumber = 15
        play.homeScore = 21
        play.awayScore = 14
        play.quarter = 3
        play.clock = 1200
        play.ballLocation = 45
        play.possession = TeamSide.HOME
        play.down = 3
        play.yardsToGo = 8
        play.defensiveNumber = "4-3"
        play.offensiveNumber = "11"
        play.offensiveSubmitter = "home_coach"
        play.defensiveSubmitter = "away_coach"
        play.playCall = PlayCall.PASS
        play.result = Scenario.GAIN_OF_12_YARDS
        play.actualResult = ActualResult.FIRST_DOWN
        play.yards = 12
        play.playTime = 25
        play.runoffTime = 5
        play.winProbability = 65.5
        play.winProbabilityAdded = 2.3
        play.homeTeam = "Alabama"
        play.awayTeam = "Georgia"
        play.difference = 4
        play.timeoutUsed = false
        play.offensiveTimeoutCalled = false
        play.defensiveTimeoutCalled = false
        play.homeTimeouts = 3
        play.awayTimeouts = 2
        play.playFinished = true
        play.offensiveResponseSpeed = 15000L
        play.defensiveResponseSpeed = 12000L

        // Verify all properties
        assertEquals(123, play.gameId)
        assertEquals(15, play.playNumber)
        assertEquals(21, play.homeScore)
        assertEquals(14, play.awayScore)
        assertEquals(3, play.quarter)
        assertEquals(1200, play.clock)
        assertEquals(45, play.ballLocation)
        assertEquals(TeamSide.HOME, play.possession)
        assertEquals(3, play.down)
        assertEquals(8, play.yardsToGo)
        assertEquals("4-3", play.defensiveNumber)
        assertEquals("11", play.offensiveNumber)
        assertEquals("home_coach", play.offensiveSubmitter)
        assertEquals("away_coach", play.defensiveSubmitter)
        assertEquals(PlayCall.PASS, play.playCall)
        assertEquals(Scenario.GAIN_OF_12_YARDS, play.result)
        assertEquals(ActualResult.FIRST_DOWN, play.actualResult)
        assertEquals(12, play.yards)
        assertEquals(25, play.playTime)
        assertEquals(5, play.runoffTime)
        assertEquals(65.5, play.winProbability)
        assertEquals(2.3, play.winProbabilityAdded)
        assertEquals("Alabama", play.homeTeam)
        assertEquals("Georgia", play.awayTeam)
        assertEquals(4, play.difference)
        assertFalse(play.timeoutUsed)
        assertFalse(play.offensiveTimeoutCalled)
        assertFalse(play.defensiveTimeoutCalled)
        assertEquals(3, play.homeTimeouts)
        assertEquals(2, play.awayTimeouts)
        assertTrue(play.playFinished)
        assertEquals(15000L, play.offensiveResponseSpeed)
        assertEquals(12000L, play.defensiveResponseSpeed)
    }
}
