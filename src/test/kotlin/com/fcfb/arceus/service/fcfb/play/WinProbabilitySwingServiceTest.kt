package com.fcfb.arceus.service.fcfb.play

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WinProbabilitySwingServiceTest {
    private val playRepository = mockk<PlayRepository>()
    private val gameRepository = mockk<GameRepository>()
    private val service = WinProbabilitySwingService(playRepository, gameRepository)

    private fun stub(
        possession: TeamSide,
        added: Double,
    ) {
        every { playRepository.findBiggestSwingInWeek(12, 2) } returns
            Play().apply {
                gameId = 7
                playId = 99
                this.possession = possession
                winProbabilityAdded = added
                quarter = 4
                clock = 30
                homeScore = 21
                awayScore = 27
                actualResult = ActualResult.TURNOVER_TOUCHDOWN
            }
        every { gameRepository.getGameById(7) } returns
            Game().apply {
                homeTeam = "Wyoming"
                awayTeam = "Idaho"
            }
    }

    @Test
    fun `a positive swing credits the offense, with the score after the play`() {
        stub(TeamSide.AWAY, 0.42)

        val swing = service.biggestSwing(12, 2)!!

        assertEquals("Idaho", swing.team)
        assertEquals("Wyoming", swing.opponent)
        assertEquals(0.42, swing.swing)
        assertEquals(21, swing.homeScore)
        assertEquals(27, swing.awayScore)
        assertEquals("TURNOVER_TOUCHDOWN", swing.result)
    }

    @Test
    fun `a negative swing credits the defense and is reported as a size`() {
        stub(TeamSide.AWAY, -0.35)

        val swing = service.biggestSwing(12, 2)!!

        assertEquals("Wyoming", swing.team)
        assertEquals(0.35, swing.swing)
    }

    @Test
    fun `a week with no scored plays has no swing`() {
        every { playRepository.findBiggestSwingInWeek(12, 2) } returns null

        assertNull(service.biggestSwing(12, 2))
    }
}
