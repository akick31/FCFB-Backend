package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.exception.game.InvalidGameReopenException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.statistics.elo.EloService
import com.fcfb.arceus.service.fcfb.statistics.season.PostseasonSeasonStatsService
import com.fcfb.arceus.service.fcfb.statistics.season.SeasonStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameReopenServiceTest {
    private val gameRepository: GameRepository = mockk(relaxed = true)
    private val playRepository: PlayRepository = mockk(relaxed = true)
    private val teamService: TeamService = mockk(relaxed = true)
    private val userService: UserService = mockk(relaxed = true)
    private val scheduleService: ScheduleService = mockk(relaxed = true)
    private val eloService: EloService = mockk(relaxed = true)
    private val service =
        GameReopenService(
            gameRepository,
            playRepository,
            mockk(relaxed = true),
            teamService,
            userService,
            scheduleService,
            mockk<GameStatsService>(relaxed = true),
            mockk<GameStatsRepository>(relaxed = true),
            mockk<SeasonStatsService>(relaxed = true),
            mockk<PostseasonSeasonStatsService>(relaxed = true),
            eloService,
        )

    @Test
    fun `reopening a game that ended on a final touchdown restores the scorer's pending conversion`() {
        val game = finalGame(homeScore = 24, awayScore = 22)
        val touchdown =
            Play().apply {
                homeTeam = "Ohio State"
                awayTeam = "Michigan"
                possession = TeamSide.AWAY
                actualResult = ActualResult.TOUCHDOWN
                playId = TOUCHDOWN_PLAY_ID
                playFinished = true
                homeScore = 22
                awayScore = 24
            }
        val endOfGame = endOfGamePlay()
        every { gameRepository.getGameById(GAME_ID) } returns game
        every { playRepository.getAllPlaysByGameId(GAME_ID) } returns listOf(touchdown, endOfGame)
        every { playRepository.getPreviousPlay(GAME_ID) } returns endOfGame
        every { gameRepository.save(any<Game>()) } answers { firstArg() }

        val reopened = service.reopenGame(GAME_ID)

        assertEquals(GameStatus.IN_PROGRESS, reopened.gameStatus)
        assertEquals(TeamSide.AWAY, reopened.possession)
        assertEquals(TeamSide.HOME, reopened.waitingOn)
        assertEquals(PlayType.PAT, reopened.currentPlayType)
        assertEquals(22, reopened.homeScore)
        assertEquals(24, reopened.awayScore)
        verify(exactly = 1) { playRepository.deleteById(END_OF_GAME_PLAY_ID) }
        verify(exactly = 1) { teamService.revertTeamWinsAndLosses(game) }
        verify(exactly = 1) { userService.revertUserWinsAndLosses(game) }
        verify(exactly = 1) { scheduleService.markGameAsUnfinished(game) }
        verify(exactly = 1) { eloService.rebuild(true) }
    }

    @Test
    fun `a game whose last real play was not a touchdown cannot be reopened and nothing is reverted`() {
        val game = finalGame(homeScore = 24, awayScore = 22)
        val punt =
            Play().apply {
                homeTeam = "Ohio State"
                awayTeam = "Michigan"
                playId = TOUCHDOWN_PLAY_ID
                playFinished = true
                actualResult = ActualResult.PUNT
            }
        every { gameRepository.getGameById(GAME_ID) } returns game
        every { playRepository.getAllPlaysByGameId(GAME_ID) } returns listOf(punt, endOfGamePlay())

        assertFailsWith<InvalidGameReopenException> { service.reopenGame(GAME_ID) }

        verify(exactly = 0) { teamService.revertTeamWinsAndLosses(any()) }
        verify(exactly = 0) { playRepository.deleteById(any()) }
        assertEquals(GameStatus.FINAL, game.gameStatus)
    }

    @Test
    fun `a game that is not final cannot be reopened`() {
        val game = finalGame(homeScore = 24, awayScore = 22).apply { gameStatus = GameStatus.IN_PROGRESS }
        every { gameRepository.getGameById(GAME_ID) } returns game

        assertFailsWith<InvalidGameReopenException> { service.reopenGame(GAME_ID) }
    }

    @Test
    fun `a national championship cannot be reopened`() {
        val game = finalGame(homeScore = 24, awayScore = 22).apply { gameType = GameType.NATIONAL_CHAMPIONSHIP }
        every { gameRepository.getGameById(GAME_ID) } returns game

        assertFailsWith<InvalidGameReopenException> { service.reopenGame(GAME_ID) }
    }

    private fun endOfGamePlay() =
        Play().apply {
            playId = END_OF_GAME_PLAY_ID
            playFinished = true
            homeTeam = "Ohio State"
            awayTeam = "Michigan"
            actualResult = ActualResult.END_OF_GAME
        }

    private fun finalGame(
        homeScore: Int,
        awayScore: Int,
    ) = Game().apply {
        gameId = GAME_ID
        homeTeam = "Ohio State"
        awayTeam = "Michigan"
        gameType = GameType.OUT_OF_CONFERENCE
        gameStatus = GameStatus.FINAL
        quarter = 4
        this.homeScore = homeScore
        this.awayScore = awayScore
        possession = TeamSide.AWAY
    }

    companion object {
        private const val GAME_ID = 2211
        private const val END_OF_GAME_PLAY_ID = 9002
        private const val TOUCHDOWN_PLAY_ID = 9001
    }
}
