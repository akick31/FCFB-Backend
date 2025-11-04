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
        // Given
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

        // When
        val savedPlay = playRepository.save(play)
        val foundPlay = playRepository.findById(savedPlay.playId).get()

        // Then
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
        // Given
        val play =
            createTestPlay(
                playId = 2,
                gameId = 456,
                offensiveSubmitter = "test_offensive#1234",
                defensiveSubmitter = "test_defensive#5678",
            )

        every { playRepository.getPlayById(2) } returns play

        // When
        val foundPlay = playRepository.getPlayById(2)

        // Then
        assertNotNull(foundPlay)
        assertEquals(2, foundPlay.playId)
        assertEquals(456, foundPlay.gameId)
        assertEquals("test_offensive#1234", foundPlay.offensiveSubmitter)
        assertEquals("test_defensive#5678", foundPlay.defensiveSubmitter)
    }

    @Test
    fun `test getPlayById returns null when not found`() {
        // Given
        every { playRepository.getPlayById(999) } returns null

        // When
        val foundPlay = playRepository.getPlayById(999)

        // Then
        assertNull(foundPlay)
    }

    @Test
    fun `test getAllPlaysByGameId`() {
        // Given
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

        // When
        val foundPlays = playRepository.getAllPlaysByGameId(100)

        // Then
        assertEquals(2, foundPlays.size)
        assertEquals(1, foundPlays[0].playId)
        assertEquals(2, foundPlays[1].playId)
    }

    @Test
    fun `test getCurrentPlay`() {
        // Given
        val play =
            createTestPlay(
                playId = 1,
                gameId = 100,
                playFinished = false,
            )

        every { playRepository.getCurrentPlay(100) } returns play

        // When
        val foundPlay = playRepository.getCurrentPlay(100)

        // Then
        assertNotNull(foundPlay)
        assertEquals(1, foundPlay.playId)
        assertFalse(foundPlay.playFinished)
    }

    @Test
    fun `test getCurrentPlay returns null when no unfinished plays`() {
        // Given
        every { playRepository.getCurrentPlay(100) } returns null

        // When
        val foundPlay = playRepository.getCurrentPlay(100)

        // Then
        assertNull(foundPlay)
    }

    @Test
    fun `test getPreviousPlay`() {
        // Given
        val play =
            createTestPlay(
                playId = 1,
                gameId = 100,
                playFinished = true,
            )

        every { playRepository.getPreviousPlay(100) } returns play

        // When
        val foundPlay = playRepository.getPreviousPlay(100)

        // Then
        assertNotNull(foundPlay)
        assertEquals(1, foundPlay.playId)
        assertTrue(foundPlay.playFinished)
    }

    @Test
    fun `test getPreviousPlay returns null when no finished plays`() {
        // Given
        every { playRepository.getPreviousPlay(100) } returns null

        // When
        val foundPlay = playRepository.getPreviousPlay(100)

        // Then
        assertNull(foundPlay)
    }

    @Test
    fun `test getAllPlaysByDiscordTag`() {
        // Given
        val play1 = createTestPlay(playId = 1, offensiveSubmitter = "coach#1234")
        val play2 = createTestPlay(playId = 2, defensiveSubmitter = "coach#1234")
        val plays = listOf(play1, play2)

        every { playRepository.getAllPlaysByDiscordTag("coach#1234") } returns plays

        // When
        val foundPlays = playRepository.getAllPlaysByDiscordTag("coach#1234")

        // Then
        assertEquals(2, foundPlays.size)
        assertTrue(foundPlays.any { it.offensiveSubmitter == "coach#1234" })
        assertTrue(foundPlays.any { it.defensiveSubmitter == "coach#1234" })
    }

    @Test
    fun `test getUserAverageResponseTime`() {
        // Given
        every { playRepository.getUserAverageResponseTime("coach#1234", 2024) } returns 15.5

        // When
        val avgResponseTime = playRepository.getUserAverageResponseTime("coach#1234", 2024)

        // Then
        assertEquals(15.5, avgResponseTime)
    }

    @Test
    fun `test getHomeDelayOfGameInstances`() {
        // Given
        every { playRepository.getHomeDelayOfGameInstances(100) } returns 2

        // When
        val delayInstances = playRepository.getHomeDelayOfGameInstances(100)

        // Then
        assertEquals(2, delayInstances)
    }

    @Test
    fun `test getAwayDelayOfGameInstances`() {
        // Given
        every { playRepository.getAwayDelayOfGameInstances(100) } returns 1

        // When
        val delayInstances = playRepository.getAwayDelayOfGameInstances(100)

        // Then
        assertEquals(1, delayInstances)
    }

    @Test
    fun `test deleteAllPlaysByGameId`() {
        // Given
        every { playRepository.deleteAllPlaysByGameId(100) } returns Unit

        // When
        playRepository.deleteAllPlaysByGameId(100)

        // Then
        verify { playRepository.deleteAllPlaysByGameId(100) }
    }

    @Test
    fun `test findAll`() {
        // Given
        val play1 = createTestPlay(playId = 1)
        val play2 = createTestPlay(playId = 2)
        val allPlays = listOf(play1, play2)

        every { playRepository.findAll() } returns allPlays

        // When
        val foundPlays = playRepository.findAll()

        // Then
        assertEquals(2, foundPlays.count())
        assertTrue(foundPlays.any { it.playId == 1 })
        assertTrue(foundPlays.any { it.playId == 2 })
    }

    @Test
    fun `test count`() {
        // Given
        every { playRepository.count() } returns 50L

        // When
        val count = playRepository.count()

        // Then
        assertEquals(50L, count)
    }

    @Test
    fun `test delete`() {
        // Given
        val play = createTestPlay(playId = 1)
        every { playRepository.delete(play) } returns Unit

        // When
        playRepository.delete(play)

        // Then
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
