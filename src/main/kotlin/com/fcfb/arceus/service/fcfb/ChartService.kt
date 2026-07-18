package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.service.fcfb.chart.EloChartRenderer
import com.fcfb.arceus.service.fcfb.chart.ScoreChartRenderer
import com.fcfb.arceus.service.fcfb.chart.WinProbabilityChartRenderer
import com.fcfb.arceus.util.Logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ChartService(
    private val scoreChartRenderer: ScoreChartRenderer,
    private val winProbabilityChartRenderer: WinProbabilityChartRenderer,
    private val eloChartRenderer: EloChartRenderer,
    private val gameService: GameService,
) {
    fun getScoreChart(gameId: Int): ResponseEntity<ByteArray> {
        val chartBytes =
            scoreChartRenderer.generateScoreChart(gameId)
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
                val chartResponse = scoreChartRenderer.generateScoreChart(game.gameId)
                if (chartResponse == null) {
                    Logger.error("Failed to generate score chart for game ${game.gameId}")
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
            Logger.error("Error generating score charts for season $season, $firstTeam vs $secondTeam: ${e.message}")
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    fun getWinProbabilityChart(gameId: Int): ResponseEntity<ByteArray> {
        val chartBytes =
            winProbabilityChartRenderer.generateWinProbabilityChart(gameId)
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
                val chartResponse = winProbabilityChartRenderer.generateWinProbabilityChart(game.gameId)
                if (chartResponse == null) {
                    Logger.error("Failed to generate win probability chart for game ${game.gameId}")
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
            Logger.error("Error generating win probability charts for season $season, $firstTeam vs $secondTeam: ${e.message}")
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    fun getEloChart(season: Int) = eloChartRenderer.generateEloChart(season)
}
