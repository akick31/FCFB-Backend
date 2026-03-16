package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.ScorebugResponse
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
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

    private val postseasonGameTypes =
        setOf(GameType.BOWL, GameType.PLAYOFFS, GameType.NATIONAL_CHAMPIONSHIP, GameType.CONFERENCE_CHAMPIONSHIP)
    private val foxChannels = setOf(TVChannel.FOX, TVChannel.FS1, TVChannel.FS2)

    /**
     * Generates a scorebug image for the game, dispatching to the appropriate renderer
     * @param game
     */
    fun generateScorebug(game: Game): BufferedImage {
        val image =
            when {
                game.gameType in postseasonGameTypes -> generatePostseasonScorebug(game)
                game.tvChannel in foxChannels -> generateFoxScorebug(game)
                else -> generateEspnScorebug(game)
            }

        // Save image to file
        val directory = File("$imagePath/scorebugs")
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Logger.info("Directory created: ${directory.absolutePath}")
            } else {
                Logger.error("Failed to create directory: ${directory.absolutePath}")
            }
        }

        val outputfile = File("$imagePath/scorebugs/${game.gameId}_scorebug.png")
        ImageIO.write(image, "png", outputfile)
        return image
    }

    /**
     * Generates the ESPN-style scorebug (default, vertical layout)
     */
    private fun generateEspnScorebug(game: Game): BufferedImage {
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)

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
        dividerColor: Color = Color.WHITE,
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

        // Horizontal line to span the entire width between teams (drawn at home team's yPos)
        if (team.name == game.homeTeam) {
            g.color = dividerColor
            g.stroke = BasicStroke(3f)
            g.drawLine(0, yPos, width, yPos)
        }
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

    // ==================== FOX SCOREBUG ====================

    /**
     * Generates a Fox-style horizontal scorebug
     */
    private fun generateFoxScorebug(game: Game): BufferedImage {
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)

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

    /**
     * Draws a Fox-style team section (one half of the team bar)
     */
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

        // Draw gradient background
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
                    if (game.homeScore > game.awayScore) "${game.homeWins?.plus(1)}-${game.homeLosses}"
                    else "${game.homeWins}-${game.homeLosses?.plus(1)}"
                } else {
                    "${game.homeWins}-${game.homeLosses}"
                }
            } else {
                if (game.gameStatus == GameStatus.FINAL) {
                    if (game.awayScore > game.homeScore) "${game.awayWins?.plus(1)}-${game.awayLosses}"
                    else "${game.awayWins}-${game.awayLosses?.plus(1)}"
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

            // Team abbreviation
            var teamName = team.abbreviation ?: team.name
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 38f)
            if (g.fontMetrics.stringWidth(teamName) > sectionWidth - logoSize - 120) {
                teamName = team.abbreviation ?: teamName
            }
            g.drawString(teamName, textStartX, yPos + 40)

            // Ranking (smaller, to the right of name)
            if (ranking != null && ranking > 0) {
                val nameWidth = g.fontMetrics.stringWidth(teamName)
                g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 26f)
                g.drawString("#$ranking", textStartX + nameWidth + 8, yPos + 40)
            }

            // Record below name
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 24f)
            g.color = Color(255, 255, 255, 200)
            g.drawString(record, textStartX, yPos + 68)

            // Score (right-aligned, large)
            g.color = Color.WHITE
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 60f)
            val scoreWidth = g.fontMetrics.stringWidth(score)
            g.drawString(score, xPos + sectionWidth - scoreWidth - 12, yPos + sectionHeight / 2 + 20)
        } else {
            // Home team: [SCORE | rank NAME | LOGO]
            val textEndX = xPos + sectionWidth - logoSize - 20

            // Score (left-aligned, large)
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 60f)
            g.drawString(score, xPos + 12, yPos + sectionHeight / 2 + 20)
            val scoreWidth = g.fontMetrics.stringWidth(score)

            // Team abbreviation (right-aligned before logo)
            var teamName = team.abbreviation ?: team.name
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaBoldFont(g)).deriveFont(Font.BOLD, 38f)
            if (g.fontMetrics.stringWidth(teamName) > sectionWidth - logoSize - 120) {
                teamName = team.abbreviation ?: teamName
            }
            val nameWidth = g.fontMetrics.stringWidth(teamName)
            g.drawString(teamName, textEndX - nameWidth, yPos + 40)

            // Ranking (smaller, to the left of name)
            if (ranking != null && ranking > 0) {
                g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 26f)
                val rankText = "#$ranking"
                val rankWidth = g.fontMetrics.stringWidth(rankText)
                g.drawString(rankText, textEndX - nameWidth - rankWidth - 8, yPos + 40)
            }

            // Record below name (right-aligned)
            g.font = Font.createFont(Font.TRUETYPE_FONT, getHelveticaFont(g)).deriveFont(Font.PLAIN, 24f)
            g.color = Color(255, 255, 255, 200)
            val recordWidth = g.fontMetrics.stringWidth(record)
            g.drawString(record, textEndX - recordWidth, yPos + 68)
        }
    }

    /**
     * Draws timeout dots above a Fox team section
     */
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

    /**
     * Draws the Fox info bar (quarter, clock, down & distance, ball location)
     */
    private fun drawFoxInfoBar(
        g: Graphics2D,
        yPos: Int,
        height: Int,
        width: Int,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ) {
        // Dark background
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

    /**
     * Draws the Fox final bar
     */
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

    // ==================== POSTSEASON SCOREBUG ====================

    /**
     * Generates a postseason-style scorebug with game logo and gold accents for playoffs
     */
    private fun generatePostseasonScorebug(game: Game): BufferedImage {
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)

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

    /**
     * Draws the postseason header with game logo or conference logos
     */
    private fun drawPostseasonHeader(
        g: Graphics2D,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
        width: Int,
        headerHeight: Int,
        isPlayoff: Boolean,
    ) {
        // Header background
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

                // Game name to the right of the logo
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

    /**
     * Draws a conference logo at the given position
     */
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

    /**
     * Draws a gold/colored border for postseason scorebugs
     */
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

    /**
     * Draws the clock section for the postseason scorebug (offset by header)
     */
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

    /**
     * Draws the down & distance section for the postseason scorebug (offset by header)
     */
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

    /**
     * Draws the final section for the postseason scorebug (offset by header)
     */
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

    // ==================== SHARED HELPERS ====================

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
