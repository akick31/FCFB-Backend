package com.fcfb.arceus.service.fcfb.chart

import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.util.Logger
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

/**
 * Base class with shared drawing helpers used across multiple chart renderers
 */
abstract class ChartRendererBase(
    private val imagePath: String,
) {
    /**
     * Calculate X position for a play based on play-by-play positioning
     * For unfinished quarters, assumes 30 plays per quarter unless more than 30 plays exist
     */
    protected fun calculatePlayXPosition(
        play: Play,
        allPlays: List<Play>,
        quartersToShow: Int,
        chartWidth: Int,
        padding: Int,
    ): Int {
        val quarter = play.quarter
        val playsInQuarter = allPlays.filter { it.quarter == quarter }
        val playIndexInQuarter = playsInQuarter.indexOf(play)

        // Determine if this quarter is finished
        val maxQuarter = allPlays.maxOfOrNull { it.quarter } ?: quarter
        val isQuarterFinished =
            when {
                quarter < maxQuarter -> true // There are plays in a higher quarter
                quarter == 4 -> {
                    // 4th quarter is finished when clock - play_time - runoff_time <= 0
                    val remainingTime = play.clock - play.playTime - play.runoffTime
                    remainingTime <= 0
                }
                else -> false // Other quarters are not finished if they're the max quarter
            }

        // For finished quarters, use actual number of plays
        // For unfinished quarters, assume 30 plays unless we have more than 30
        val assumedPlaysInQuarter =
            when {
                isQuarterFinished -> playsInQuarter.size
                playsInQuarter.size > 30 -> playsInQuarter.size
                else -> 30
            }

        // Calculate position within the quarter
        val quarterProgress = (quarter - 1).toFloat() / quartersToShow
        val playProgressInQuarter =
            if (assumedPlaysInQuarter > 1) {
                playIndexInQuarter.toFloat() / (assumedPlaysInQuarter - 1)
            } else {
                0f // Handle edge case of single play in quarter
            }

        return padding + ((quarterProgress + playProgressInQuarter / quartersToShow) * chartWidth).toInt()
    }

    protected fun drawQuarterDivisions(
        g: Graphics2D,
        plays: List<Play>,
        padding: Int,
        chartWidth: Int,
        chartHeight: Int,
    ) {
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(1f)

        // Always show full 4 quarters, plus OT if present
        val maxQuarter = plays.maxOfOrNull { it.quarter } ?: 4
        val quartersToShow = if (maxQuarter > 4) maxQuarter else 4

        for (quarter in 1..quartersToShow) {
            val quarterProgress = quarter.toFloat() / quartersToShow
            val x = padding + (quarterProgress * chartWidth).toInt()

            // Draw vertical line at quarter boundary
            g.drawLine(x, padding, x, padding + chartHeight)

            // Draw quarter label
            g.font = Font("Arial", Font.BOLD, 12)
            val quarterLabel = "${quarter}Q"
            val labelWidth = g.fontMetrics.stringWidth(quarterLabel)
            g.drawString(quarterLabel, x - labelWidth / 2, padding - 10)
        }
    }

    protected fun drawTeamLogo(
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

    protected fun parseColor(colorString: String): Color {
        return try {
            Color.decode(colorString)
        } catch (e: NumberFormatException) {
            Color.BLACK
        }
    }

    protected fun saveChartImage(
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
}
