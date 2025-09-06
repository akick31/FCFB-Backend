package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.PlayRepository
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
        val width = 800
        val height = 600
        val padding = 60
        val chartWidth = width - (padding * 2)
        val chartHeight = height - (padding * 2)

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // White background
        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)

        // Black border
        g.color = Color.BLACK
        g.stroke = BasicStroke(2f)
        g.drawRect(padding, padding, chartWidth, chartHeight)

        // Title
        g.font = Font("Arial", Font.BOLD, 20)
        g.color = Color.BLACK
        val title = "${game.awayTeam} @ ${game.homeTeam}"
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 30)

        // Get team colors
        val homeColor = parseColor(homeTeam.primaryColor ?: "#000000")
        val awayColor = parseColor(awayTeam.primaryColor ?: "#000000")

        // Find max score for scaling
        val maxScore =
            maxOf(
                plays.maxOfOrNull { it.homeScore } ?: 0,
                plays.maxOfOrNull { it.awayScore } ?: 0,
            )

        // Draw quarter divisions
        drawQuarterDivisions(g, plays, padding, chartWidth, chartHeight)

        // Draw score lines
        drawScoreLines(g, plays, homeColor, awayColor, padding, chartWidth, chartHeight, maxScore)

        // Draw axes
        drawScoreAxes(g, padding, chartWidth, chartHeight, maxScore)

        // Draw legend
        drawScoreLegend(g, homeTeam, awayTeam, homeColor, awayColor, width, height)

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
        val width = 800
        val height = 600
        val padding = 60
        val chartWidth = width - (padding * 2)
        val chartHeight = height - (padding * 2)

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // White background
        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)

        // Black border
        g.color = Color.BLACK
        g.stroke = BasicStroke(2f)
        g.drawRect(padding, padding, chartWidth, chartHeight)

        // Title
        g.font = Font("Arial", Font.BOLD, 20)
        g.color = Color.BLACK
        val title = "${game.awayTeam} @ ${game.homeTeam} - Win Probability"
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 30)

        // Get team colors
        val homeColor = parseColor(homeTeam.primaryColor ?: "#000000")
        val awayColor = parseColor(awayTeam.primaryColor ?: "#000000")

        // Draw 50% line
        val centerY = padding + (chartHeight / 2)
        g.color = Color.GRAY
        g.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(5f, 5f), 0f)
        g.drawLine(padding, centerY, padding + chartWidth, centerY)

        // Draw quarter divisions
        drawQuarterDivisions(g, plays, padding, chartWidth, chartHeight)

        // Draw win probability line
        drawWinProbabilityLine(g, plays, homeColor, awayColor, padding, chartWidth, chartHeight)

        // Draw axes
        drawWinProbabilityAxes(g, padding, chartWidth, chartHeight, homeTeam, awayTeam)

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
            val x1 = padding + (i.toFloat() / (totalPlays - 1) * chartWidth).toInt()
            val x2 = padding + ((i + 1).toFloat() / (totalPlays - 1) * chartWidth).toInt()
            val y1 = padding + chartHeight - ((plays[i].homeScore.toFloat() / maxScore) * chartHeight).toInt()
            val y2 = padding + chartHeight - ((plays[i + 1].homeScore.toFloat() / maxScore) * chartHeight).toInt()
            g.drawLine(x1, y1, x2, y2)
        }

        // Draw away team line
        g.color = awayColor
        for (i in 0 until totalPlays - 1) {
            val x1 = padding + (i.toFloat() / (totalPlays - 1) * chartWidth).toInt()
            val x2 = padding + ((i + 1).toFloat() / (totalPlays - 1) * chartWidth).toInt()
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
        plays: List<Play>,
        homeColor: Color,
        awayColor: Color,
        padding: Int,
        chartWidth: Int,
        chartHeight: Int,
    ) {
        g.stroke = BasicStroke(3f)

        val totalPlays = plays.size
        if (totalPlays < 2) return

        for (i in 0 until totalPlays - 1) {
            // Get win probabilities for each team using the service method
            // This ensures consistent calculation logic across the application
            val (homeWinProb1, awayWinProb1) = winProbabilityService.getWinProbabilityForEachTeam(plays[i])
            val (homeWinProb2, awayWinProb2) = winProbabilityService.getWinProbabilityForEachTeam(plays[i + 1])

            // Use home team win probability for the chart (0.0 to 1.0)
            // Above 0.5 = home team advantage, below 0.5 = away team advantage
            var winProb1 = homeWinProb1
            var winProb2 = homeWinProb2

            // Normalize win probability values to ensure they're between 0.0 and 1.0
            // Handle case where win probability might be stored as percentage (0-100)
            if (winProb1 > 1.0) winProb1 = winProb1 / 100.0
            if (winProb2 > 1.0) winProb2 = winProb2 / 100.0

            // Clamp values to valid range
            winProb1 = winProb1.coerceIn(0.0, 1.0)
            winProb2 = winProb2.coerceIn(0.0, 1.0)

            val x1 = padding + (i.toFloat() / (totalPlays - 1) * chartWidth).toInt()
            val x2 = padding + ((i + 1).toFloat() / (totalPlays - 1) * chartWidth).toInt()
            val y1 = padding + chartHeight - ((winProb1 * chartHeight).toInt())
            val y2 = padding + chartHeight - ((winProb2 * chartHeight).toInt())

            // Draw line segment by segment, changing color based on possession and win probability
            drawWinProbabilitySegment(
                g,
                x1,
                y1,
                x2,
                y2,
                winProb1,
                winProb2,
                plays[i].possession,
                plays[i + 1].possession,
                homeColor,
                awayColor,
            )
        }
    }

    /**
     * Draw a segment of the win probability line, changing color based on possession and win probability
     */
    private fun drawWinProbabilitySegment(
        g: Graphics2D,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        winProb1: Double,
        winProb2: Double,
        possession1: com.fcfb.arceus.enums.team.TeamSide,
        possession2: com.fcfb.arceus.enums.team.TeamSide,
        homeColor: Color,
        awayColor: Color,
    ) {
        // Win probability is calculated from the perspective of whichever team has possession
        // > 0.5 = possessing team advantage, <= 0.5 = defending team advantage

        // Determine colors based on possession
        val color1 = if (possession1 == com.fcfb.arceus.enums.team.TeamSide.HOME) homeColor else awayColor
        val color2 = if (possession2 == com.fcfb.arceus.enums.team.TeamSide.HOME) homeColor else awayColor

        // If both points have the same possession and are on the same side of 50%, use single color
        if (possession1 == possession2 && ((winProb1 > 0.5 && winProb2 > 0.5) || (winProb1 <= 0.5 && winProb2 <= 0.5))) {
            g.color = color1
            g.drawLine(x1, y1, x2, y2)
        } else {
            // Line crosses 50% or possession changes, calculate exact crossing point
            val crossingPoint = (0.5 - winProb1) / (winProb2 - winProb1)
            val crossingX = x1 + (crossingPoint * (x2 - x1)).toInt()
            val crossingY = y1 + (crossingPoint * (y2 - y1)).toInt()

            // Draw first half
            g.color = color1
            g.drawLine(x1, y1, crossingX, crossingY)

            // Draw second half
            g.color = color2
            g.drawLine(crossingX, crossingY, x2, y2)
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
    ) {
        g.color = Color.BLACK
        g.stroke = BasicStroke(2f)
        g.font = Font("Arial", Font.PLAIN, 12)

        // Y-axis (Score)
        g.drawLine(padding, padding, padding, padding + chartHeight)
        g.drawLine(padding, padding + chartHeight, padding + chartWidth, padding + chartHeight)

        // Y-axis labels
        for (i in 0..maxScore step maxOf(1, maxScore / 10)) {
            val y = padding + chartHeight - ((i.toFloat() / maxScore) * chartHeight).toInt()
            g.drawString(i.toString(), padding - 30, y + 5)
        }

        // Axis labels
        g.font = Font("Arial", Font.BOLD, 14)
        g.drawString("Score", 10, padding + chartHeight / 2)
        g.drawString("Play Number", padding + chartWidth / 2 - 50, padding + chartHeight + 40)
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
    ) {
        g.color = Color.BLACK
        g.stroke = BasicStroke(2f)

        // Y-axis (Win Probability)
        g.drawLine(padding, padding, padding, padding + chartHeight)
        g.drawLine(padding, padding + chartHeight, padding + chartWidth, padding + chartHeight)

        // Draw team logos instead of Y-axis labels
        drawTeamLogo(g, homeTeam, 10, padding + 20, 40, 40)
        drawTeamLogo(g, awayTeam, 10, padding + chartHeight - 60, 40, 40)

        // X-axis label
        g.font = Font("Arial", Font.BOLD, 14)
        g.color = Color.BLACK
        g.drawString("Play Number", padding + chartWidth / 2 - 50, padding + chartHeight + 40)
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
                g.drawImage(logoImage, x, y, width, height, null)
            } catch (e: IOException) {
                Logger.error("Error loading logo for ${team.name}: ${e.message}")
                // Fallback to team abbreviation if logo fails to load
                g.font = Font("Arial", Font.BOLD, 12)
                g.color = parseColor(team.primaryColor ?: "#000000")
                g.drawString(team.abbreviation ?: "TEAM", x, y + height / 2)
            }
        } else {
            // Fallback to team abbreviation if no logo URL
            g.font = Font("Arial", Font.BOLD, 12)
            g.color = parseColor(team.primaryColor ?: "#000000")
            g.drawString(team.abbreviation ?: "TEAM", x, y + height / 2)
        }
    }

    /**
     * Draw legend for score chart
     */
    private fun drawScoreLegend(
        g: Graphics2D,
        homeTeam: Team,
        awayTeam: Team,
        homeColor: Color,
        awayColor: Color,
        width: Int,
        height: Int,
    ) {
        g.font = Font("Arial", Font.BOLD, 14)

        val legendY = height - 30
        val legendX = width - 200

        // Home team legend
        g.color = homeColor
        g.fillRect(legendX, legendY - 10, 15, 3)
        g.color = Color.BLACK
        g.drawString(homeTeam.abbreviation ?: "HOME", legendX + 20, legendY)

        // Away team legend
        g.color = awayColor
        g.fillRect(legendX + 100, legendY - 10, 15, 3)
        g.color = Color.BLACK
        g.drawString(awayTeam.abbreviation ?: "AWAY", legendX + 120, legendY)
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
}
