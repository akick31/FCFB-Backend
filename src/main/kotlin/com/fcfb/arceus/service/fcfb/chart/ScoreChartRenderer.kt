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

/**
 * Renders the score-over-time chart for a game
 */
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
        val width = 1000 // Match win probability chart width
        val height = 510 // Match win probability chart height
        val padding = 80 // Match win probability chart padding
        val chartWidth = width - (padding * 2)
        val chartHeight = height - (padding * 2)

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        // Dark background like the win probability chart
        g.color = Color(30, 30, 30) // Dark gray background
        g.fillRect(0, 0, width, height)

        // Calculate actual chart width based on data
        val actualChartWidth =
            if (plays.isNotEmpty()) {
                ((plays.size - 1).toFloat() / plays.size * chartWidth).toInt()
            } else {
                chartWidth
            }

        // Chart area background (only fill the area with data)
        g.color = Color(40, 40, 40) // Slightly lighter for chart area
        g.fillRect(padding, padding, actualChartWidth, chartHeight)

        val homeColor = parseColor(homeTeam.primaryColor ?: "#FF0000")
        val awayColor = parseColor(awayTeam.primaryColor ?: "#0000FF")

        // Find max score for scaling (0-49, or higher if needed)
        val actualMaxScore =
            maxOf(
                plays.maxOfOrNull { it.homeScore } ?: 0,
                plays.maxOfOrNull { it.awayScore } ?: 0,
            )
        val scaleMax = maxOf(49, actualMaxScore)

        // Draw quarter divisions
        drawQuarterDivisions(g, plays, padding, chartWidth, chartHeight)

        // Draw score lines
        drawScoreLines(g, plays, homeColor, awayColor, padding, chartWidth, chartHeight, scaleMax)

        // Draw axes
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

        // Calculate quarters to show (always 4, plus OT if present)
        val maxQuarter = plays.maxOfOrNull { it.quarter } ?: 4
        val quartersToShow = if (maxQuarter > 4) maxQuarter else 4

        // Draw home team line
        g.color = homeColor
        for (i in 0 until totalPlays - 1) {
            val play = plays[i]
            val nextPlay = plays[i + 1]

            // Calculate X position based on play position within quarters
            val currentX = calculatePlayXPosition(play, plays, quartersToShow, chartWidth, padding)
            val nextX = calculatePlayXPosition(nextPlay, plays, quartersToShow, chartWidth, padding)

            val y1 = padding + chartHeight - ((play.homeScore.toFloat() / maxScore) * chartHeight).toInt()
            val y2 = padding + chartHeight - ((nextPlay.homeScore.toFloat() / maxScore) * chartHeight).toInt()
            g.drawLine(currentX, y1, nextX, y2)
        }

        // Draw away team line
        g.color = awayColor
        for (i in 0 until totalPlays - 1) {
            val play = plays[i]
            val nextPlay = plays[i + 1]

            // Calculate X position based on play position within quarters
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
        // Draw title above the chart (top left)
        g.font = Font("Arial", Font.BOLD, 24)
        g.color = Color.WHITE
        val title = "Score"
        g.drawString(title, padding, padding - 35)

        // Get team abbreviations
        val homeTeamAbbr = homeTeam.abbreviation ?: homeTeam.name?.take(3) ?: "HOME"
        val awayTeamAbbr = awayTeam.abbreviation ?: awayTeam.name?.take(3) ?: "AWAY"

        // Draw team labels with colors and logos - both stacked vertically
        g.font = Font("Arial", Font.BOLD, 15)

        // Home team label (top) with logo on the right
        val homeLogoSize = 20
        g.color = parseColor(homeTeam.primaryColor ?: "#FF0000")
        g.fillRect(padding + 20, padding + 20, 12, 12)
        g.color = Color.WHITE
        g.drawString(homeTeamAbbr, padding + 40, padding + 32)
        // Logo to the right of abbreviation
        drawTeamLogo(g, homeTeam, padding + 80, padding + 15, homeLogoSize, homeLogoSize)

        // Away team label (right below home team) with logo on the right
        g.color = parseColor(awayTeam.primaryColor ?: "#0000FF")
        g.fillRect(padding + 20, padding + 45, 12, 12)
        g.color = Color.WHITE
        g.drawString(awayTeamAbbr, padding + 40, padding + 57)
        // Logo to the right of abbreviation
        drawTeamLogo(g, awayTeam, padding + 80, padding + 40, homeLogoSize, homeLogoSize)

        // Draw score labels on the RIGHT side of the chart (intervals of 7)
        g.font = Font("Arial", Font.BOLD, 16)
        g.color = Color.WHITE

        // Use the scaleMax parameter passed from createScoreChart
        val scaleMax = maxScore

        // Draw intervals of 7: 0, 7, 14, 21, 28, 35, 42, 49 (stop at 49, don't show 50)
        val intervals = mutableListOf<Int>()
        for (i in 0..6) { // Changed from 7 to 6 to stop at 42
            val score = i * 7
            intervals.add(score)
        }
        // Add 49 as the final interval
        intervals.add(49)

        // Add the actual max score if it's higher than 49
        if (maxScore > 49) {
            intervals.add(maxScore)
        }

        // Draw the intervals on the right side
        intervals.forEach { score ->
            val y = padding + chartHeight - ((score.toFloat() / scaleMax) * chartHeight).toInt()
            g.drawString(score.toString(), padding + chartWidth + 10, y + 5)
        }

        // Current scores in top right with both team logos and scores (raised 15% and larger)
        if (plays.isNotEmpty()) {
            val finalPlay = plays.last()
            val homeScore = finalPlay.homeScore
            val awayScore = finalPlay.awayScore

            // Draw format: [home logo] [home score] - [away score] [away logo]
            val logoSize = 36
            val startX = padding + chartWidth - 150
            val logoY = padding - 60

            // Home team logo
            drawTeamLogo(g, homeTeam, startX, logoY, logoSize, logoSize)

            // Home score (larger font)
            g.font = Font("Arial", Font.BOLD, 24)
            g.color = Color.WHITE
            val homeScoreX = startX + logoSize + 8
            g.drawString(homeScore.toString(), homeScoreX, logoY + logoSize / 2 + 8)

            // Dash separator
            val dashX = homeScoreX + 30
            g.drawString("-", dashX, logoY + logoSize / 2 + 8)

            // Away score
            val awayScoreX = dashX + 20
            g.drawString(awayScore.toString(), awayScoreX, logoY + logoSize / 2 + 8)

            // Away team logo
            val awayLogoX = awayScoreX + 30
            drawTeamLogo(g, awayTeam, awayLogoX, logoY, logoSize, logoSize)
        }
    }
}
