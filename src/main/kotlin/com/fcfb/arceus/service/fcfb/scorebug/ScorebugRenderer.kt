package com.fcfb.arceus.service.fcfb.scorebug

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.InputStream

/**
 * Interface for all scorebug renderers
 */
interface ScorebugRenderer {
    fun render(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage
}

/**
 * Base class with shared drawing helpers used across multiple scorebug styles
 */
abstract class ScorebugRendererBase : ScorebugRenderer {
    // ==================== SCALING ====================

    protected fun scaleImage(
        image: BufferedImage,
        width: Int,
        height: Int,
    ): BufferedImage {
        val scaledWidth = (width * 0.50).toInt()
        val scaledHeight = (height * 0.50).toInt()
        val scaledImage = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB)
        val gScaled: Graphics2D = scaledImage.createGraphics()

        gScaled.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        gScaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

        gScaled.drawImage(image, 0, 0, scaledWidth, scaledHeight, null)
        gScaled.dispose()
        return scaledImage
    }

    // ==================== GRADIENT ====================

    protected fun paintGradient(
        g: Graphics2D,
        color: Color,
        yPos: Int,
        rowHeight: Int,
    ) {
        val startColor = color
        val endColor = startColor.darker()

        val gradient =
            LinearGradientPaint(
                Point2D.Float(0f, yPos.toFloat()),
                Point2D.Float(0f, (yPos + rowHeight).toFloat()),
                floatArrayOf(0f, 1f),
                arrayOf(startColor, endColor),
            )
        g.paint = gradient
    }

    // ==================== TEAM SECTIONS (ESPN/Postseason shared) ====================

    protected fun drawTeamSection(
        g: Graphics2D,
        color: Color,
        yPos: Int,
        rowHeight: Int,
    ) {
        paintGradient(g, color, yPos, rowHeight)
        g.fillRect(240, yPos, 120, rowHeight)
        g.drawRect(240, yPos, 120, rowHeight)
        g.fillRect(0, yPos, 260, rowHeight)
        g.drawRect(0, yPos, 260, rowHeight)
    }

