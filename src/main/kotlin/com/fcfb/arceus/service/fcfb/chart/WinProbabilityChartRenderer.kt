package com.fcfb.arceus.service.fcfb.chart

import com.fcfb.arceus.dto.response.PlayWinProbabilityResponse
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.service.fcfb.WinProbabilityService
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
 * Renders the win-probability-over-time chart for a game
 */
@Component
class WinProbabilityChartRenderer(
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val teamService: TeamService,
    private val winProbabilityService: WinProbabilityService,
    @Value("\${images.path}")
    imagePath: String,
) : ChartRendererBase(imagePath) {
    fun generateWinProbabilityChart(gameId: Int): ByteArray? {
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

            val chartImage = createWinProbabilityChart(game, plays, homeTeam, awayTeam)
            val chartBytes = saveChartImage(chartImage, "win_prob_$gameId")

            return chartBytes
        } catch (e: Exception) {
            Logger.error("Error generating win probability chart for game $gameId: ${e.message}")
            return null
        }
    }

    private fun createWinProbabilityChart(
        game: Game,
        plays: List<Play>,
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage {
        val width = 1000 // Wider
        val height = 510 // 15% thinner (600 * 0.85 = 510)
        val padding = 80 // More padding for cleaner look
        val chartWidth = width - (padding * 2)
        val chartHeight = height - (padding * 2)

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        // Enable anti-aliasing for crisp graphics
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        // Dark background like the reference image
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

        // Draw quarter divisions
        drawQuarterDivisions(g, plays, padding, chartWidth, chartHeight)

        // Draw win probability line
        drawWinProbabilityLine(g, game, plays, homeColor, awayColor, padding, chartWidth, chartHeight)

        // Get win probabilities for the axes
        val winProbabilitiesResponse = winProbabilityService.getWinProbabilitiesForGame(plays[0].gameId, plays)
        val winProbabilities = winProbabilitiesResponse.plays

        // Draw axes
        drawWinProbabilityAxes(g, padding, chartWidth, chartHeight, homeTeam, awayTeam, winProbabilities, game)

        g.dispose()
        return image
    }

    private fun drawWinProbabilityLine(
        g: Graphics2D,
        game: Game,
        plays: List<Play>,
        homeColor: Color,
        awayColor: Color,
        padding: Int,
        chartWidth: Int,
        chartHeight: Int,
    ) {
        val totalPlays = plays.size
        if (totalPlays < 2) return

        // Get win probabilities for the entire game using the service method
        val winProbabilitiesResponse = winProbabilityService.getWinProbabilitiesForGame(plays[0].gameId, plays)
        val originalWinProbabilities = winProbabilitiesResponse.plays

        val winProbabilities = originalWinProbabilities.toMutableList()

        // If game is final, set win probability to 100% for the winning team on the final play only
        if (game.gameStatus == GameStatus.FINAL && winProbabilities.isNotEmpty()) {
            val finalPlayIndex = winProbabilities.size - 1
            val finalPlay = winProbabilities[finalPlayIndex]

            // Determine which team won based on final scores
            val homeScore = plays.last().homeScore
            val awayScore = plays.last().awayScore

            if (homeScore > awayScore) {
                // Home team won
                winProbabilities[finalPlayIndex] =
                    finalPlay.copy(
                        homeTeamWinProbability = 1.0,
                        awayTeamWinProbability = 0.0,
                    )
            } else if (awayScore > homeScore) {
                // Away team won
                winProbabilities[finalPlayIndex] =
                    finalPlay.copy(
                        homeTeamWinProbability = 0.0,
                        awayTeamWinProbability = 1.0,
                    )
            }
        }

        // Create filled area chart like the reference image
        drawWinProbabilityFilledArea(
            g,
            winProbabilities,
            plays,
            homeColor,
            awayColor,
            padding,
            chartWidth,
            chartHeight,
        )
    }

    private fun drawWinProbabilityFilledArea(
        g: Graphics2D,
        winProbabilities: List<PlayWinProbabilityResponse>,
        plays: List<Play>,
        homeColor: Color,
        awayColor: Color,
        padding: Int,
        chartWidth: Int,
        chartHeight: Int,
    ) {
        val totalPlays = winProbabilities.size
        if (totalPlays < 2) return

        // Calculate quarters to show (always 4, plus OT if present)
        val maxQuarter = plays.maxOfOrNull { it.quarter } ?: 4
        val quartersToShow = if (maxQuarter > 4) maxQuarter else 4

        // Draw the 50% line first (full width of chart)
        g.color = Color(200, 200, 200)
        g.stroke = BasicStroke(1f)
        val midY = padding + chartHeight / 2
        g.drawLine(padding, midY, padding + chartWidth, midY)

        // Create the win probability line with color changes at 50%
        g.stroke = BasicStroke(3f)

        for (i in 0 until totalPlays - 1) {
            val play = plays[i]
            val nextPlay = plays[i + 1]

            // Calculate X position based on play position within quarters
            val currentX = calculatePlayXPosition(play, plays, quartersToShow, chartWidth, padding)
            val nextX = calculatePlayXPosition(nextPlay, plays, quartersToShow, chartWidth, padding)

            // Ensure we don't draw beyond the chart boundaries
            if (nextX > padding + chartWidth) break
            val homeWinProb1 = winProbabilities[i].homeTeamWinProbability
            val homeWinProb2 = winProbabilities[i + 1].homeTeamWinProbability

            val y1 = padding + chartHeight - ((homeWinProb1 * chartHeight).toInt())
            val y2 = padding + chartHeight - ((homeWinProb2 * chartHeight).toInt())

            // Check if the line crosses the 50% threshold
            val crosses50 = (homeWinProb1 > 0.5 && homeWinProb2 <= 0.5) || (homeWinProb1 <= 0.5 && homeWinProb2 > 0.5)

            if (crosses50) {
                // Calculate the exact crossing point
                val crossingPoint = (0.5 - homeWinProb1) / (homeWinProb2 - homeWinProb1)
                val crossingX = currentX + (crossingPoint * (nextX - currentX)).toInt()
                val crossingY = padding + chartHeight - ((0.5 * chartHeight).toInt())

                // Draw first segment
                g.color = if (homeWinProb1 > 0.5) homeColor else awayColor
                g.drawLine(currentX, y1, crossingX, crossingY)

                // Draw second segment
                g.color = if (homeWinProb2 > 0.5) homeColor else awayColor
                g.drawLine(crossingX, crossingY, nextX, y2)
            } else {
                // No crossing, use single color
                g.color = if (homeWinProb1 > 0.5) homeColor else awayColor
                g.drawLine(currentX, y1, nextX, y2)
            }
        }

        // Draw filled areas - one continuous area that changes color
        for (i in 0 until totalPlays - 1) {
            val play = plays[i]
            val nextPlay = plays[i + 1]

            // Calculate X position based on play position within quarters
            val currentX = calculatePlayXPosition(play, plays, quartersToShow, chartWidth, padding)
            val nextX = calculatePlayXPosition(nextPlay, plays, quartersToShow, chartWidth, padding)

            // Ensure we don't draw beyond the chart boundaries
            if (nextX > padding + chartWidth) break
            val homeWinProb1 = winProbabilities[i].homeTeamWinProbability
            val homeWinProb2 = winProbabilities[i + 1].homeTeamWinProbability

            val y1 = padding + chartHeight - ((homeWinProb1 * chartHeight).toInt())
            val y2 = padding + chartHeight - ((homeWinProb2 * chartHeight).toInt())

            // Check if the line crosses the 50% threshold
            val crosses50 = (homeWinProb1 > 0.5 && homeWinProb2 <= 0.5) || (homeWinProb1 <= 0.5 && homeWinProb2 > 0.5)

            if (crosses50) {
                // Calculate the exact crossing point
                val crossingPoint = (0.5 - homeWinProb1) / (homeWinProb2 - homeWinProb1)
                val crossingX = currentX + (crossingPoint * (nextX - currentX)).toInt()
                val crossingY = padding + chartHeight - ((0.5 * chartHeight).toInt())

                // Draw first filled area segment
                g.color =
                    if (homeWinProb1 > 0.5) {
                        Color(homeColor.red, homeColor.green, homeColor.blue, 120)
                    } else {
                        Color(awayColor.red, awayColor.green, awayColor.blue, 120)
                    }
                val areaX1 = intArrayOf(currentX, crossingX, crossingX, currentX)
                val areaY1 = intArrayOf(y1, crossingY, midY, midY)
                g.fillPolygon(areaX1, areaY1, 4)

                // Draw second filled area segment
                g.color =
                    if (homeWinProb2 > 0.5) {
                        Color(homeColor.red, homeColor.green, homeColor.blue, 120)
                    } else {
                        Color(awayColor.red, awayColor.green, awayColor.blue, 120)
                    }
                val areaX2 = intArrayOf(crossingX, nextX, nextX, crossingX)
                val areaY2 = intArrayOf(crossingY, y2, midY, midY)
                g.fillPolygon(areaX2, areaY2, 4)
            } else {
                // No crossing, use single color
                g.color =
                    if (homeWinProb1 > 0.5) {
                        Color(homeColor.red, homeColor.green, homeColor.blue, 120)
                    } else {
                        Color(awayColor.red, awayColor.green, awayColor.blue, 120)
                    }
                val areaX = intArrayOf(currentX, nextX, nextX, currentX)
                val areaY = intArrayOf(y1, y2, midY, midY)
                g.fillPolygon(areaX, areaY, 4)
            }
        }
    }

    private fun drawWinProbabilitySegment(
        g: Graphics2D,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        winProb1: Double,
        winProb2: Double,
        homeColor: Color,
        awayColor: Color,
    ) {
        // Simple color scheme: > 50% = home team color, <= 50% = away team color
        // Change color exactly at 50% line

        // Check if the line crosses the 50% threshold
        val crosses50 = (winProb1 > 0.5 && winProb2 <= 0.5) || (winProb1 <= 0.5 && winProb2 > 0.5)

        if (crosses50) {
            // Calculate exact crossing point
            val crossingPoint = (0.5 - winProb1) / (winProb2 - winProb1)
            val crossingX = x1 + (crossingPoint * (x2 - x1)).toInt()
            val crossingY = y1 + (crossingPoint * (y2 - y1)).toInt()

            // Draw first segment with appropriate color
            g.color = if (winProb1 > 0.5) homeColor else awayColor
            g.drawLine(x1, y1, crossingX, crossingY)

            // Draw second segment with opposite color
            g.color = if (winProb2 > 0.5) homeColor else awayColor
            g.drawLine(crossingX, crossingY, x2, y2)
        } else {
            // No crossing, use single color for entire segment
            g.color = if (winProb1 > 0.5) homeColor else awayColor
            g.drawLine(x1, y1, x2, y2)
        }
    }

    private fun drawWinProbabilityAxes(
        g: Graphics2D,
        padding: Int,
        chartWidth: Int,
        chartHeight: Int,
        homeTeam: Team,
        awayTeam: Team,
        winProbabilities: List<PlayWinProbabilityResponse>,
        game: Game,
    ) {
        // Draw title above the chart (top left)
        g.font = Font("Arial", Font.BOLD, 24)
        g.color = Color.WHITE
        val title = "Win Probability"
        g.drawString(title, padding, padding - 35)

        // Get team abbreviations
        val homeTeamAbbr = homeTeam.abbreviation ?: homeTeam.name?.take(3) ?: "HOME"
        val awayTeamAbbr = awayTeam.abbreviation ?: awayTeam.name?.take(3) ?: "AWAY"

        // Draw team labels with colors (5% bigger font)
        g.font = Font("Arial", Font.BOLD, 15)

        // Home team label (top)
        g.color = parseColor(homeTeam.primaryColor ?: "#FF0000")
        g.fillRect(padding + 20, padding + 20, 12, 12)
        g.color = Color.WHITE
        g.drawString(homeTeamAbbr, padding + 40, padding + 32)

        // Away team label (bottom)
        g.color = parseColor(awayTeam.primaryColor ?: "#0000FF")
        g.fillRect(padding + 20, padding + chartHeight - 25, 12, 12)
        g.color = Color.WHITE
        g.drawString(awayTeamAbbr, padding + 40, padding + chartHeight - 13)

        // Draw percentage labels on the RIGHT side of the chart (larger font)
        g.font = Font("Arial", Font.BOLD, 16)
        g.color = Color.WHITE

        // 100% at top
        g.drawString("100", padding + chartWidth + 10, padding + 20)

        // 50% in middle
        val midY = padding + chartHeight / 2
        g.drawString("50", padding + chartWidth + 10, midY + 5)

        // 100% at bottom (for away team)
        g.drawString("100", padding + chartWidth + 10, padding + chartHeight - 10)

        // Quarter division lines are drawn by drawQuarterDivisions method
        // No additional grid lines needed here

        // Draw team logo and current win probability in top right
        if (winProbabilities.isNotEmpty()) {
            val finalWinProb = winProbabilities.last().homeTeamWinProbability
            val currentTeam = if (finalWinProb > 0.5) homeTeam else awayTeam
            val currentWinProb = if (finalWinProb > 0.5) finalWinProb else (1.0 - finalWinProb)

            // For final games, the win probability should be 100% for the winning team
            // This matches the logic in the chart drawing where final games show 100%
            val adjustedWinProb =
                if (game.gameStatus == GameStatus.FINAL) {
                    1.0 // Show 100% for final games
                } else {
                    currentWinProb // Show actual probability for live games
                }

            // Draw team logo using the proper method
            val logoSize = 70
            val logoX = padding + chartWidth - 150
            val logoY = padding - 80

            drawTeamLogo(g, currentTeam, logoX, logoY, logoSize, logoSize)

            // Draw win probability percentage (bigger font)
            g.color = Color.WHITE
            g.font = Font("Arial", Font.BOLD, 25)
            val winProbText = String.format("%.1f%%", adjustedWinProb * 100)
            g.drawString(winProbText, logoX + logoSize + 10, logoY + 45)
        }
    }
}
