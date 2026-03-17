package com.fcfb.arceus.service.fcfb.scorebug

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Component
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URI
import javax.imageio.ImageIO

@Component
class FoxScorebugRenderer : ScorebugRendererBase() {
    override fun render(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage {
        val width = 700
        val height = 170
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        g.stroke = BasicStroke(2f)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.composite = AlphaComposite.Clear
        g.fillRect(0, 0, width, height)
        g.composite = AlphaComposite.Src

        val teamBarY = 14
        val teamBarHeight = 90
        val halfWidth = width / 2
        val infoBarY = teamBarY + teamBarHeight + 4
        val infoBarHeight = 50

        // Draw timeout dots above team bars
        drawFoxTimeoutDots(g, game, awayTeam, 0, teamBarY, halfWidth - 2, isHome = false)
        drawFoxTimeoutDots(g, game, homeTeam, halfWidth + 2, teamBarY, halfWidth - 2, isHome = true)

        // Draw away team section (left half)
        drawFoxTeamSection(g, game, awayTeam, 0, teamBarY, halfWidth - 2, teamBarHeight, isHome = false)
        // Draw home team section (right half)
        drawFoxTeamSection(g, game, homeTeam, halfWidth + 2, teamBarY, halfWidth - 2, teamBarHeight, isHome = true)

        // Draw possession indicator between the two score sections
        if (game.gameStatus != GameStatus.FINAL) {
            val dotSize = 10
            val dotX = halfWidth - dotSize / 2
            val dotY = teamBarY + teamBarHeight / 2 - dotSize / 2 - 5
            g.color = Color(255, 165, 0) // Orange
            g.fillOval(dotX, dotY, dotSize, dotSize)
        }

        // Draw info bar
        if (game.gameStatus != GameStatus.FINAL) {
            drawFoxInfoBar(g, infoBarY, infoBarHeight, width, game, homeTeam, awayTeam)
        } else {
            drawFoxFinalBar(g, infoBarY, infoBarHeight, width, game)
        }

        g.dispose()
        return scaleImage(image, width, height)
    }

    private fun drawFoxTeamSection(
        g: Graphics2D,
        game: Game,
        team: Team,
        xPos: Int,
        yPos: Int,
        sectionWidth: Int,
        sectionHeight: Int,
        isHome: Boolean,
    ) {
        val teamColor = Color.decode(team.primaryColor)
        val darkerColor = teamColor.darker()

        val gradient =
            LinearGradientPaint(
                Point2D.Float(xPos.toFloat(), yPos.toFloat()),
                Point2D.Float(xPos.toFloat(), (yPos + sectionHeight).toFloat()),
                floatArrayOf(0f, 1f),
                arrayOf(teamColor, darkerColor),
            )
        g.paint = gradient
        g.fillRect(xPos, yPos, sectionWidth, sectionHeight)

        val score = if (isHome) game.homeScore.toString() else game.awayScore.toString()
        val ranking = if (isHome) game.homeTeamRank else game.awayTeamRank
        val record =
            if (isHome) {
                if (game.gameStatus == GameStatus.FINAL) {
                    if (game.homeScore > game.awayScore) {
                        "${game.homeWins?.plus(1)}-${game.homeLosses}"
                    } else {
                        "${game.homeWins}-${game.homeLosses?.plus(1)}"
                    }
                } else {
                    "${game.homeWins}-${game.homeLosses}"
                }
            } else {
                if (game.gameStatus == GameStatus.FINAL) {
                    if (game.awayScore > game.homeScore) {
                        "${game.awayWins?.plus(1)}-${game.awayLosses}"
                    } else {
                        "${game.awayWins}-${game.awayLosses?.plus(1)}"
                    }
                } else {
                    "${game.awayWins}-${game.awayLosses}"
                }
            }

        // Load and draw team logo
        val logoSize = 65
        val logoUrl = team.scorebugLogo
        if (logoUrl != null) {
            try {
                val logoImage = ImageIO.read(URI(logoUrl).toURL())
                val logoX = if (isHome) xPos + sectionWidth - logoSize - 8 else xPos + 8
                val logoY = yPos + (sectionHeight - logoSize) / 2
                g.drawImage(logoImage, logoX, logoY, logoSize, logoSize, null)
            } catch (e: IOException) {
                Logger.error("Error loading logo for ${team.name}: ${e.message}")
            }
        }

        g.color = Color.WHITE

        if (!isHome) {
            // Away team: [LOGO | NAME rank | SCORE]
            val textStartX = xPos + logoSize + 20

            var teamName = team.abbreviation ?: team.name
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 38f)
            if (g.fontMetrics.stringWidth(teamName) > sectionWidth - logoSize - 120) {
                teamName = team.abbreviation ?: teamName
            }
            g.drawString(teamName, textStartX, yPos + 40)

            if (ranking != null && ranking > 0) {
                val nameWidth = g.fontMetrics.stringWidth(teamName)
                g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 26f)
                g.drawString("#$ranking", textStartX + nameWidth + 8, yPos + 40)
            }

            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 24f)
            g.color = Color(255, 255, 255, 200)
            g.drawString(record, textStartX, yPos + 68)