    protected fun drawTeamNameSection(
        g: Graphics2D,
        game: Game,
        team: Team,
        width: Int,
        yPos: Int,
        rowHeight: Int,
        dividerColor: Color = Color.WHITE,
    ) {
        drawTeamSection(g, Color.decode(team.primaryColor), yPos, rowHeight)
        val teamRanking = if (team.name == game.homeTeam) game.homeTeamRank else game.awayTeamRank

        if (teamRanking == 0) {
            var teamName = "${team.name}"
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 40f)
            val textWidth = g.fontMetrics.stringWidth(teamName)
            if (textWidth > 260) {
                teamName = "${team.shortName}"
                val w = g.fontMetrics.stringWidth(teamName)
                if (w > 260 || team.shortName == null) {
                    teamName = "${team.abbreviation}"
                }
            }
            g.color = Color(255, 255, 255)
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 40f)
            g.drawString(teamName, 10, yPos + rowHeight / 2 + 10)
        } else {
            val ranking = "${teamRanking ?: ""}"
            var teamName = "${team.name}"
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 40f)
            val textWidth = g.fontMetrics.stringWidth(ranking + teamName + 20)
            if (textWidth > 260) {
                teamName = "${team.shortName}"
                val w = g.fontMetrics.stringWidth(teamName)
                if (w > 260 || team.shortName == null) {
                    teamName = "${team.abbreviation}"
                }
            }
            g.color = Color(255, 255, 255)
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 33f)
            val rankingWidth = g.fontMetrics.stringWidth(ranking)
            g.drawString(ranking, 10, yPos + rowHeight / 2 + 10)

            g.color = Color(255, 255, 255)
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 40f)
            g.drawString(teamName, rankingWidth + 15, yPos + rowHeight / 2 + 10)
        }

        drawTimeoutBoxes(g, yPos + 7, rowHeight, if (team.name == game.homeTeam) game.homeTimeouts else game.awayTimeouts)

        val record =
            if (team.name == game.homeTeam) {
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
        g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 33f)
        g.color = Color(255, 255, 255)
        g.drawString(record, width - 10 - g.fontMetrics.stringWidth(record), yPos + rowHeight / 2 + 10)

        if (team.name == game.homeTeam) {
            g.color = dividerColor
            g.stroke = BasicStroke(3f)
            g.drawLine(0, yPos, width, yPos)
        }
    }

    protected fun drawTimeoutBoxes(
        g: Graphics2D,
        yPos: Int,
        rowHeight: Int,
        timeouts: Int,
    ) {
        val boxWidth = 38
        val boxHeight = 7
        val boxSpacing = 7

        for (i in 0 until 3) {
            val xPos = 10 + (i * (boxWidth + boxSpacing))

            if (i < timeouts) {
                g.color = Color(255, 255, 80)
            } else {
                g.color = Color(211, 211, 211, 100)
            }

            g.fillRect(xPos, yPos + rowHeight - boxHeight - 10, boxWidth, boxHeight)
        }
    }

    protected fun drawTeamScoreSection(
        g: Graphics2D,
        game: Game,
        team: Team,
        yPos: Int,
        rowHeight: Int,
    ) {
        drawTeamSection(g, Color.decode(team.primaryColor), yPos, rowHeight)

        val logoUrl = team.scorebugLogo
        val logoWidth = 130
        val logoHeight = 130

        val shadowX = 245
        val shadowY = yPos + (rowHeight - 100)
        val shadowColor = Color(0, 0, 0, 120)
        val shadowGradient =
            LinearGradientPaint(
                Point2D.Float(shadowX.toFloat(), shadowY.toFloat()),
                Point2D.Float(290.toFloat(), shadowY.toFloat()),
                floatArrayOf(0f, 0.25f, 1f),
                arrayOf(shadowColor, shadowColor, Color(0, 0, 0, 0)),
            )
        g.composite = java.awt.AlphaComposite.SrcOver
        g.paint = shadowGradient
        g.fillRect(shadowX, shadowY, logoWidth, logoHeight)

        if (logoUrl != null) {
            try {
                val logoImage = javax.imageio.ImageIO.read(java.net.URI(logoUrl).toURL())
                val logoX = 245 + (115 - logoWidth) / 2
                val logoY = yPos + (rowHeight - 100)
                g.drawImage(logoImage, logoX, logoY, logoWidth, logoHeight, null)
                paintGradient(g, Color.decode(team.primaryColor), yPos, rowHeight)
                g.fillRect(0, yPos, 246, 75)
            } catch (e: java.io.IOException) {
                com.fcfb.arceus.util.Logger.error("Error loading logo for ${team.name}: ${e.message}")
            }
        }

        val score = if (team.name == game.homeTeam) game.homeScore.toString() else game.awayScore.toString()

        g.color = Color(255, 255, 255)
        if (game.possession == TeamSide.AWAY && team.name == game.awayTeam && game.gameStatus != GameStatus.FINAL) {
            val unicodeChar = "\u25C0"
            val charHeight = g.fontMetrics.ascent

            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 75f)
            var ascent = g.fontMetrics.ascent
            val width = g.fontMetrics.stringWidth(score)
            g.drawString(score, 10, (yPos - 2) + rowHeight / 2 + ascent / 2)

            g.font = Font.createFont(Font.TRUETYPE_FONT, getSansFont(g)).deriveFont(Font.PLAIN, 40f)
            ascent = g.fontMetrics.ascent
            g.drawString(
                unicodeChar,
                width + 15,
                (yPos) + rowHeight / 2 + ascent / 2 - (charHeight / 2),
            )
        } else if (game.possession == TeamSide.HOME && team.name == game.homeTeam && game.gameStatus != GameStatus.FINAL) {
            val unicodeChar = "\u25C0"
            val charHeight = g.fontMetrics.ascent

            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 75f)
            var ascent = g.fontMetrics.ascent
            val width = g.fontMetrics.stringWidth(score)
            g.drawString(score, 10, (yPos - 2) + rowHeight / 2 + ascent / 2)

            g.font = Font.createFont(Font.TRUETYPE_FONT, getSansFont(g)).deriveFont(Font.PLAIN, 40f)
            ascent = g.fontMetrics.ascent
            g.drawString(
                unicodeChar,
                width + 15,
                (yPos + 22) + rowHeight / 2 + ascent / 2 - (charHeight / 2),
            )
        } else {
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 75f)
            val ascent = g.fontMetrics.ascent
            g.drawString(score, 10, (yPos - 2) + rowHeight / 2 + ascent / 2)
        }

        g.color = Color(211, 211, 211, 110)
        g.drawLine(245, yPos, 245, yPos + rowHeight)
    }

    // ==================== BORDER ====================

    protected fun drawBorder(
        g: Graphics2D,
        width: Int,
        height: Int,
    ) {
        g.color = Color.WHITE
        g.stroke = BasicStroke(3f)
        g.drawLine(0, 0, width, 0)
        g.drawLine(0, 0, 0, height)
        g.drawLine(width - 1, 0, width - 3, height)
        g.drawLine(0, height - 1, width, height - 3)
    }

    // ==================== TEXT HELPERS ====================

    protected fun getQuarterText(quarter: Int): String {
        var quarterText =
            when (quarter) {
                5 -> "OT"
                4 -> "4th"
                3 -> "3rd"
                2 -> "2nd"
                1 -> "1st"
                else -> "Unknown"
            }
        quarterText =
            if (quarter >= 6) {
                "${quarter - 4} OT"
            } else {
                quarterText
            }
        return quarterText
    }

    protected fun getClockText(
        quarter: Int,
        clock: String,
    ): String {
        return if (quarter >= 5) {
            ""
        } else {
            clock
        }
    }

    protected fun getDownDistanceText(game: Game): String {
        val down = game.down
        val yardsToGo = game.yardsToGo
        val ballLocation = game.ballLocation
        if (game.currentPlayType == PlayType.KICKOFF) {
            return "Kickoff"
        }
        if (game.currentPlayType == PlayType.PAT) {
            return "PAT"
        }
        return when (down) {
            1 -> "1st"
            2 -> "2nd"
            3 -> "3rd"
            4 -> "4th"
            else -> down.toString()
        } + " & " +
            when {
                (ballLocation.plus(yardsToGo)) >= 100 -> "Goal"
                else -> yardsToGo.toString()
            }
    }

    protected fun getBallLocationText(
        homeTeamName: String,
        awayTeamName: String,
        homeTeamAbbreviation: String?,
        awayTeamAbbreviation: String?,
        ballLocation: Int,
        possession: TeamSide,
    ): String {
        return when {
            ballLocation == 50 -> "50"
            ballLocation < 50 &&
                possession == TeamSide.HOME ->
                if (homeTeamAbbreviation != awayTeamAbbreviation) {
                    "${homeTeamAbbreviation ?: homeTeamName.uppercase()} $ballLocation"
                } else {
                    "${homeTeamName.uppercase()} $ballLocation"
                }
            ballLocation < 50 &&
                possession == TeamSide.AWAY ->
                if (homeTeamAbbreviation != awayTeamAbbreviation) {
                    "${awayTeamAbbreviation ?: awayTeamName.uppercase()} $ballLocation"
                } else {
                    "${awayTeamName.uppercase()} $ballLocation"
                }
            ballLocation > 50 &&
                possession == TeamSide.HOME ->
                if (homeTeamAbbreviation != awayTeamAbbreviation) {
                    "${awayTeamAbbreviation ?: awayTeamName.uppercase()} ${100 - ballLocation}"
                } else {
                    "${awayTeamName.uppercase()} $ballLocation"
                }
            ballLocation > 50 &&
                possession == TeamSide.AWAY ->
                if (homeTeamAbbreviation != awayTeamAbbreviation) {
                    "${homeTeamAbbreviation ?: homeTeamName.uppercase()} ${100 - ballLocation}"
                } else {
                    "${homeTeamName.uppercase()} $ballLocation"
                }
            else -> "Unknown Location"
        }
    }

    // ==================== FONTS ====================

    protected fun getSansFont(g: Graphics2D): InputStream? {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        return this::class.java.classLoader.getResourceAsStream("DejaVuSans.ttf")
    }

    protected fun getHelveticaFont(g: Graphics2D): InputStream? {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        return this::class.java.classLoader.getResourceAsStream("Helvetica.ttf")
    }

    protected fun getHelveticaBoldFont(g: Graphics2D): InputStream? {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        return this::class.java.classLoader.getResourceAsStream("Helvetica-Bold.ttf")
    }
}
