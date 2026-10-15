package com.fcfb.arceus.service.fcfb.graphics

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.rendering.chart.EloChartRenderer
import com.fcfb.arceus.rendering.chart.ScoreChartRenderer
import com.fcfb.arceus.rendering.chart.WinProbabilityChartRenderer
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.probability.WinProbabilityOrchestrationService
import com.fcfb.arceus.service.fcfb.probability.WinProbabilityService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.ml.XGBoostPredictor
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import javax.imageio.ImageIO

class ChartServiceTest {
    @TempDir
    lateinit var imagePath: Path
    private val gameRepository = mockk<GameRepository>()
    private val playRepository = mockk<PlayRepository>()
    private val teamRepository = mockk<TeamRepository>()
    private val gameStatsRepository = mockk<GameStatsRepository>()
    private val teamService = mockk<TeamService>()
    private val gameService = mockk<GameService>()
    private val predictor = mockk<XGBoostPredictor>()
    private val probabilityService =
        spyk(
            WinProbabilityOrchestrationService(
                WinProbabilityService(predictor, playRepository),
                gameService,
                mockk(),
                teamService,
                mockk(),
            ),
        )
    private val scoreRenderer = spyk(ScoreChartRenderer())
    private val probabilityRenderer = spyk(WinProbabilityChartRenderer())
    private val eloRenderer = spyk(EloChartRenderer())
    private val homeTeam =
        Team().apply {
            name = "Wyoming"
            abbreviation = "WYO"
            primaryColor = "#FFC425"
        }
    private val awayTeam =
        Team().apply {
            name = "San Jose State"
            abbreviation = "SJS"
            primaryColor = "#0055A2"
        }
    private val game =
        Game().apply {
            gameId = 7
            homeTeam = "Wyoming"
            awayTeam = "San Jose State"
        }

    private fun service() =
        ChartService(
            scoreRenderer,
            probabilityRenderer,
            eloRenderer,
            gameService,
            gameRepository,
            playRepository,
            teamRepository,
            gameStatsRepository,
            teamService,
            probabilityService,
            imagePath.toString(),
        )

    private fun plays() =
        listOf(3, 1, 2).map { number ->
            Play().apply {
                gameId = 7
                playNumber = number
                quarter = number
                clock = 300
                homeScore = number * 7
                awayScore = 7
                possession = if (number == 2) TeamSide.AWAY else TeamSide.HOME
                winProbability = if (number == 2) 0.65 else 0.6
            }
        }

    private fun stubGame(plays: List<Play>) {
        every { gameRepository.getGameById(7) } returns game
        every { playRepository.getAllPlaysByGameId(7) } returns plays
        every { teamService.getTeamByName("Wyoming") } returns homeTeam
        every { teamService.getTeamByName("San Jose State") } returns awayTeam
    }

    private fun assertPng(
        bytes: ByteArray,
        filename: String,
        width: Int,
        height: Int,
    ) {
        assertArrayEquals(bytes, Files.readAllBytes(imagePath.resolve("charts/$filename.png")))
        val image = ImageIO.read(bytes.inputStream())
        assertEquals(width, image.width)
        assertEquals(height, image.height)
    }

    @Test
    fun `score response retains PNG headers storage and query order`() {
        stubGame(plays())
        val response = service().getScoreChart(7)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(MediaType.IMAGE_PNG, response.headers.contentType)
        assertEquals(response.body!!.size.toLong(), response.headers.contentLength)
        assertPng(response.body!!, "score_7", 1000, 510)
        verifyOrder {
            gameRepository.getGameById(7)
            playRepository.getAllPlaysByGameId(7)
            teamService.getTeamByName("Wyoming")
            teamService.getTeamByName("San Jose State")
        }
    }

    @ParameterizedTest
    @CsvSource("FINAL, 21, 7, 3, 1.0", "FINAL, 7, 21, 3, 0.0", "FINAL, 7, 7, 3, 0.6", "FINAL, 7, 0, 1, 0.6", "IN_PROGRESS, 21, 7, 3, 0.6")
    fun `probability chart retains stored probabilities and finished game output`(
        status: GameStatus,
        homeScore: Int,
        awayScore: Int,
        count: Int,
        expectedLineProbability: Double,
    ) {
        game.gameStatus = status
        val plays = plays().sortedBy { it.playNumber }.take(count)
        plays.last().homeScore = homeScore
        plays.last().awayScore = awayScore
        stubGame(
            plays.reversed() +
                Play().apply {
                    playNumber = 0
                    winProbability = null
                },
        )
        val response = service().getWinProbabilityChart(7)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(MediaType.IMAGE_PNG, response.headers.contentType)
        assertEquals(response.body!!.size.toLong(), response.headers.contentLength)
        assertPng(response.body!!, "win_prob_7", 1000, 510)
        verify(exactly = 1) { probabilityService.getWinProbabilitiesForGame(7, plays) }
        verify {
            probabilityRenderer.render(
                game,
                plays,
                homeTeam,
                awayTeam,
                match { it.last().homeTeamWinProbability == expectedLineProbability },
                match { it.last().homeTeamWinProbability == 0.6 },
            )
        }
        verify(exactly = 0) { predictor.predict(any()) }
        assertEquals(0.6, plays.last().winProbability)
    }

