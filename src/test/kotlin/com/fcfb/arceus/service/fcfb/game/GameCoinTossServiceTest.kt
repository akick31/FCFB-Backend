package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.gameflow.CoinTossCall
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.exception.game.InvalidCoinTossChoiceException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.game.GameCoinToss
import com.fcfb.arceus.repositories.game.GameCoinTossRepository
import com.fcfb.arceus.repositories.game.GameRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Random

class GameCoinTossServiceTest {
    private val repository = mockk<GameRepository>()
    private val gameCoinTossRepository = mockk<GameCoinTossRepository>(relaxed = true)
    private val gameService =
        GameService(
            repository,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    private val service = GameCoinTossService(gameService, gameCoinTossRepository)
    private val game =
        Game().apply {
            gameId = 7
            gameStatus = GameStatus.PREGAME
            coinTossWinner = TeamSide.HOME
            overtimeCoinTossWinner = TeamSide.AWAY
            possession = TeamSide.HOME
            waitingOn = TeamSide.AWAY
            gameTimer = "existing timer"
        }

    @BeforeEach
    fun setUp() {
        every { repository.getGameById(7) } returns game
        every { repository.save(game) } returns game
        every { gameCoinTossRepository.save(any()) } answers { firstArg() }
        mockkConstructor(Random::class)
        every { anyConstructed<Random>().nextInt(2) } returns 1
    }

    @AfterEach
    fun releaseRandomMock() {
        unmockkConstructor(Random::class)
    }

    @ParameterizedTest
    @CsvSource(
        "PREGAME,HEADS,1,AWAY",
        "PREGAME,HEADS,0,HOME",
        "PREGAME,TAILS,1,HOME",
        "PREGAME,TAILS,0,AWAY",
        "END_OF_REGULATION,HEADS,1,AWAY",
        "END_OF_REGULATION,HEADS,0,HOME",
        "END_OF_REGULATION,TAILS,1,HOME",
        "END_OF_REGULATION,TAILS,0,AWAY",
    )
    fun `toss retains call interpretation and changes only the applicable winner`(
        status: GameStatus,
        call: CoinTossCall,
        flip: Int,
        winner: TeamSide,
    ) {
        game.gameStatus = status
        every { anyConstructed<Random>().nextInt(2) } returns flip
        assertSame(game, service.runCoinToss(7, call))
        assertEquals(if (status == GameStatus.PREGAME) winner else TeamSide.HOME, game.coinTossWinner)
        assertEquals(if (status == GameStatus.END_OF_REGULATION) winner else TeamSide.AWAY, game.overtimeCoinTossWinner)
        assertEquals(winner, game.waitingOn)
        assertEquals(TeamSide.HOME, game.possession)
        assertEquals(status, game.gameStatus)
        assertNotEquals("existing timer", game.gameTimer)
        verify(exactly = 1) { repository.save(game) }
        val captured = slot<GameCoinToss>()
        verify(exactly = 1) { gameCoinTossRepository.save(capture(captured)) }
        assertEquals(7, captured.captured.gameId)
        assertEquals(game.coinTossWinner, captured.captured.coinTossWinner)
        assertEquals(game.overtimeCoinTossWinner, captured.captured.overtimeCoinTossWinner)
    }

    @ParameterizedTest
    @CsvSource("HOME,RECEIVE,AWAY,HOME", "HOME,DEFER,HOME,AWAY", "AWAY,RECEIVE,HOME,AWAY", "AWAY,DEFER,AWAY,HOME")
    fun `regulation choice retains kickoff possession waiting side and timer`(
        winner: TeamSide,
        choice: CoinTossChoice,
        possession: TeamSide,
        waitingOn: TeamSide,
    ) {
        game.coinTossWinner = winner
        assertSame(game, service.makeCoinTossChoice(7, choice))
        assertEquals(choice, game.coinTossChoice)
        assertEquals(possession, game.possession)
        assertEquals(waitingOn, game.waitingOn)
        assertEquals(GameStatus.OPENING_KICKOFF, game.gameStatus)
        assertNotEquals("existing timer", game.gameTimer)
        verify(exactly = 1) { repository.save(game) }
    }

    @ParameterizedTest
    @CsvSource("HOME,DEFENSE,AWAY,HOME", "HOME,OFFENSE,HOME,AWAY", "AWAY,DEFENSE,HOME,AWAY", "AWAY,OFFENSE,AWAY,HOME")
    fun `overtime choice retains possession waiting side and existing timer`(
        winner: TeamSide,
        choice: OvertimeCoinTossChoice,
        possession: TeamSide,
        waitingOn: TeamSide,
    ) {
        game.overtimeCoinTossWinner = winner
        assertSame(game, service.makeOvertimeCoinTossChoice(7, choice))
        assertEquals(choice, game.overtimeCoinTossChoice)
        assertEquals(possession, game.possession)
        assertEquals(waitingOn, game.waitingOn)
        assertEquals(GameStatus.OVERTIME, game.gameStatus)
        assertEquals("existing timer", game.gameTimer)
        verify(exactly = 1) { repository.save(game) }
    }

    @Test
    fun `toss in another status refreshes timer without changing winners`() {
        game.gameStatus = GameStatus.IN_PROGRESS
        service.runCoinToss(7, CoinTossCall.HEADS)
        assertEquals(TeamSide.HOME, game.coinTossWinner)
        assertEquals(TeamSide.AWAY, game.overtimeCoinTossWinner)
        assertEquals(TeamSide.AWAY, game.waitingOn)
        assertEquals(GameStatus.IN_PROGRESS, game.gameStatus)
        assertNotEquals("existing timer", game.gameTimer)
        verify(exactly = 1) { repository.save(game) }
    }

    @Test
    fun `absent winners retain choice behavior without new validation`() {
        game.coinTossWinner = null
        game.overtimeCoinTossWinner = null
        service.makeCoinTossChoice(7, CoinTossChoice.RECEIVE)
        assertEquals(TeamSide.HOME, game.possession)
        assertEquals(TeamSide.AWAY, game.waitingOn)
        assertEquals(GameStatus.OPENING_KICKOFF, game.gameStatus)
        service.makeOvertimeCoinTossChoice(7, OvertimeCoinTossChoice.DEFENSE)
        assertEquals(TeamSide.HOME, game.possession)
        assertEquals(TeamSide.AWAY, game.waitingOn)
        assertEquals(GameStatus.OVERTIME, game.gameStatus)
    }

    @Test
    fun `choice save argument errors retain distinct domain messages`() {
        every { repository.save(game) } throws IllegalArgumentException("Save failed")
        assertEquals(
            "Invalid coin toss choice: RECEIVE",
            assertThrows(InvalidCoinTossChoiceException::class.java) { service.makeCoinTossChoice(7, CoinTossChoice.RECEIVE) }.message,
        )
        assertEquals(
            "Invalid coin toss choice DEFENSE",
            assertThrows(InvalidCoinTossChoiceException::class.java) {
                service.makeOvertimeCoinTossChoice(7, OvertimeCoinTossChoice.DEFENSE)
            }.message,
        )
    }

    @Test
    fun `lookup failures stay outside choice exception translation`() {
        val failure = IllegalArgumentException("Lookup failed")
        every { repository.getGameById(7) } throws failure
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) { service.runCoinToss(7, CoinTossCall.HEADS) })
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) { service.makeCoinTossChoice(7, CoinTossChoice.RECEIVE) })
        assertSame(
            failure,
            assertThrows(IllegalArgumentException::class.java) {
                service.makeOvertimeCoinTossChoice(7, OvertimeCoinTossChoice.DEFENSE)
            },
        )
        verify(exactly = 0) { repository.save(any<Game>()) }
    }

    @Test
    fun `ordinary save failures propagate from all operations`() {
        val failure = IllegalStateException("Database unavailable")
        every { repository.save(game) } throws failure
        assertSame(failure, assertThrows(IllegalStateException::class.java) { service.runCoinToss(7, CoinTossCall.HEADS) })
        assertSame(failure, assertThrows(IllegalStateException::class.java) { service.makeCoinTossChoice(7, CoinTossChoice.RECEIVE) })
        assertSame(
            failure,
            assertThrows(IllegalStateException::class.java) {
                service.makeOvertimeCoinTossChoice(7, OvertimeCoinTossChoice.DEFENSE)
            },
        )
    }

    @Test
    fun `toss returns loaded game while choices return the save result`() {
        val saved = Game().apply { gameId = 99 }
        every { repository.save(game) } returns saved
        assertSame(game, service.runCoinToss(7, CoinTossCall.HEADS))
        assertSame(saved, service.makeCoinTossChoice(7, CoinTossChoice.RECEIVE))
        assertSame(saved, service.makeOvertimeCoinTossChoice(7, OvertimeCoinTossChoice.DEFENSE))
    }
}
