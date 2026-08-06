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

abstract class ChartRendererBase(
    private val imagePath: String,
) {
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

        val maxQuarter = allPlays.maxOfOrNull { it.quarter } ?: quarter
        val isQuarterFinished =
            when {
                quarter < maxQuarter -> true
                quarter == 4 -> {
                    val remainingTime = play.clock - play.playTime - play.runoffTime
                    remainingTime <= 0
                }
                else -> false
            }

        val assumedPlaysInQuarter =
            when {
                isQuarterFinished -> playsInQuarter.size
                playsInQuarter.size > 30 -> playsInQuarter.size
                else -> 30
            }

        val quarterProgress = (quarter - 1).toFloat() / quartersToShow
        val playProgressInQuarter =
            if (assumedPlaysInQuarter > 1) {
                playIndexInQuarter.toFloat() / (assumedPlaysInQuarter - 1)
            } else {
                0f
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

        val maxQuarter = plays.maxOfOrNull { it.quarter } ?: 4
        val quartersToShow = if (maxQuarter > 4) maxQuarter else 4

        for (quarter in 1..quartersToShow) {
            val quarterProgress = quarter.toFloat() / quartersToShow
            val x = padding + (quarterProgress * chartWidth).toInt()

            g.drawLine(x, padding, x, padding + chartHeight)

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
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g.drawImage(logoImage, x, y, width, height, null)
            } catch (e: IOException) {
                Logger.error("Error loading logo for ${team.name}: ${e.message}")
                g.font = Font("Arial", Font.BOLD, 16)
                g.color = parseColor(team.primaryColor ?: "#000000")
                val abbreviation = team.abbreviation ?: "TEAM"
                val textWidth = g.fontMetrics.stringWidth(abbreviation)
                g.drawString(abbreviation, x + (width - textWidth) / 2, y + height / 2 + 6)
            }
        } else {
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

        val directory = File("$imagePath/charts")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        ImageIO.write(image, "png", outputFile)
        return outputFile.readBytes()
    }
}
