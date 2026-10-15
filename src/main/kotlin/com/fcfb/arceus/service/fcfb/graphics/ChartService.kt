package com.fcfb.arceus.service.fcfb.graphics

import com.fcfb.arceus.dto.response.PlayWinProbabilityResponse
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.exception.game.GameNotFoundException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.rendering.chart.EloChartRenderer
import com.fcfb.arceus.rendering.chart.ScoreChartRenderer
import com.fcfb.arceus.rendering.chart.WinProbabilityChartRenderer
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.probability.WinProbabilityOrchestrationService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@Service
class ChartService(
    private val scoreChartRenderer: ScoreChartRenderer,
    private val winProbabilityChartRenderer: WinProbabilityChartRenderer,
    private val eloChartRenderer: EloChartRenderer,
    private val gameService: GameService,
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val teamRepository: TeamRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val teamService: TeamService,
    private val winProbabilityOrchestrationService: WinProbabilityOrchestrationService,
    @Value("\${images.path}")
    private val imagePath: String,
) {
    fun getScoreChart(gameId: Int): ResponseEntity<ByteArray> {
        val chartBytes =
            generateScoreChart(gameId)
                ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.IMAGE_PNG
                contentLength = chartBytes.size.toLong()
            }

        return ResponseEntity(chartBytes, headers, HttpStatus.OK)
    }

    fun getScoreChartBySeasonAndMatchup(
        season: Int,
        firstTeam: String,
        secondTeam: String,
    ): ResponseEntity<List<ByteArray>> {
        try {
            val gameList = gameService.getGameBySeasonAndMatchup(season, firstTeam, secondTeam)
            if (gameList.isEmpty()) {
                return ResponseEntity(HttpStatus.NOT_FOUND)
            }

            val chartList = mutableListOf<ByteArray>()
            for (game in gameList) {
                val chartResponse = generateScoreChart(game.gameId)
                if (chartResponse == null) {
                    Logger().error("Failed to generate score chart for game ${game.gameId}")
                    continue
                }
                chartList.add(chartResponse)
            }

            if (chartList.isEmpty()) {
                return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
            }

            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }

            return ResponseEntity(chartList, headers, HttpStatus.OK)
        } catch (e: Exception) {
            Logger().error("Error generating score charts for season $season, $firstTeam vs $secondTeam: ${e.message}")
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    fun getWinProbabilityChart(gameId: Int): ResponseEntity<ByteArray> {
        val chartBytes =
            generateWinProbabilityChart(gameId)
                ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.IMAGE_PNG
                contentLength = chartBytes.size.toLong()
            }

        return ResponseEntity(chartBytes, headers, HttpStatus.OK)
    }

    fun getWinProbabilityChartBySeasonAndMatchup(
        season: Int,
        firstTeam: String,
        secondTeam: String,
    ): ResponseEntity<List<ByteArray>> {
        try {
            val gameList = gameService.getGameBySeasonAndMatchup(season, firstTeam, secondTeam)
            if (gameList.isEmpty()) {
                return ResponseEntity(HttpStatus.NOT_FOUND)
            }

            val chartList = mutableListOf<ByteArray>()
            for (game in gameList) {
                val chartResponse = generateWinProbabilityChart(game.gameId)
                if (chartResponse == null) {
                    Logger().error("Failed to generate win probability chart for game ${game.gameId}")
                    continue
                }
                chartList.add(chartResponse)
            }

            if (chartList.isEmpty()) {
                return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
            }

            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }

            return ResponseEntity(chartList, headers, HttpStatus.OK)
        } catch (e: Exception) {
            Logger().error("Error generating win probability charts for season $season, $firstTeam vs $secondTeam: ${e.message}")
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun generateScoreChart(gameId: Int): ByteArray? {
        try {
            val game =
                gameRepository.getGameById(gameId)
                    ?: throw GameNotFoundException("Game not found with ID: $gameId")

            val plays = playRepository.getAllPlaysByGameId(gameId).sortedBy { it.playNumber }
            if (plays.isEmpty()) {
                return null
            }

            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            val chartImage = scoreChartRenderer.render(game, plays, homeTeam, awayTeam)
            val chartBytes = saveChartImage(chartImage, "score_$gameId")

            return chartBytes
        } catch (e: Exception) {
            Logger().error("Error generating score chart for game $gameId: ${e.message}")
            return null
        }
    }

    private fun generateWinProbabilityChart(gameId: Int): ByteArray? {
        try {
            val game =
                gameRepository.getGameById(gameId)
                    ?: throw GameNotFoundException("Game not found with ID: $gameId")

            val plays =
                playRepository.getAllPlaysByGameId(gameId)
                    .filter { it.winProbability != null }
                    .sortedBy { it.playNumber }

            if (plays.isEmpty()) {
                return null
            }

            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            val probabilities = winProbabilityOrchestrationService.getWinProbabilitiesForGame(plays[0].gameId, plays).plays
            val lineProbabilities = probabilitiesForLine(game, plays, probabilities)
            val chartImage = winProbabilityChartRenderer.render(game, plays, homeTeam, awayTeam, lineProbabilities, probabilities)
            val chartBytes = saveChartImage(chartImage, "win_prob_$gameId")

            return chartBytes
        } catch (e: Exception) {
            Logger().error("Error generating win probability chart for game $gameId: ${e.message}")
            return null
        }
    }

    fun getEloChart(season: Int): ByteArray? {
        try {
            Logger().info("Generating ELO chart for season $season")

            val teams = teamRepository.getAllActiveTeams()
            if (teams.isEmpty()) {
                return null
            }

            val gameStatsList = gameStatsRepository.findBySeasonOrderByGameIdAsc(season)
            if (gameStatsList.isEmpty()) {
                return null
            }

            val teamEloData = mutableMapOf<String, MutableMap<Int, Double>>()
            val teamColors = mutableMapOf<String, Pair<String?, String?>>()

            teams.forEach { team ->
                teamEloData[team.name ?: ""] = mutableMapOf()
                teamColors[team.name ?: ""] = team.primaryColor to team.secondaryColor
            }

            gameStatsList.forEach { gameStats ->
                val teamName = gameStats.team ?: return@forEach
                val week = gameStats.week ?: return@forEach
                val elo = gameStats.teamElo

                teamEloData[teamName]?.set(week, elo)
            }

            val chartImage = eloChartRenderer.render(teamEloData, teamColors, season)

            val chartBytes = saveChartImage(chartImage, "elo_chart_season_$season")
            return chartBytes
        } catch (e: Exception) {
            Logger().error("Error generating ELO chart for season $season: ${e.message}", e)
            return null
        }
    }

    private fun probabilitiesForLine(
        game: Game,
        plays: List<Play>,
        probabilities: List<PlayWinProbabilityResponse>,
    ): List<PlayWinProbabilityResponse> {
        if (plays.size < 2) return probabilities
        val winProbabilities = probabilities.toMutableList()

        if (game.gameStatus == GameStatus.FINAL && winProbabilities.isNotEmpty()) {
            val finalPlayIndex = winProbabilities.size - 1
            val finalPlay = winProbabilities[finalPlayIndex]

            val homeScore = plays.last().homeScore
            val awayScore = plays.last().awayScore

            if (homeScore > awayScore) {
                winProbabilities[finalPlayIndex] =
                    finalPlay.copy(
                        homeTeamWinProbability = 1.0,
                        awayTeamWinProbability = 0.0,
                    )
            } else if (awayScore > homeScore) {
                winProbabilities[finalPlayIndex] =
                    finalPlay.copy(
                        homeTeamWinProbability = 0.0,
                        awayTeamWinProbability = 1.0,
                    )
            }
        }

        return winProbabilities
    }

    private fun saveChartImage(
        image: BufferedImage,
        filename: String,
    ): ByteArray {
        val outputFile = File("$imagePath/charts/$filename.png")

        val directory = File("$imagePath/charts")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        ImageIO.write(image, "png", outputFile)
        return outputFile.readBytes()
    }
}
