package com.fcfb.arceus.service.fcfb.scorebug

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

@Component
class EspnScorebugRenderer : ScorebugRendererBase() {
    override fun render(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage {
        val width = 360
        val height = 400
        val rowHeight = 70
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        g.stroke = BasicStroke(2f)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.composite = AlphaComposite.Clear
        g.fillRect(0, 0, width, height)
        g.composite = AlphaComposite.Src

        val adjustedRowHeightForTeamName = rowHeight - 5
        val adjustedRowHeightForTeamScore = rowHeight + 5

        drawTeamScoreSection(g, game, awayTeam, 65, adjustedRowHeightForTeamScore)
        drawTeamScoreSection(g, game, homeTeam, 205, adjustedRowHeightForTeamScore)

        drawTeamNameSection(g, game, awayTeam, width, 0, adjustedRowHeightForTeamName)
        drawTeamNameSection(g, game, homeTeam, width, 140, adjustedRowHeightForTeamName)

        if (game.gameStatus != GameStatus.FINAL) {
            drawClockInformationSection(g, rowHeight - 10, game, homeTeam, awayTeam)
            drawDownAndDistanceSection(g, rowHeight - 10, game)
        } else {
            drawFinalSection(g, (rowHeight - 10) * 2, game)
        }

        drawBorder(g, width, height)
        g.dispose()

        return scaleImage(image, width, height)
    }

    private fun drawClockInformationSection(
        g: Graphics2D,
        rowHeight: Int,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ) {
        val rowY = 280

        // Draw Quarter Section
        var xPos = 0
        g.color = Color(255, 255, 255)
        g.fillRect(xPos, rowY, 126, rowHeight)
        g.color = Color.LIGHT_GRAY

        val quarterText = getQuarterText(game.quarter)
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 40f)
        val quarterTextAscent = g.fontMetrics.ascent
        g.color = Color.BLACK
        g.drawString(
            quarterText,
            xPos + (100 - g.fontMetrics.stringWidth(quarterText)) / 2,
            rowY + rowHeight / 2 + quarterTextAscent / 2,
        )

        // Draw Clock Section
        xPos = 100
        g.color = Color(255, 255, 255)
        g.fillRect(xPos, rowY, 160, rowHeight)

        val clockText = getClockText(game.quarter, game.clock)
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 40f)
        val clockTextAscent = g.fontMetrics.ascent
        g.color = Color.BLACK
        g.drawString(
            clockText,
            xPos + (160 - g.fontMetrics.stringWidth(clockText)) / 2,
            rowY + rowHeight / 2 + clockTextAscent / 2,
        )

        // Draw Ball Location Section
        xPos = 260
        g.color = Color(255, 255, 255)
        g.fillRect(xPos, rowY, 100, rowHeight)

        val ballLocationText =
            getBallLocationText(
                homeTeam.name ?: "",
                awayTeam.name ?: "",
                homeTeam.abbreviation,
                awayTeam.abbreviation,
                game.ballLocation,
                game.possession,
            )
        var fontSize = 29
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, fontSize.toFloat())

        var textWidth = g.fontMetrics.stringWidth(ballLocationText)
        val ballLocationTextAscent = g.fontMetrics.ascent

        while (textWidth > 85 && fontSize > 10) {
            fontSize -= 2
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, fontSize.toFloat())
            textWidth = g.fontMetrics.stringWidth(ballLocationText)
        }

        g.color = Color.BLACK
        g.drawString(ballLocationText, xPos + (100 - textWidth) / 2, rowY + (rowHeight) / 2 + ballLocationTextAscent / 2)

        // Vertical line to separate Quarter and Clock sections
        val verticalLineX = 100
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(3f)
        g.drawLine(verticalLineX, rowY + rowHeight, verticalLineX, rowY + (rowHeight / 2))

        // Vertical line to separate Clock and Ball Location sections
        val verticalLineX2 = 260
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(3f)
        g.drawLine(verticalLineX2, rowY + rowHeight, verticalLineX2, rowY + (rowHeight / 2))

        // Top horizontal line
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(3f)
        g.drawLine(0, rowY, 360, rowY)

        // Bottom horizontal line
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(3f)
        g.drawLine(0, rowY + rowHeight, 360, rowY + rowHeight)
    }

    private fun drawDownAndDistanceSection(
        g: Graphics2D,
        rowHeight: Int,
        game: Game,
    ) {
        val rowY = 340
        g.color = Color(255, 255, 255)
        g.fillRect(0, rowY, 360, rowHeight)
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(3f)
        g.drawRect(0, rowY, 360, rowHeight)

        val downDistanceText = getDownDistanceText(game)
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 43f)
        val ascent = g.fontMetrics.ascent
        g.color = Color.BLACK
        g.drawString(downDistanceText, 10, rowY + rowHeight / 2 + ascent / 2)
    }

    private fun drawFinalSection(
        g: Graphics2D,
        rowHeight: Int,
        game: Game,
    ) {
        val rowY = 280
        g.color = Color(255, 255, 255)
        g.fillRect(0, rowY, 360, rowHeight)
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(3f)
        g.drawRect(0, rowY, 360, rowHeight)

        var finalText = "FINAL"
        if (game.quarter >= 6) {
            finalText += "/${game.quarter - 4} OT"
        } else if (game.quarter == 5) {
            finalText += "/OT"
        }
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.BOLD, 43f)
        val ascent = g.fontMetrics.ascent
        g.color = Color.BLACK
        g.drawString(finalText, 10, rowY + rowHeight / 2 + ascent / 2)
    }
}
