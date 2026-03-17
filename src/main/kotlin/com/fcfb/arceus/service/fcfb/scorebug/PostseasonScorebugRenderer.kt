package com.fcfb.arceus.service.fcfb.scorebug

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Component
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URI
import javax.imageio.ImageIO

@Component
class PostseasonScorebugRenderer : ScorebugRendererBase() {
    override fun render(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage {
        val isPlayoff =
            game.gameType == GameType.PLAYOFFS || game.gameType == GameType.NATIONAL_CHAMPIONSHIP

        val width = 360
        val headerHeight = 60
        val height = 400 + headerHeight
        val rowHeight = 70
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        g.stroke = BasicStroke(2f)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.composite = AlphaComposite.Clear
        g.fillRect(0, 0, width, height)
        g.composite = AlphaComposite.Src

        // Draw header with game logo
        drawPostseasonHeader(g, game, homeTeam, awayTeam, width, headerHeight, isPlayoff)

        // Offset all existing sections by headerHeight
        val adjustedRowHeightForTeamName = rowHeight - 5
        val adjustedRowHeightForTeamScore = rowHeight + 5

        drawTeamScoreSection(g, game, awayTeam, 65 + headerHeight, adjustedRowHeightForTeamScore)
        drawTeamScoreSection(g, game, homeTeam, 205 + headerHeight, adjustedRowHeightForTeamScore)

        val dividerColor = if (isPlayoff) Color(212, 175, 55) else Color.WHITE
        drawTeamNameSection(g, game, awayTeam, width, 0 + headerHeight, adjustedRowHeightForTeamName, dividerColor)
        drawTeamNameSection(g, game, homeTeam, width, 140 + headerHeight, adjustedRowHeightForTeamName, dividerColor)

        val accentColor = if (isPlayoff) Color(212, 175, 55) else Color.LIGHT_GRAY
        if (game.gameStatus != GameStatus.FINAL) {
            drawPostseasonClockSection(g, rowHeight - 10, game, homeTeam, awayTeam, headerHeight, accentColor)
            drawPostseasonDownSection(g, rowHeight - 10, game, headerHeight, accentColor)
        } else {
            drawPostseasonFinalSection(g, (rowHeight - 10) * 2, game, headerHeight, accentColor)
        }

        // Draw border - gold for playoffs, white otherwise
        if (isPlayoff) {
            drawPostseasonBorder(g, width, height, Color(212, 175, 55))
        } else {
            drawBorder(g, width, height)
        }

        g.dispose()
        return scaleImage(image, width, height)
    }

    private fun drawPostseasonHeader(
        g: Graphics2D,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
        width: Int,
        headerHeight: Int,
        isPlayoff: Boolean,
    ) {
        val bgColor = if (isPlayoff) Color(25, 25, 25) else Color(40, 40, 40)
        g.color = bgColor
        g.fillRect(0, 0, width, headerHeight)

        val textColor = Color.WHITE
        val gameName = game.postseasonGameName ?: game.gameType?.description ?: "Postseason"
        val logoSize = headerHeight - 10

        // Try to load the postseason game logo (left-aligned)
        var logoLoaded = false
        val gameLogoUrl = game.postseasonGameLogo
        if (gameLogoUrl != null) {
            try {
                val gameLogo = ImageIO.read(URI(gameLogoUrl).toURL())
                val logoWidth = (gameLogo.width.toDouble() / gameLogo.height * logoSize).toInt()
                g.composite = AlphaComposite.SrcOver
                g.drawImage(gameLogo, 8, 5, logoWidth, logoSize, null)
                logoLoaded = true

                g.color = textColor
                g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 22f)
                val ascent = g.fontMetrics.ascent
                g.drawString(gameName, logoWidth + 18, headerHeight / 2 + ascent / 2)
                return
            } catch (e: IOException) {
                Logger.error("Error loading postseason game logo: ${e.message}")
            }
        }

        // Fallback for conference championship: conference logo left, game name right
        if (!logoLoaded && game.gameType == GameType.CONFERENCE_CHAMPIONSHIP) {
            val confLogoSize = logoSize
            val conf = homeTeam.conference ?: awayTeam.conference
            if (conf?.logoUrl != null) {
                drawConferenceLogo(g, conf, 8, 5, confLogoSize, confLogoSize)
                g.color = textColor
                g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 22f)
                val ascent = g.fontMetrics.ascent
                g.drawString(gameName, confLogoSize + 18, headerHeight / 2 + ascent / 2)
                return
            }
        }