    @Test
    fun `missing game and empty plays retain server errors without team lookups`() {
        every { gameRepository.getGameById(7) } returns null
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, service().getScoreChart(7).statusCode)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, service().getWinProbabilityChart(7).statusCode)
        verify(exactly = 0) { playRepository.getAllPlaysByGameId(any()) }
        stubGame(emptyList())
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, service().getScoreChart(7).statusCode)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, service().getWinProbabilityChart(7).statusCode)
        verify(exactly = 0) { teamService.getTeamByName(any()) }
    }

    @Test
    fun `Elo output retains active team selection and later game overrides`() {
        every { teamRepository.getAllActiveTeams() } returns listOf(homeTeam, awayTeam)
        every { gameStatsRepository.findBySeasonOrderByGameIdAsc(12) } returns
            listOf(
                GameStats(team = "Wyoming", week = 1, teamElo = 1400.0),
                GameStats(team = "Wyoming", week = 1, teamElo = 1500.0),
                GameStats(team = "Wyoming", week = 2, teamElo = 1600.0),
                GameStats(team = "San Jose State", week = 1, teamElo = 1550.0),
                GameStats(team = "San Jose State", week = 2, teamElo = 1450.0),
                GameStats(team = "Inactive", week = 3, teamElo = 2500.0),
                GameStats(team = null, week = 2),
                GameStats(team = "Wyoming", week = null),
            )
        val bytes = service().getEloChart(12)!!
        verify {
            eloRenderer.render(
                mapOf("Wyoming" to mapOf(1 to 1500.0, 2 to 1600.0), "San Jose State" to mapOf(1 to 1550.0, 2 to 1450.0)),
                mapOf("Wyoming" to ("#FFC425" to null), "San Jose State" to ("#0055A2" to null)),
                12,
            )
        }
        assertPng(bytes, "elo_chart_season_12", 1200, 800)
        verifyOrder {
            teamRepository.getAllActiveTeams()
            gameStatsRepository.findBySeasonOrderByGameIdAsc(12)
        }
    }

    @Test
    fun `Elo empty active teams short circuits game stats`() {
        every { teamRepository.getAllActiveTeams() } returns emptyList()
        assertNull(service().getEloChart(12))
        verify(exactly = 0) { gameStatsRepository.findBySeasonOrderByGameIdAsc(any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `matchup results keep partial successes and existing error statuses`(probability: Boolean) {
        stubGame(plays())
        val missing = Game().apply { gameId = 8 }
        every { gameRepository.getGameById(8) } returns null
        every { gameService.getGameBySeasonAndMatchup(12, "Wyoming", "San Jose State") } returns listOf(missing, game)
        val service = service()

        fun matchup() =
            if (probability) {
                service.getWinProbabilityChartBySeasonAndMatchup(12, "Wyoming", "San Jose State")
            } else {
                service.getScoreChartBySeasonAndMatchup(12, "Wyoming", "San Jose State")
            }
        val response = matchup()
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        assertEquals(1, response.body!!.size)
        val json = ObjectMapper().readTree(ObjectMapper().writeValueAsBytes(response.body))
        assertArrayEquals(response.body!![0], Base64.getDecoder().decode(json[0].asText()))
        every { gameService.getGameBySeasonAndMatchup(12, "Wyoming", "San Jose State") } returns listOf(missing)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, matchup().statusCode)
        every { gameService.getGameBySeasonAndMatchup(12, "Wyoming", "San Jose State") } returns emptyList()
        assertEquals(HttpStatus.NOT_FOUND, matchup().statusCode)
    }

    @Test
    fun `unavailable probabilities skip teams and rendering`() {
        stubGame(listOf(Play()))
        val response = service().getWinProbabilityChart(7)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNull(response.body)
        verify(exactly = 0) { teamService.getTeamByName(any()) }
        verify(exactly = 0) { probabilityRenderer.render(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `rendering and image storage failures retain server errors`() {
        stubGame(plays())
        every { scoreRenderer.render(any(), any(), any(), any()) } throws IllegalStateException("render failed")
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, service().getScoreChart(7).statusCode)
        Files.writeString(imagePath.resolve("charts"), "not a directory")
        val response = service().getWinProbabilityChart(7)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun `Elo empty statistics and failed reads retain null output`() {
        every { teamRepository.getAllActiveTeams() } returns listOf(homeTeam)
        every { gameStatsRepository.findBySeasonOrderByGameIdAsc(12) } returns emptyList()
        assertNull(service().getEloChart(12))
        every { gameStatsRepository.findBySeasonOrderByGameIdAsc(12) } throws IllegalStateException("read failed")
        assertNull(service().getEloChart(12))
        verify(exactly = 0) { eloRenderer.render(any(), any(), any()) }
    }
}