            g.color = Color.WHITE
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 60f)
            val scoreWidth = g.fontMetrics.stringWidth(score)
            g.drawString(score, xPos + sectionWidth - scoreWidth - 12, yPos + sectionHeight / 2 + 20)
        } else {
            // Home team: [SCORE | rank NAME | LOGO]
            val textEndX = xPos + sectionWidth - logoSize - 20

            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 60f)
            g.drawString(score, xPos + 12, yPos + sectionHeight / 2 + 20)

            var teamName = team.abbreviation ?: team.name
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 38f)
            if (g.fontMetrics.stringWidth(teamName) > sectionWidth - logoSize - 120) {
                teamName = team.abbreviation ?: teamName
            }
            val nameWidth = g.fontMetrics.stringWidth(teamName)
            g.drawString(teamName, textEndX - nameWidth, yPos + 40)

            if (ranking != null && ranking > 0) {
                g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 26f)
                val rankText = "#$ranking"
                val rankWidth = g.fontMetrics.stringWidth(rankText)
                g.drawString(rankText, textEndX - nameWidth - rankWidth - 8, yPos + 40)
            }

            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 24f)
            g.color = Color(255, 255, 255, 200)
            val recordWidth = g.fontMetrics.stringWidth(record)
            g.drawString(record, textEndX - recordWidth, yPos + 68)
        }
    }

    private fun drawFoxTimeoutDots(
        g: Graphics2D,
        game: Game,
        team: Team,
        xPos: Int,
        teamBarY: Int,
        sectionWidth: Int,
        isHome: Boolean,
    ) {
        val timeouts = if (isHome) game.homeTimeouts else game.awayTimeouts
        val dotSize = 8
        val dotSpacing = 12
        val dotY = teamBarY - dotSize - 3

        for (i in 0 until 3) {
            val dotX =
                if (!isHome) {
                    xPos + 10 + i * dotSpacing
                } else {
                    xPos + sectionWidth - 10 - (3 - i) * dotSpacing
                }

            if (i < timeouts) {
                g.color = Color.decode(team.primaryColor)
            } else {
                g.color = Color(150, 150, 150, 100)
            }
            g.fillOval(dotX, dotY, dotSize, dotSize)
        }
    }

    private fun drawFoxInfoBar(
        g: Graphics2D,
        yPos: Int,
        height: Int,
        width: Int,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ) {
        g.color = Color(30, 30, 30)
        g.fillRect(0, yPos, width, height)

        g.color = Color.WHITE

        // Quarter
        val quarterText = getQuarterText(game.quarter)
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 32f)
        val ascent = g.fontMetrics.ascent
        val centerY = yPos + height / 2 + ascent / 2
        g.drawString(quarterText, 20, centerY)
        val quarterWidth = g.fontMetrics.stringWidth(quarterText)

        // Separator
        g.color = Color(100, 100, 100)
        g.drawLine(quarterWidth + 40, yPos + 10, quarterWidth + 40, yPos + height - 10)

        // Clock
        g.color = Color.WHITE
        val clockText = getClockText(game.quarter, game.clock)
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 32f)
        g.drawString(clockText, quarterWidth + 60, centerY)
        val clockWidth = g.fontMetrics.stringWidth(clockText)

        // Separator
        g.color = Color(100, 100, 100)
        val sep2X = quarterWidth + clockWidth + 80
        g.drawLine(sep2X, yPos + 10, sep2X, yPos + height - 10)

        // Down & Distance
        g.color = Color.WHITE
        val downText = getDownDistanceText(game)
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 32f)
        g.drawString(downText, sep2X + 20, centerY)
        val downWidth = g.fontMetrics.stringWidth(downText)

        // Separator
        g.color = Color(100, 100, 100)
        val sep3X = sep2X + downWidth + 40
        g.drawLine(sep3X, yPos + 10, sep3X, yPos + height - 10)

        // Ball Location
        g.color = Color.WHITE
        val ballLocationText =
            getBallLocationText(
                homeTeam.name ?: "",
                awayTeam.name ?: "",
                homeTeam.abbreviation,
                awayTeam.abbreviation,
                game.ballLocation,
                game.possession,
            )
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 28f)
        g.drawString(ballLocationText, sep3X + 20, centerY)
    }

    private fun drawFoxFinalBar(
        g: Graphics2D,
        yPos: Int,
        height: Int,
        width: Int,
        game: Game,
    ) {
        g.color = Color(30, 30, 30)
        g.fillRect(0, yPos, width, height)

        var finalText = "FINAL"
        if (game.quarter >= 6) {
            finalText += "/${game.quarter - 4} OT"
        } else if (game.quarter == 5) {
            finalText += "/OT"
        }

        g.color = Color.WHITE
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 34f)
        val ascent = g.fontMetrics.ascent
        val textWidth = g.fontMetrics.stringWidth(finalText)
        g.drawString(finalText, (width - textWidth) / 2, yPos + height / 2 + ascent / 2)
    }
}