        // Fallback: game name centered
        g.color = textColor
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 24f)
        val ascent = g.fontMetrics.ascent
        val textWidth = g.fontMetrics.stringWidth(gameName)
        g.drawString(gameName, (width - textWidth) / 2, headerHeight / 2 + ascent / 2)
    }

    private fun drawConferenceLogo(
        g: Graphics2D,
        conference: Conference?,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val logoUrl = conference?.logoUrl ?: return
        try {
            val logo = ImageIO.read(URI(logoUrl).toURL())
            g.composite = AlphaComposite.SrcOver
            g.drawImage(logo, x, y, width, height, null)
        } catch (e: IOException) {
            Logger.error("Error loading conference logo for ${conference.name}: ${e.message}")
        }
    }

    private fun drawPostseasonBorder(
        g: Graphics2D,
        width: Int,
        height: Int,
        borderColor: Color,
    ) {
        g.color = borderColor
        g.stroke = BasicStroke(4f)
        g.drawLine(0, 0, width, 0)
        g.drawLine(0, 0, 0, height)
        g.drawLine(width - 1, 0, width - 3, height)
        g.drawLine(0, height - 1, width, height - 3)
    }

    private fun drawPostseasonClockSection(
        g: Graphics2D,
        rowHeight: Int,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
        headerOffset: Int,
        accentColor: Color = Color.LIGHT_GRAY,
    ) {
        val rowY = 280 + headerOffset

        var xPos = 0
        g.color = Color(255, 255, 255)
        g.fillRect(xPos, rowY, 126, rowHeight)

        val quarterText = getQuarterText(game.quarter)
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 40f)
        val quarterTextAscent = g.fontMetrics.ascent
        g.color = Color.BLACK
        g.drawString(
            quarterText,
            xPos + (100 - g.fontMetrics.stringWidth(quarterText)) / 2,
            rowY + rowHeight / 2 + quarterTextAscent / 2,
        )

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

        val verticalLineX = 100
        g.color = accentColor
        g.stroke = BasicStroke(3f)
        g.drawLine(verticalLineX, rowY + rowHeight, verticalLineX, rowY + (rowHeight / 2))

        val verticalLineX2 = 260
        g.color = accentColor
        g.stroke = BasicStroke(3f)
        g.drawLine(verticalLineX2, rowY + rowHeight, verticalLineX2, rowY + (rowHeight / 2))

        g.color = accentColor
        g.stroke = BasicStroke(3f)
        g.drawLine(0, rowY, 360, rowY)

        g.color = accentColor
        g.stroke = BasicStroke(3f)
        g.drawLine(0, rowY + rowHeight, 360, rowY + rowHeight)
    }

    private fun drawPostseasonDownSection(
        g: Graphics2D,
        rowHeight: Int,
        game: Game,
        headerOffset: Int,
        accentColor: Color = Color.LIGHT_GRAY,
    ) {
        val rowY = 340 + headerOffset
        g.color = Color(255, 255, 255)
        g.fillRect(0, rowY, 360, rowHeight)
        g.color = accentColor
        g.stroke = BasicStroke(3f)
        g.drawRect(0, rowY, 360, rowHeight)

        val downDistanceText = getDownDistanceText(game)
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 43f)
        val ascent = g.fontMetrics.ascent
        g.color = Color.BLACK
        g.drawString(downDistanceText, 10, rowY + rowHeight / 2 + ascent / 2)
    }

    private fun drawPostseasonFinalSection(
        g: Graphics2D,
        rowHeight: Int,
        game: Game,
        headerOffset: Int,
        accentColor: Color = Color.LIGHT_GRAY,
    ) {
        val rowY = 280 + headerOffset
        g.color = Color(255, 255, 255)
        g.fillRect(0, rowY, 360, rowHeight)
        g.color = accentColor
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
