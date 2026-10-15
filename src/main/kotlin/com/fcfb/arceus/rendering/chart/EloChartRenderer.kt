package com.fcfb.arceus.rendering.chart

import org.springframework.stereotype.Component
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

@Component
class EloChartRenderer : ChartRendererBase() {
    fun render(
        teamEloData: Map<String, Map<Int, Double>>,
        teamColors: Map<String, Pair<String?, String?>>,
        season: Int,
    ): BufferedImage {
        val width = 1200
        val height = 800
        val padding = 80
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d = image.createGraphics()

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val background = Color.WHITE
        g2d.color = background
        g2d.fillRect(0, 0, width, height)

        val allWeeks = teamEloData.values.flatMap { it.keys }.distinct().sorted()
        val allElos = teamEloData.values.flatMap { it.values }
        val minElo = allElos.minOrNull() ?: 1500.0
        val maxElo = allElos.maxOrNull() ?: 1500.0
        val eloRange = maxElo - minElo
        val eloPadding = eloRange * 0.1

        val minWeek = allWeeks.minOrNull() ?: 1
        val maxWeek = allWeeks.maxOrNull() ?: 1
        val weekRange = maxWeek - minWeek

        g2d.color = Color.BLACK
        g2d.font = Font("Arial", Font.BOLD, 24)
        val title = "Team ELO Progression - Season $season"
        val titleWidth = g2d.fontMetrics.stringWidth(title)
        g2d.drawString(title, (width - titleWidth) / 2, 30)

        g2d.color = Color.BLACK
        g2d.stroke = BasicStroke(2f)
        g2d.drawLine(padding, padding, padding, height - padding)
        g2d.drawLine(padding, height - padding, width - padding, height - padding)

        g2d.font = Font("Arial", Font.PLAIN, 12)
        val eloStep = eloRange / 10
        for (i in 0..10) {
            val elo = minElo - eloPadding + (eloStep * i)
            val y = height - padding - (i * chartHeight / 10)
            val eloText = elo.toInt().toString()
            val textWidth = g2d.fontMetrics.stringWidth(eloText)
            g2d.drawString(eloText, padding - textWidth - 10, y + 5)
        }

        for (week in minWeek..maxWeek) {
            val x = padding + ((week - minWeek) * chartWidth / weekRange)
            val weekText = "W$week"
            val textWidth = g2d.fontMetrics.stringWidth(weekText)
            g2d.drawString(weekText, x - textWidth / 2, height - padding + 20)
        }

        g2d.color = Color.LIGHT_GRAY
        g2d.stroke = BasicStroke(1f)
        for (i in 0..10) {
            val y = height - padding - (i * chartHeight / 10)
            g2d.drawLine(padding, y, width - padding, y)
        }

        g2d.stroke = BasicStroke(2f)
        teamEloData.forEach { (teamName, weekElos) ->
            if (weekElos.isNotEmpty()) {
                val (primaryHex, secondaryHex) = teamColors[teamName] ?: (null to null)
                val color =
                    ChartColor().select(
                        primaryHex?.let { parseColor(it) },
                        secondaryHex?.let { parseColor(it) },
                        background,
                    )
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
}
