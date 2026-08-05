package com.fcfb.arceus.service.fcfb.chart

import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.util.GameNotFoundException
import com.fcfb.arceus.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

@Component
class ScoreChartRenderer(
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val teamService: TeamService,
    @Value("\${images.path}")
    imagePath: String,
) : ChartRendererBase(imagePath) {
    fun generateScoreChart(gameId: Int): ByteArray? {
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

            val chartImage = createScoreChart(game, plays, homeTeam, awayTeam)
            val chartBytes = saveChartImage(chartImage, "score_$gameId")

            return chartBytes
        } catch (e: Exception) {
            Logger.error("Error generating score chart for game $gameId: ${e.message}")
            return null
        }
    }

    private fun createScoreChart(
        game: Game,
        plays: List<Play>,
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage {
        val width = 1000
        val height = 510
        val padding = 80
        val chartWidth = width - (padding * 2)
        val chartHeight = height - (padding * 2)

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        g.color = Color(30, 30, 30)
        g.fillRect(0, 0, width, height)

        val actualChartWidth =
            if (plays.isNotEmpty()) {
                ((plays.size - 1).toFloat() / plays.size * chartWidth).toInt()
            } else {
                chartWidth
            }

        g.color = Color(40, 40, 40)
        g.fillRect(padding, padding, actualChartWidth, chartHeight)

        val homeColor = parseColor(homeTeam.primaryColor ?: "#FF0000")
        val awayColor = parseColor(awayTeam.primaryColor ?: "#0000FF")

        val actualMaxScore =
            maxOf(
                plays.maxOfOrNull { it.homeScore } ?: 0,
                plays.maxOfOrNull { it.awayScore } ?: 0,
            )
        val scaleMax = maxOf(49, actualMaxScore)

        drawQuarterDivisions(g, plays, padding, chartWidth, chartHeight)

        drawScoreLines(g, plays, homeColor, awayColor, padding, chartWidth, chartHeight, scaleMax)

        drawScoreAxes(g, padding, chartWidth, chartHeight, scaleMax, homeTeam, awayTeam, plays, game)

        g.dispose()
        return image
    }

    private fun drawScoreLines(
        g: Graphics2D,
        plays: List<Play>,
        homeColor: Color,
        awayColor: Color,
        padding: Int,
        chartWidth: Int,
        chartHeight: Int,
        maxScore: Int,
    ) {
        g.stroke = BasicStroke(3f)

        val totalPlays = plays.size
        if (totalPlays < 2) return

        val maxQuarter = plays.maxOfOrNull { it.quarter } ?: 4
        val quartersToShow = if (maxQuarter > 4) maxQuarter else 4

        g.color = homeColor
        for (i in 0 until totalPlays - 1) {
            val play = plays[i]
            val nextPlay = plays[i + 1]

            val currentX = calculatePlayXPosition(play, plays, quartersToShow, chartWidth, padding)
            val nextX = calculatePlayXPosition(nextPlay, plays, quartersToShow, chartWidth, padding)

            val y1 = padding + chartHeight - ((play.homeScore.toFloat() / maxScore) * chartHeight).toInt()
            val y2 = padding + chartHeight - ((nextPlay.homeScore.toFloat() / maxScore) * chartHeight).toInt()
            g.drawLine(currentX, y1, nextX, y2)
        }

        g.color = awayColor
        for (i in 0 until totalPlays - 1) {
            val play = plays[i]
            val nextPlay = plays[i + 1]

            val currentX = calculatePlayXPosition(play, plays, quartersToShow, chartWidth, padding)
            val nextX = calculatePlayXPosition(nextPlay, plays, quartersToShow, chartWidth, padding)

            val y1 = padding + chartHeight - ((play.awayScore.toFloat() / maxScore) * chartHeight).toInt()
            val y2 = padding + chartHeight - ((nextPlay.awayScore.toFloat() / maxScore) * chartHeight).toInt()
            g.drawLine(currentX, y1, nextX, y2)
        }
    }

    private fun drawScoreAxes(
        g: Graphics2D,
        padding: Int,
        chartWidth: Int,
        chartHeight: Int,
        maxScore: Int,
        homeTeam: Team,
        awayTeam: Team,
        plays: List<Play>,
        game: Game,
    ) {
        g.font = Font("Arial", Font.BOLD, 24)
        g.color = Color.WHITE
        val title = "Score"
        g.drawString(title, padding, padding - 35)

        val homeTeamAbbr = homeTeam.abbreviation ?: homeTeam.name?.take(3) ?: "HOME"
        val awayTeamAbbr = awayTeam.abbreviation ?: awayTeam.name?.take(3) ?: "AWAY"

        g.font = Font("Arial", Font.BOLD, 15)

        val homeLogoSize = 20
        g.color = parseColor(homeTeam.primaryColor ?: "#FF0000")
        g.fillRect(padding + 20, padding + 20, 12, 12)
        g.color = Color.WHITE
        g.drawString(homeTeamAbbr, padding + 40, padding + 32)
        drawTeamLogo(g, homeTeam, padding + 80, padding + 15, homeLogoSize, homeLogoSize)

        g.color = parseColor(awayTeam.primaryColor ?: "#0000FF")
        g.fillRect(padding + 20, padding + 45, 12, 12)
        g.color = Color.WHITE
        g.drawString(awayTeamAbbr, padding + 40, padding + 57)
        drawTeamLogo(g, awayTeam, padding + 80, padding + 40, homeLogoSize, homeLogoSize)

        g.font = Font("Arial", Font.BOLD, 16)
        g.color = Color.WHITE

        val scaleMax = maxScore

        val intervals = mutableListOf<Int>()
        for (i in 0..6) {
            val score = i * 7
            intervals.add(score)
        }
        intervals.add(49)

        if (maxScore > 49) {
            intervals.add(maxScore)
        }

        intervals.forEach { score ->
            val y = padding + chartHeight - ((score.toFloat() / scaleMax) * chartHeight).toInt()
            g.drawString(score.toString(), padding + chartWidth + 10, y + 5)
        }

        if (plays.isNotEmpty()) {
            val finalPlay = plays.last()
            val homeScore = finalPlay.homeScore
            val awayScore = finalPlay.awayScore

            val logoSize = 36
            val startX = padding + chartWidth - 150
            val logoY = padding - 60

            drawTeamLogo(g, homeTeam, startX, logoY, logoSize, logoSize)

            g.font = Font("Arial", Font.BOLD, 24)
            g.color = Color.WHITE
            val homeScoreX = startX + logoSize + 8
            g.drawString(homeScore.toString(), homeScoreX, logoY + logoSize / 2 + 8)

            val dashX = homeScoreX + 30
            g.drawString("-", dashX, logoY + logoSize / 2 + 8)

            val awayScoreX = dashX + 20
            g.drawString(awayScore.toString(), awayScoreX, logoY + logoSize / 2 + 8)

            val awayLogoX = awayScoreX + 30
            drawTeamLogo(g, awayTeam, awayLogoX, logoY, logoSize, logoSize)
        }
    }
}
