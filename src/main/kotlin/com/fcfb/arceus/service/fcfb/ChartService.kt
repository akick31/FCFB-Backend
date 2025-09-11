package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.PlayWinProbabilityResponse
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.util.GameNotFoundException
import com.fcfb.arceus.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URI
import javax.imageio.ImageIO

@Service
class ChartService(
    private val gameRepository: GameRepository,
    private val playRepository: PlayRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val teamRepository: TeamRepository,
    private val teamService: TeamService,
    private val winProbabilityService: WinProbabilityService,
    @Value("\${images.path}")
    private val imagePath: String,
) {
    /**
     * Generate a score chart for a game
     * @param gameId The ID of the game to generate a chart for
     * @return ResponseEntity containing the chart image as PNG bytes
     */
    fun generateScoreChart(gameId: Int): ResponseEntity<ByteArray> {
        try {
            val game =
                gameRepository.getGameById(gameId)
                    ?: throw GameNotFoundException("Game not found with ID: $gameId")

            val plays = playRepository.getAllPlaysByGameId(gameId).sortedBy { it.playNumber }
            if (plays.isEmpty()) {
                return ResponseEntity(HttpStatus.NOT_FOUND)
            }

            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            val chartImage = createScoreChart(game, plays, homeTeam, awayTeam)
            val chartBytes = saveChartImage(chartImage, "score_$gameId")

            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.IMAGE_PNG
                    contentLength = chartBytes.size.toLong()
                }

            return ResponseEntity(chartBytes, headers, HttpStatus.OK)
        } catch (e: Exception) {
            Logger.error("Error generating score chart for game $gameId: ${e.message}")
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    /**
     * Generate a win probability chart for a game
     * @param gameId The ID of the game to generate a chart for
     * @return ResponseEntity containing the chart image as PNG bytes
     */
    fun generateWinProbabilityChart(gameId: Int): ResponseEntity<ByteArray> {
        try {
            val game =
                gameRepository.getGameById(gameId)
                    ?: throw GameNotFoundException("Game not found with ID: $gameId")

            val plays =
                playRepository.getAllPlaysByGameId(gameId)
                    .filter { it.winProbability != null }
                    .sortedBy { it.playNumber }

            if (plays.isEmpty()) {
                return ResponseEntity(HttpStatus.NOT_FOUND)
            }

            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            val chartImage = createWinProbabilityChart(game, plays, homeTeam, awayTeam)
            val chartBytes = saveChartImage(chartImage, "win_prob_$gameId")

            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.IMAGE_PNG
                    contentLength = chartBytes.size.toLong()
                }

            return ResponseEntity(chartBytes, headers, HttpStatus.OK)
        } catch (e: Exception) {
            Logger.error("Error generating win probability chart for game $gameId: ${e.message}")
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    /**
     * Create the score chart image
     */
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

        // Get team colors
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

    /**
     * Create the win probability chart image
     */
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

        // Get team colors
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

    /**
     * Draw quarter divisions on the chart
     */
    private fun drawQuarterDivisions(
        g: Graphics2D,
        plays: List<Play>,
        padding: Int,
        chartWidth: Int,
        chartHeight: Int,
    ) {
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(1f)

        val quarters = plays.groupBy { it.quarter }
        val totalPlays = plays.size

        quarters.forEach { (quarter, quarterPlays) ->
            val quarterStart = quarterPlays.minOf { it.playNumber }
            val quarterEnd = quarterPlays.maxOf { it.playNumber }

            val x1 = padding + ((quarterStart - 1).toFloat() / (totalPlays - 1) * chartWidth).toInt()
            val x2 = padding + ((quarterEnd - 1).toFloat() / (totalPlays - 1) * chartWidth).toInt()

            // Draw vertical line at quarter boundary
            g.drawLine(x2, padding, x2, padding + chartHeight)

            // Draw quarter label
            g.font = Font("Arial", Font.BOLD, 12)
            val quarterLabel = "${quarter}Q"
            val labelWidth = g.fontMetrics.stringWidth(quarterLabel)
            g.drawString(quarterLabel, x1 + (x2 - x1 - labelWidth) / 2, padding - 10)
        }
    }

    /**
     * Draw score lines for both teams
     */
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

        // Draw home team line
        g.color = homeColor
        for (i in 0 until totalPlays - 1) {
            val x1 = padding + (i.toFloat() / totalPlays * chartWidth).toInt()
            val x2 = padding + ((i + 1).toFloat() / totalPlays * chartWidth).toInt()
            val y1 = padding + chartHeight - ((plays[i].homeScore.toFloat() / maxScore) * chartHeight).toInt()
            val y2 = padding + chartHeight - ((plays[i + 1].homeScore.toFloat() / maxScore) * chartHeight).toInt()
            g.drawLine(x1, y1, x2, y2)
        }

        // Draw away team line
        g.color = awayColor
        for (i in 0 until totalPlays - 1) {
            val x1 = padding + (i.toFloat() / totalPlays * chartWidth).toInt()
            val x2 = padding + ((i + 1).toFloat() / totalPlays * chartWidth).toInt()
            val y1 = padding + chartHeight - ((plays[i].awayScore.toFloat() / maxScore) * chartHeight).toInt()
            val y2 = padding + chartHeight - ((plays[i + 1].awayScore.toFloat() / maxScore) * chartHeight).toInt()
            g.drawLine(x1, y1, x2, y2)
        }
    }

    /**
     * Draw win probability line with color coding
     */
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

        // Create a mutable list for modifications
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
            homeColor,
            awayColor,
            padding,
            chartWidth,
            chartHeight,
        )
    }

    /**
     * Draw win probability as filled areas like the reference image
     */
    private fun drawWinProbabilityFilledArea(
        g: Graphics2D,
        winProbabilities: List<PlayWinProbabilityResponse>,
        homeColor: Color,
        awayColor: Color,
        padding: Int,
        chartWidth: Int,
        chartHeight: Int,
    ) {
        val totalPlays = winProbabilities.size
        if (totalPlays < 2) return

        // Draw the 50% line first (only to the end of actual data)
        g.color = Color(200, 200, 200)
        g.stroke = BasicStroke(1f)
        val midY = padding + chartHeight / 2
        val actualChartWidth =
            if (winProbabilities.isNotEmpty()) {
                ((winProbabilities.size - 1).toFloat() / winProbabilities.size * chartWidth).toInt()
            } else {
                chartWidth
            }
        g.drawLine(padding, midY, padding + actualChartWidth, midY)

        // Create the win probability line with color changes at 50%
        g.stroke = BasicStroke(3f)

        for (i in 0 until totalPlays - 1) {
            val x1 = padding + (i.toFloat() / totalPlays * chartWidth).toInt()
            val x2 = padding + ((i + 1).toFloat() / totalPlays * chartWidth).toInt()

            // Ensure we don't draw beyond the chart boundaries
            if (x2 > padding + chartWidth) break
            val homeWinProb1 = winProbabilities[i].homeTeamWinProbability
            val homeWinProb2 = winProbabilities[i + 1].homeTeamWinProbability

            val y1 = padding + chartHeight - ((homeWinProb1 * chartHeight).toInt())
            val y2 = padding + chartHeight - ((homeWinProb2 * chartHeight).toInt())

            // Determine color based on win probability
            val avgWinProb = (homeWinProb1 + homeWinProb2) / 2.0
            g.color = if (avgWinProb > 0.5) homeColor else awayColor

            g.drawLine(x1, y1, x2, y2)
        }

        // Draw filled areas - one continuous area that changes color
        for (i in 0 until totalPlays - 1) {
            val x1 = padding + (i.toFloat() / totalPlays * chartWidth).toInt()
            val x2 = padding + ((i + 1).toFloat() / totalPlays * chartWidth).toInt()

            // Ensure we don't draw beyond the chart boundaries
            if (x2 > padding + chartWidth) break
            val homeWinProb1 = winProbabilities[i].homeTeamWinProbability
            val homeWinProb2 = winProbabilities[i + 1].homeTeamWinProbability

            val y1 = padding + chartHeight - ((homeWinProb1 * chartHeight).toInt())
            val y2 = padding + chartHeight - ((homeWinProb2 * chartHeight).toInt())

            // Create filled area for this segment
            val areaX = intArrayOf(x1, x2, x2, x1)
            val areaY = intArrayOf(y1, y2, midY, midY)

            // Determine color based on win probability
            val avgWinProb = (homeWinProb1 + homeWinProb2) / 2.0
            g.color =
                if (avgWinProb > 0.5) {
                    Color(homeColor.red, homeColor.green, homeColor.blue, 120)
                } else {
                    Color(awayColor.red, awayColor.green, awayColor.blue, 120)
                }

            g.fillPolygon(areaX, areaY, 4)
        }
    }

    /**
     * Draw a segment of the win probability line with color change exactly at 50%
     */
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

    /**
     * Draw axes for score chart
     */
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

    /**
     * Draw axes for win probability chart
     */
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

    /**
     * Draw team logo on the chart
     */
    private fun drawTeamLogo(
        g: Graphics2D,
        team: Team,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val logoUrl = team.scorebugLogo
        if (logoUrl != null) {
            try {
                val logoImage = ImageIO.read(URI(logoUrl).toURL())
                // Use high-quality rendering for logos
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g.drawImage(logoImage, x, y, width, height, null)
            } catch (e: IOException) {
                Logger.error("Error loading logo for ${team.name}: ${e.message}")
                // Fallback to team abbreviation with better styling
                g.font = Font("Arial", Font.BOLD, 16)
                g.color = parseColor(team.primaryColor ?: "#000000")
                val abbreviation = team.abbreviation ?: "TEAM"
                val textWidth = g.fontMetrics.stringWidth(abbreviation)
                g.drawString(abbreviation, x + (width - textWidth) / 2, y + height / 2 + 6)
            }
        } else {
            // Fallback to team abbreviation with better styling
            g.font = Font("Arial", Font.BOLD, 16)
            g.color = parseColor(team.primaryColor ?: "#000000")
            val abbreviation = team.abbreviation ?: "TEAM"
            val textWidth = g.fontMetrics.stringWidth(abbreviation)
            g.drawString(abbreviation, x + (width - textWidth) / 2, y + height / 2 + 6)
        }
    }

    /**
     * Parse color string to Color object
     */
    private fun parseColor(colorString: String): Color {
        return try {
            Color.decode(colorString)
        } catch (e: NumberFormatException) {
            Color.BLACK
        }
    }

    /**
     * Save chart image to file and return bytes
     */
    private fun saveChartImage(
        image: BufferedImage,
        filename: String,
    ): ByteArray {
        val outputFile = File("$imagePath/charts/$filename.png")

        // Create directory if it doesn't exist
        val directory = File("$imagePath/charts")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        ImageIO.write(image, "png", outputFile)
        return outputFile.readBytes()
    }

    /**
     * Get score chart response
     */
    fun getScoreChartResponse(gameId: Int) = generateScoreChart(gameId)

    /**
     * Get win probability chart response
     */
    fun getWinProbabilityChartResponse(gameId: Int) = generateWinProbabilityChart(gameId)

    /**
     * Generate an ELO chart for all teams in a season
     * @param season The season number
     * @return ResponseEntity containing the chart image as PNG bytes
     */
    fun generateEloChart(season: Int): ResponseEntity<ByteArray> {
        try {
            Logger.info("Generating ELO chart for season $season")

            // Get all teams
            val teams = teamRepository.getAllActiveTeams()
            if (teams.isEmpty()) {
                return ResponseEntity(HttpStatus.NOT_FOUND)
            }

            // Get all game stats for the season
            val gameStatsList = gameStatsRepository.findBySeasonOrderByGameIdAsc(season)
            if (gameStatsList.isEmpty()) {
                return ResponseEntity(HttpStatus.NOT_FOUND)
            }

            // Group game stats by team and week
            val teamEloData = mutableMapOf<String, MutableMap<Int, Double>>()
            val teamColors = mutableMapOf<String, String>()

            // Initialize team data
            teams.forEach { team ->
                teamEloData[team.name ?: ""] = mutableMapOf()
                teamColors[team.name ?: ""] = team.primaryColor ?: "#000000"
            }

            // Process game stats to get ELO by week
            gameStatsList.forEach { gameStats ->
                val teamName = gameStats.team ?: return@forEach
                val week = gameStats.week ?: return@forEach
                val elo = gameStats.teamElo

                teamEloData[teamName]?.set(week, elo)
            }

            // Create the chart
            val chartImage = createEloChart(teamEloData, teamColors, season)

            // Save and return the chart
            val chartBytes = saveChartImage(chartImage, "elo_chart_season_$season")
            val headers = HttpHeaders()
            headers.contentType = MediaType.IMAGE_PNG
            headers.contentLength = chartBytes.size.toLong()

            return ResponseEntity.ok()
                .headers(headers)
                .body(chartBytes)
        } catch (e: Exception) {
            Logger.error("Error generating ELO chart for season $season: ${e.message}", e)
            return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    /**
     * Create an ELO chart showing team ELO progression by week
     */
    private fun createEloChart(
        teamEloData: Map<String, Map<Int, Double>>,
        teamColors: Map<String, String>,
        season: Int,
    ): BufferedImage {
        val width = 1200
        val height = 800
        val padding = 80
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Fill background
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, height)

        // Get data ranges
        val allWeeks = teamEloData.values.flatMap { it.keys }.distinct().sorted()
        val allElos = teamEloData.values.flatMap { it.values }
        val minElo = allElos.minOrNull() ?: 1500.0
        val maxElo = allElos.maxOrNull() ?: 1500.0
        val eloRange = maxElo - minElo
        val eloPadding = eloRange * 0.1

        val minWeek = allWeeks.minOrNull() ?: 1
        val maxWeek = allWeeks.maxOrNull() ?: 1
        val weekRange = maxWeek - minWeek

        // Draw title
        g2d.color = Color.BLACK
        g2d.font = Font("Arial", Font.BOLD, 24)
        val title = "Team ELO Progression - Season $season"
        val titleWidth = g2d.fontMetrics.stringWidth(title)
        g2d.drawString(title, (width - titleWidth) / 2, 30)

        // Draw axes
        g2d.color = Color.BLACK
        g2d.stroke = BasicStroke(2f)
        g2d.drawLine(padding, padding, padding, height - padding) // Y-axis
        g2d.drawLine(padding, height - padding, width - padding, height - padding) // X-axis

        // Draw Y-axis labels (ELO)
        g2d.font = Font("Arial", Font.PLAIN, 12)
        val eloStep = eloRange / 10
        for (i in 0..10) {
            val elo = minElo - eloPadding + (eloStep * i)
            val y = height - padding - (i * chartHeight / 10)
            val eloText = elo.toInt().toString()
            val textWidth = g2d.fontMetrics.stringWidth(eloText)
            g2d.drawString(eloText, padding - textWidth - 10, y + 5)
        }

        // Draw X-axis labels (Weeks)
        for (week in minWeek..maxWeek) {
            val x = padding + ((week - minWeek) * chartWidth / weekRange)
            val weekText = "W$week"
            val textWidth = g2d.fontMetrics.stringWidth(weekText)
            g2d.drawString(weekText, x - textWidth / 2, height - padding + 20)
        }

        // Draw grid lines
        g2d.color = Color.LIGHT_GRAY
        g2d.stroke = BasicStroke(1f)
        for (i in 0..10) {
            val y = height - padding - (i * chartHeight / 10)
            g2d.drawLine(padding, y, width - padding, y)
        }

        // Draw team lines
        g2d.stroke = BasicStroke(2f)
        teamEloData.forEach { (teamName, weekElos) ->
            if (weekElos.isNotEmpty()) {
                val color =
                    try {
                        Color.decode(teamColors[teamName] ?: "#000000")
                    } catch (e: NumberFormatException) {
                        Color.BLACK
                    }
                g2d.color = color

                val sortedWeeks = weekElos.keys.sorted()
                for (i in 0 until sortedWeeks.size - 1) {
                    val week1 = sortedWeeks[i]
                    val week2 = sortedWeeks[i + 1]
                    val elo1 = weekElos[week1] ?: continue
                    val elo2 = weekElos[week2] ?: continue

                    val x1 = padding + ((week1 - minWeek) * chartWidth / weekRange)
                    val y1 = height - padding - ((elo1 - minElo + eloPadding) * chartHeight / (eloRange + 2 * eloPadding))
                    val x2 = padding + ((week2 - minWeek) * chartWidth / weekRange)
                    val y2 = height - padding - ((elo2 - minElo + eloPadding) * chartHeight / (eloRange + 2 * eloPadding))

                    g2d.drawLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())
                }

                // Draw team name at the end of the line
                val lastWeek = sortedWeeks.lastOrNull()
                if (lastWeek != null) {
                    val lastElo = weekElos[lastWeek] ?: return@forEach
                    val x = padding + ((lastWeek - minWeek) * chartWidth / weekRange)
                    val y = height - padding - ((lastElo - minElo + eloPadding) * chartHeight / (eloRange + 2 * eloPadding))
                    g2d.drawString(teamName, x + 5, y.toInt())
                }
            }
        }

        g2d.dispose()
        return image
    }

    /**
     * Get ELO chart response
     */
    fun getEloChartResponse(season: Int) = generateEloChart(season)
}
