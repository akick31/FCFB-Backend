package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayRepositoryTest {
    private lateinit var playRepository: PlayRepository

    @BeforeEach
    fun setUp() {
        playRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and findById`() {
        val play =
            createTestPlay(
                playId = 1,
                gameId = 123,
                offensiveSubmitter = "offensive_coach#1234",
                defensiveSubmitter = "defensive_coach#5678",
                playCall = PlayCall.RUN,
                actualResult = ActualResult.GAIN,
                playFinished = true,
            )

        every { playRepository.save(any()) } returns play
        every { playRepository.findById(1) } returns java.util.Optional.of(play)

        val savedPlay = playRepository.save(play)
        val foundPlay = playRepository.findById(savedPlay.playId).get()

        assertNotNull(foundPlay)
        assertEquals(1, foundPlay.playId)
        assertEquals(123, foundPlay.gameId)
        assertEquals("offensive_coach#1234", foundPlay.offensiveSubmitter)
        assertEquals("defensive_coach#5678", foundPlay.defensiveSubmitter)
        assertEquals(PlayCall.RUN, foundPlay.playCall)
        assertEquals(ActualResult.GAIN, foundPlay.actualResult)
        assertTrue(foundPlay.playFinished)
    }

    @Test
    fun `test getPlayById`() {
        val play =
            createTestPlay(
                playId = 2,
                gameId = 456,
                offensiveSubmitter = "test_offensive#1234",
                defensiveSubmitter = "test_defensive#5678",
            )

        every { playRepository.getPlayById(2) } returns play

        val foundPlay = playRepository.getPlayById(2)

        assertNotNull(foundPlay)
        assertEquals(2, foundPlay.playId)
        assertEquals(456, foundPlay.gameId)
        assertEquals("test_offensive#1234", foundPlay.offensiveSubmitter)
        assertEquals("test_defensive#5678", foundPlay.defensiveSubmitter)
    }

    @Test
    fun `test getPlayById returns null when not found`() {
        every { playRepository.getPlayById(999) } returns null

        val foundPlay = playRepository.getPlayById(999)

        assertNull(foundPlay)
    }

    @Test
    fun `test getAllPlaysByGameId`() {
        val play1 =
            createTestPlay(
                playId = 1,
                gameId = 100,
                offensiveSubmitter = "coach1#1234",
                defensiveSubmitter = "coach2#5678",
            )
        val play2 =
            createTestPlay(
                playId = 2,
                gameId = 100,
                offensiveSubmitter = "coach3#1234",
                defensiveSubmitter = "coach4#5678",
            )
        val plays = listOf(play1, play2)

        every { playRepository.getAllPlaysByGameId(100) } returns plays

        val foundPlays = playRepository.getAllPlaysByGameId(100)

        assertEquals(2, foundPlays.size)
        assertEquals(1, foundPlays[0].playId)
        assertEquals(2, foundPlays[1].playId)
    }

    @Test
    fun `test getCurrentPlay`() {
        val play =
            createTestPlay(
                playId = 1,
                gameId = 100,
                playFinished = false,
            )

        every { playRepository.getCurrentPlay(100) } returns play

        val foundPlay = playRepository.getCurrentPlay(100)

        assertNotNull(foundPlay)
        assertEquals(1, foundPlay.playId)
        assertFalse(foundPlay.playFinished)
    }

    @Test
    fun `test getCurrentPlay returns null when no unfinished plays`() {
        every { playRepository.getCurrentPlay(100) } returns null

        val foundPlay = playRepository.getCurrentPlay(100)

        assertNull(foundPlay)
    }

    @Test
    fun `test getPreviousPlay`() {
        val play =
            createTestPlay(
                playId = 1,
                gameId = 100,
                playFinished = true,
            )

        every { playRepository.getPreviousPlay(100) } returns play

        val foundPlay = playRepository.getPreviousPlay(100)

        assertNotNull(foundPlay)
        assertEquals(1, foundPlay.playId)
        assertTrue(foundPlay.playFinished)
    }

    @Test
    fun `test getPreviousPlay returns null when no finished plays`() {
        every { playRepository.getPreviousPlay(100) } returns null

        val foundPlay = playRepository.getPreviousPlay(100)

        assertNull(foundPlay)
    }

    @Test
    fun `test getAllPlaysByDiscordId`() {
        val play1 = createTestPlay(playId = 1, offensiveSubmitter = "coach#1234")
        val play2 = createTestPlay(playId = 2, defensiveSubmitter = "coach#1234")
        val plays = listOf(play1, play2)

        every { playRepository.getAllPlaysByDiscordId("111222333") } returns plays

        val foundPlays = playRepository.getAllPlaysByDiscordId("111222333")

        assertEquals(2, foundPlays.size)
        assertTrue(foundPlays.any { it.offensiveSubmitter == "coach#1234" })
        assertTrue(foundPlays.any { it.defensiveSubmitter == "coach#1234" })
    }

    @Test
    fun `test getUserAverageResponseTime`() {
        every { playRepository.getUserAverageResponseTime("coach#1234", 2024) } returns 15.5

        val avgResponseTime = playRepository.getUserAverageResponseTime("coach#1234", 2024)

        assertEquals(15.5, avgResponseTime)
    }

    @Test
    fun `test getHomeDelayOfGameInstances`() {
        every { playRepository.getHomeDelayOfGameInstances(100) } returns 2

        val delayInstances = playRepository.getHomeDelayOfGameInstances(100)

        assertEquals(2, delayInstances)
    }

    @Test
    fun `test getAwayDelayOfGameInstances`() {
        every { playRepository.getAwayDelayOfGameInstances(100) } returns 1

        val delayInstances = playRepository.getAwayDelayOfGameInstances(100)

        assertEquals(1, delayInstances)
    }

    @Test
    fun `test deleteAllPlaysByGameId`() {
        every { playRepository.deleteAllPlaysByGameId(100) } returns Unit

        playRepository.deleteAllPlaysByGameId(100)

        verify { playRepository.deleteAllPlaysByGameId(100) }
    }

    @Test
    fun `test findAll`() {
        val play1 = createTestPlay(playId = 1)
        val play2 = createTestPlay(playId = 2)
        val allPlays = listOf(play1, play2)

        every { playRepository.findAll() } returns allPlays

        val foundPlays = playRepository.findAll()

        assertEquals(2, foundPlays.count())
        assertTrue(foundPlays.any { it.playId == 1 })
        assertTrue(foundPlays.any { it.playId == 2 })
    }

    @Test
    fun `test count`() {
        every { playRepository.count() } returns 50L

        val count = playRepository.count()

        assertEquals(50L, count)
    }

    @Test
    fun `test delete`() {
        val play = createTestPlay(playId = 1)
        every { playRepository.delete(play) } returns Unit

        playRepository.delete(play)

        verify { playRepository.delete(play) }
    }

    private fun createTestPlay(
        playId: Int = 1,
        gameId: Int = 100,
        offensiveSubmitter: String = "offensive#1234",
        defensiveSubmitter: String = "defensive#5678",
        playCall: PlayCall = PlayCall.RUN,
        actualResult: ActualResult = ActualResult.GAIN,
        playFinished: Boolean = true,
    ): Play {
        return Play(
            gameId = gameId,
            playNumber = 1,
            homeScore = 0,
            awayScore = 0,
            quarter = 1,
            clock = 420,
            ballLocation = 20,
            possession = TeamSide.HOME,
            down = 1,
            yardsToGo = 10,
            defensiveNumber = "1",
            offensiveNumber = "1",
            offensiveSubmitter = offensiveSubmitter,
            offensiveSubmitterId = "${offensiveSubmitter}_id",
            defensiveSubmitter = defensiveSubmitter,
            defensiveSubmitterId = "${defensiveSubmitter}_id",
            playCall = playCall,
            result = null,
            actualResult = actualResult,
            yards = 5,
            playTime = 30,
            runoffTime = 0,
            winProbability = 0.5,
            winProbabilityAdded = 0.0,
            homeTeam = "Alabama",
            awayTeam = "Auburn",
            difference = 0,
            timeoutUsed = false,
            offensiveTimeoutCalled = false,
            defensiveTimeoutCalled = false,
            homeTimeouts = 3,
            awayTimeouts = 3,
            playFinished = playFinished,
            offensiveResponseSpeed = 5000L,
            defensiveResponseSpeed = 5000L,
        ).apply { this.playId = playId }
    }
}
