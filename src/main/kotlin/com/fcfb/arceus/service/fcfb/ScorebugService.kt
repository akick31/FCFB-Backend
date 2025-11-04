package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.ScorebugResponse
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter
import com.fcfb.arceus.service.specification.GameSpecificationService.GameSort
import com.fcfb.arceus.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Base64
import javax.imageio.ImageIO

@Service
class ScorebugService(
    private val teamService: TeamService,
    private val gameService: GameService,
    @Value("\${images.path}")
    private val imagePath: String,
) {
    /**
     * Get the scorebug for a game filtered
     * @param filters
     * @param category
     * @param sort
     * @param conference
     * @param season
     * @param week
     * @param gameMode
     * @param pageable
     */
    fun getFilteredScorebugs(
        filters: List<GameFilter>?,
        category: GameCategory?,
        sort: GameSort,
        conference: String?,
        season: Int?,
        week: Int?,
        gameMode: GameMode?,
        pageable: Pageable,
    ): ResponseEntity<PageImpl<ScorebugResponse>> {
        val filteredGames =
            gameService.getFilteredGames(
                filters = filters ?: emptyList(),
                category = category,
                conference = conference,
                season = season,
                week = week,
                gameMode = gameMode,
                sort = sort,
                pageable = pageable,
            )

        val scorebugResponses =
            filteredGames.content.map { game ->
                var scorebug = getScorebugBytes(game.gameId)
                if (scorebug == null) {
                    generateScorebug(game)
                    scorebug = getScorebugBytes(game.gameId)
                }
                ScorebugResponse(
                    gameId = game.gameId,
                    scorebug = scorebug,
                    homeTeam = game.homeTeam,
                    awayTeam = game.awayTeam,
                    status = game.gameStatus,
                )
            }

        val pageResponse =
            PageImpl(
                scorebugResponses,
                filteredGames.pageable,
                filteredGames.totalElements,
            )

        return ResponseEntity.ok(pageResponse)
    }

    /**
     * Generate all scorebugs
     */
    fun generateAllScorebugs() {
        val games = gameService.getAllGames()
        for (game in games) {
            generateScorebug(game)
        }
    }

    /**
     * Get the scorebug byte array for a game
     * @param gameId
     */
    private fun getScorebugBytes(gameId: Int): ByteArray? {
        return try {
            File("$imagePath/scorebugs/${gameId}_scorebug.png").readBytes()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the scorebug image for a game
     * @param gameId
     */
    fun getScorebugByGameId(gameId: Int): ResponseEntity<ByteArray> {
        val game = gameService.getGameById(gameId)
        generateScorebug(game)

        try {
            val scorebug = File("$imagePath/scorebugs/${game.gameId}_scorebug.png").readBytes()

            // Set the response headers
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.IMAGE_PNG
                    contentLength = scorebug.size.toLong()
                }

            // Return the image in the response
            return ResponseEntity(scorebug, headers, HttpStatus.OK)
        } catch (e: Exception) {
            Logger.error("Error fetching scorebug image: ${e.message}")
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * Get the latest scorebug image for a game without generating
     * @param gameId
     */
    fun getLatestScorebugByGameId(gameId: Int): ResponseEntity<ByteArray> {
        val bytes = getScorebugBytes(gameId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .contentLength(bytes.size.toLong())
            .body(bytes)
    }

    /**
     * Get the scorebug images for a conference
     * @param season
     * @param week
     * @param conference
     */
    fun getScorebugsForConference(
        season: Int,
        week: Int,
        conference: Conference,
    ): ResponseEntity<List<Map<String, Any>>> {
        try {
            val teams = teamService.getTeamsInConference(conference.name) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
            val games = gameService.getGamesWithTeams(teams, season, week)
            if (games.isEmpty()) {
                return ResponseEntity(HttpStatus.NOT_FOUND)
            }

            val scorebugs = mutableListOf<Map<String, Any>>()

            for (game in games) {
                generateScorebug(game)
                val fileBytes = File("$imagePath/scorebugs/${game.gameId}_scorebug.png").readBytes()
                val base64Image = Base64.getEncoder().encodeToString(fileBytes)

                scorebugs.add(
                    mapOf(
                        "gameId" to game.gameId.toString(),
                        "image" to base64Image,
                    ),
                )
            }

            return ResponseEntity(scorebugs, HttpStatus.OK)
        } catch (e: Exception) {
            Logger.error("Error fetching scorebug images: ${e.message}")
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * Generates a scorebug image for the game
     * @param game
     */
    fun generateScorebug(game: Game): BufferedImage {
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)

        val width = 360
        val height = 400
        val rowHeight = 70
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        // Line stroke width
        g.stroke = BasicStroke(2f)

        // Enable anti-aliasing for smoother text
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Transparent background
        g.composite = AlphaComposite.Clear
        g.fillRect(0, 0, width, height)
        g.composite = AlphaComposite.Src

        // Adjusted row height for team name section (5 pixels shorter) and team score section (5 pixels taller)
        val adjustedRowHeightForTeamName = rowHeight - 5
        val adjustedRowHeightForTeamScore = rowHeight + 5

        // Draw the away team score section with adjusted height
        drawTeamScoreSection(g, game, awayTeam, 65, adjustedRowHeightForTeamScore)
        // Draw the home team score section with adjusted height
        drawTeamScoreSection(g, game, homeTeam, 205, adjustedRowHeightForTeamScore)

        // Draw the away team name section with adjusted height
        drawTeamNameSection(g, game, awayTeam, width, 0, adjustedRowHeightForTeamName)
        // Draw the home team name section with adjusted height
        drawTeamNameSection(g, game, homeTeam, width, 140, adjustedRowHeightForTeamName)

        if (game.gameStatus != GameStatus.FINAL) {
            drawClockInformationSection(g, rowHeight - 10, game, homeTeam, awayTeam)
            drawDownAndDistanceSection(g, rowHeight - 10, game)
        } else {
            drawFinalSection(g, (rowHeight - 10) * 2, game)
        }

        // Draw a border around the entire scorebug
        drawBorder(g, width, height)

        // Dispose graphics context
        g.dispose()

        // Scale and draw the image
        val scaledImage = scaleImage(image, width, height)

        // Save image to file
        val outputfile = File("$imagePath/scorebugs/${game.gameId}_scorebug.png")

        // Create directory if it does not exist
        val directory = File("$imagePath/scorebugs")
        if (!directory.exists()) {
            // Create the directory and any necessary parent directories
            if (directory.mkdirs()) {
                Logger.info("Directory created: ${directory.absolutePath}")
            } else {
                Logger.error("Failed to create directory: ${directory.absolutePath}")
            }
        }

        ImageIO.write(scaledImage, "png", outputfile)
        return image
    }

    /**
     * Scales the image to a smaller size
     * @param image
     * @param width
     * @param height
     */
    private fun scaleImage(
        image: BufferedImage,
        width: Int,
        height: Int,
    ): BufferedImage {
        // Create a new BufferedImage for the smaller version
        val scaledWidth = (width * 0.50).toInt()
        val scaledHeight = (height * 0.50).toInt()
        val scaledImage = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB)
        val gScaled: Graphics2D = scaledImage.createGraphics()

        // Enable anti-aliasing for smoother scaling
        gScaled.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        gScaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

        // Draw the original image onto the scaled image
        gScaled.drawImage(image, 0, 0, scaledWidth, scaledHeight, null)

        // Dispose graphics context
        gScaled.dispose()
        return scaledImage
    }

    /**
     * Draws the team name section of the scorebug
     * @param g
     * @param team
     * @param yPos
     * @param rowHeight
     */
    private fun drawTeamNameSection(
        g: Graphics2D,
        game: Game,
        team: Team,
        width: Int,
        yPos: Int,
        rowHeight: Int,
    ) {
        drawTeamSection(g, Color.decode(team.primaryColor), yPos, rowHeight)
        val teamRanking = if (team.name == game.homeTeam) game.homeTeamRank else game.awayTeamRank

        // Calculate the width of the text
        if (teamRanking == 0) {
            var teamName = "${team.name}"
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 40f)
            val textWidth = g.fontMetrics.stringWidth(teamName)
            if (textWidth > 260) {
                teamName = "${team.shortName}"
                val width = g.fontMetrics.stringWidth(teamName)
                if (width > 260 || team.shortName == null) {
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
                val width = g.fontMetrics.stringWidth(teamName)
                if (width > 260 || team.shortName == null) {
                    teamName = "${team.abbreviation}"
                }
            }
            // Reduce the font size for the ranking
            g.color = Color(255, 255, 255)
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 33f)
            val rankingWidth = g.fontMetrics.stringWidth(ranking)
            g.drawString(ranking, 10, yPos + rowHeight / 2 + 10)

            g.color = Color(255, 255, 255)
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 40f)
            g.drawString(teamName, rankingWidth + 15, yPos + rowHeight / 2 + 10)
        }

        // Draw the timeout boxes
        drawTimeoutBoxes(g, yPos + 7, rowHeight, if (team.name == game.homeTeam) game.homeTimeouts else game.awayTimeouts)

        // Draw the team record
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

        // Horizontal line to span the entire width between teams
        g.color = Color(255, 255, 255)
        g.stroke = BasicStroke(3f)
        g.drawLine(0, 140, width, 140)
    }

    private fun drawTimeoutBoxes(
        g: Graphics2D,
        yPos: Int,
        rowHeight: Int,
        timeouts: Int,
    ) {
        val boxWidth = 38
        val boxHeight = 7
        val boxSpacing = 7

        // Draw 3 boxes, left-aligned, with spacing
        for (i in 0 until 3) {
            val xPos = 10 + (i * (boxWidth + boxSpacing))

            // Set color based on remaining timeouts
            if (i < timeouts) {
                g.color = Color(255, 255, 80)
            } else {
                g.color = Color(211, 211, 211, 100)
            }

            // Draw the rectangle for the timeout box
            g.fillRect(xPos, yPos + rowHeight - boxHeight - 10, boxWidth, boxHeight)
        }
    }

    /**
     * Draws the team score section of the scorebug
     * @param g
     * @param team
     * @param yPos
     * @param rowHeight
     */
    private fun drawTeamScoreSection(
        g: Graphics2D,
        game: Game,
        team: Team,
        yPos: Int,
        rowHeight: Int,
    ) {
        drawTeamSection(g, Color.decode(team.primaryColor), yPos, rowHeight)

        // Draw the team logo
        val logoUrl = team.scorebugLogo
        val logoWidth = 130
        val logoHeight = 130

        // Create a gradient for the shadow (dark color, fading to transparent)
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
        g.composite = AlphaComposite.SrcOver
        g.paint = shadowGradient
        g.fillRect(shadowX, shadowY, logoWidth, logoHeight)

        if (logoUrl != null) {
            try {
                // Download the logo from the URL
                val logoImage = ImageIO.read(URI(logoUrl).toURL())

                // Calculate position: right-aligned in the 140px width
                val logoX = 245 + (115 - logoWidth) / 2
                val logoY = yPos + (rowHeight - 100)

                // Draw the logo image and overlay the box back over the logo if it spills over
                g.drawImage(logoImage, logoX, logoY, logoWidth, logoHeight, null)
                paintGradient(g, Color.decode(team.primaryColor), yPos, rowHeight)
                g.fillRect(0, yPos, 246, 75)
            } catch (e: IOException) {
                // Handle error if the logo cannot be loaded (e.g., invalid URL)
                Logger.error("Error loading logo for ${team.name}: ${e.message}")
            }
        }

        // Draw the team score and possession arrow
        val score = if (team.name == game.homeTeam) game.homeScore.toString() else game.awayScore.toString()

        g.color = Color(255, 255, 255)
        if (game.possession == TeamSide.AWAY && team.name == game.awayTeam && game.gameStatus != GameStatus.FINAL) {
            val unicodeChar = "\u25C0"
            val charHeight = g.fontMetrics.ascent

            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 75f)
            var ascent = g.fontMetrics.ascent
            val width = g.fontMetrics.stringWidth(score)
            g.drawString(score, 10, (yPos - 2) + rowHeight / 2 + ascent / 2)

            // Reduce the font size for the Unicode character
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

            // Reduce the font size for the Unicode character
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

        // Draw the gray line to separate the two sections
        g.color = Color(211, 211, 211, 110)
        g.drawLine(245, yPos, 245, yPos + rowHeight)
    }

    /**
     * Draws the team section of the scorebug
     * @param g
     * @param color
     * @param yPos
     * @param rowHeight
     */
    private fun drawTeamSection(
        g: Graphics2D,
        color: Color,
        yPos: Int,
        rowHeight: Int,
    ) {
        paintGradient(g, color, yPos, rowHeight)
        // Draw the right section box for the row
        g.fillRect(240, yPos, 120, rowHeight)
        g.drawRect(240, yPos, 120, rowHeight)
        // Draw the left section box for the row
        g.fillRect(0, yPos, 260, rowHeight)
        g.drawRect(0, yPos, 260, rowHeight)
    }

    /**
     * Draws a border around the entire scorebug
     * @param g
     * @param width
     * @param height
     */
    private fun drawBorder(
        g: Graphics2D,
        width: Int,
        height: Int,
    ) {
        g.color = Color.WHITE
        g.stroke = BasicStroke(3f)
        g.drawLine(0, 0, width, 0) // Top border
        g.drawLine(0, 0, 0, height) // Left border
        g.drawLine(width - 1, 0, width - 3, height) // Right border
        g.drawLine(0, height - 1, width, height - 3) // Bottom border
    }

    /**
     * Paints a gradient background for the section of the scorebug
     * @param g
     * @param color
     * @param yPos
     * @param rowHeight
     */
    private fun paintGradient(
        g: Graphics2D,
        color: Color,
        yPos: Int,
        rowHeight: Int,
    ) {
        // Create a gradient background from the team's primary color to a slightly darker color
        val startColor = color
        val endColor = startColor.darker()

        // Define the start and end points for the gradient (top to bottom)
        val gradient =
            LinearGradientPaint(
                Point2D.Float(0f, yPos.toFloat()),
                Point2D.Float(0f, (yPos + rowHeight).toFloat()),
                floatArrayOf(0f, 1f),
                arrayOf(startColor, endColor),
            )
        g.paint = gradient
    }

    /**
     * Draws the clock information section of the scorebug
     * @param g
     * @param rowHeight
     */
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

        // Draw Quarter text
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

        // Draw Clock text
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

        // Draw Ball Location text
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

        // Calculate the width of the text
        var textWidth = g.fontMetrics.stringWidth(ballLocationText)
        val ballLocationTextAscent = g.fontMetrics.ascent

        // Decrease font size if text overflows
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

        // Top horizontal line to span the entire width of the section
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(3f)
        g.drawLine(0, rowY, 360, rowY)

        // Bottom horizontal line to span the entire width of the section
        g.color = Color.LIGHT_GRAY
        g.stroke = BasicStroke(3f)
        g.drawLine(0, rowY + rowHeight, 360, rowY + rowHeight)
    }

    /**
     * Draws the down and distance section of the scorebug
     * @param g
     * @param rowHeight
     */
    private fun drawDownAndDistanceSection(
        g: Graphics2D,
        rowHeight: Int,
        game: Game,
    ) {
        // Draw Down & Distance Section
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

    /**
     * Draws the final section of the scorebug
     * @param g
     * @param rowHeight
     */
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

    /**
     * Gets the quarter text for the scorebug
     * @param quarter
     */
    private fun getQuarterText(quarter: Int): String {
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

    /**
     * Gets the clock text for the scorebug
     * @param quarter
     * @param clock
     */
    private fun getClockText(
        quarter: Int,
        clock: String,
    ): String {
        return if (quarter >= 5) {
            ""
        } else {
            clock
        }
    }

    /**
     * Gets the down and distance text for the scorebug
     * @param down
     * @param yardsToGo
     * @param ballLocation
     */
    private fun getDownDistanceText(game: Game): String {
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

    /**
     * Gets the ball location text for the scorebug
     * @param homeTeamName
     * @param awayTeamName
     * @param homeTeamAbbreviation
     * @param awayTeamAbbreviation
     * @param ballLocation
     * @param possession
     */
    private fun getBallLocationText(
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

    private fun getSansFont(g: Graphics2D): InputStream? {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        return this::class.java.classLoader.getResourceAsStream("DejaVuSans.ttf")
    }

    private fun getHelveticaFont(g: Graphics2D): InputStream? {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        return this::class.java.classLoader.getResourceAsStream("Helvetica.ttf")
    }

    private fun getHelveticaBoldFont(g: Graphics2D): InputStream? {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        return this::class.java.classLoader.getResourceAsStream("Helvetica-Bold.ttf")
    }
}
