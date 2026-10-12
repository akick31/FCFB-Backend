package com.fcfb.arceus.service.fcfb.media

import com.fcfb.arceus.dto.response.ScorebugResponse
import com.fcfb.arceus.enums.game.GameCategory
import com.fcfb.arceus.enums.game.GameFilter
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameSort
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.game.TVChannel.CBS_SPORTS_NETWORK
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.scorebug.PostseasonScorebugHeader
import com.fcfb.arceus.rendering.scorebug.CbsScorebugRenderer
import com.fcfb.arceus.rendering.scorebug.ChewBadgeOverlay
import com.fcfb.arceus.rendering.scorebug.CwScorebugRenderer
import com.fcfb.arceus.rendering.scorebug.EspnScorebugRenderer
import com.fcfb.arceus.rendering.scorebug.FoxScorebugRenderer
import com.fcfb.arceus.rendering.scorebug.NbcScorebugRenderer
import com.fcfb.arceus.rendering.scorebug.PacTwelveScorebugRenderer
import com.fcfb.arceus.rendering.scorebug.PostseasonScorebugRenderer
import com.fcfb.arceus.rendering.scorebug.ScorebugRenderer
import com.fcfb.arceus.rendering.scorebug.TntScorebugRenderer
import com.fcfb.arceus.repositories.ConferenceRepository
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.Base64
import javax.imageio.ImageIO

@Service
class ScorebugService(
    private val teamService: TeamService,
    private val gameService: GameService,
    private val espnRenderer: EspnScorebugRenderer,
    private val foxRenderer: FoxScorebugRenderer,
    private val postseasonRenderer: PostseasonScorebugRenderer,
    private val cbsRenderer: CbsScorebugRenderer,
    private val nbcRenderer: NbcScorebugRenderer,
    private val tntRenderer: TntScorebugRenderer,
    private val cwRenderer: CwScorebugRenderer,
    private val pacTwelveRenderer: PacTwelveScorebugRenderer,
    private val chewBadgeOverlay: ChewBadgeOverlay,
    @Value("\${images.path}")
    private val imagePath: String,
    private val conferenceRepository: ConferenceRepository,
) {
    private val postseasonGameTypes =
        setOf(GameType.BOWL, GameType.PLAYOFFS, GameType.NATIONAL_CHAMPIONSHIP, GameType.CONFERENCE_CHAMPIONSHIP)
    private val foxChannels = setOf(TVChannel.FOX, TVChannel.FS1, TVChannel.FS2, TVChannel.BIG_TEN_NETWORK)
    private val cbsChannels = setOf(TVChannel.CBS, CBS_SPORTS_NETWORK)
    private val tntChannels = setOf(TVChannel.TNT)
    private val nbcChannels = setOf(TVChannel.NBC, TVChannel.PEACOCK)
    private val cwChannels = setOf(TVChannel.THE_CW)
    private val pacTwelveChannels = setOf(TVChannel.PAC_12_NETWORK)

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

    fun generateAllScorebugs() {
        val games = gameService.getAllGames()
        for (game in games) {
            generateScorebug(game)
        }
    }

    private fun getScorebugBytes(gameId: Int): ByteArray? {
        return try {
            File("$imagePath/scorebugs/${gameId}_scorebug.png").readBytes()
        } catch (e: Exception) {
            null
        }
    }

    fun getScorebugByGameId(gameId: Int): ResponseEntity<ByteArray> {
        val game = gameService.getGameById(gameId)
        val scorebug = generateScorebug(game)
        val rendered =
            if (game.gameMode == GameMode.CHEW && game.gameStatus != GameStatus.FINAL) {
                chewBadgeOverlay.apply(scorebug)
            } else {
                scorebug
            }

        return try {
            val bytes = toPngBytes(rendered)

            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.IMAGE_PNG
                    contentLength = bytes.size.toLong()
                }

            ResponseEntity(bytes, headers, HttpStatus.OK)
        } catch (e: Exception) {
            Logger.error("Error fetching scorebug image: ${e.message}")
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    private fun toPngBytes(image: BufferedImage): ByteArray {
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }

    fun getLatestScorebugByGameId(gameId: Int): ResponseEntity<ByteArray> {
        val bytes = getScorebugBytes(gameId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .contentLength(bytes.size.toLong())
            .body(bytes)
    }

    fun getScorebugsForConference(
        season: Int,
        week: Int,
        conference: String,
    ): ResponseEntity<List<Map<String, Any>>> {
        try {
            val teams = teamService.getTeamsInConference(conference) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
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

    fun generateScorebug(game: Game): BufferedImage {
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)

        val image =
            if (game.gameType in postseasonGameTypes) {
                val header = preparePostseasonHeader(game, homeTeam, awayTeam)
                postseasonRenderer.render(game, homeTeam, awayTeam, header)
            } else {
                val renderer: ScorebugRenderer =
                    when {
                        game.tvChannel in foxChannels -> espnRenderer
                        game.tvChannel in cbsChannels -> espnRenderer
                        game.tvChannel in tntChannels -> espnRenderer
                        game.tvChannel in nbcChannels -> espnRenderer
                        game.tvChannel in cwChannels -> espnRenderer
                        game.tvChannel in pacTwelveChannels -> espnRenderer
                        else -> espnRenderer
                    }

                renderer.render(game, homeTeam, awayTeam)
            }

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

    private fun preparePostseasonHeader(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ): PostseasonScorebugHeader {
        val logoSize = PostseasonScorebugRenderer.LOGO_SIZE
        var gameLogo: BufferedImage? = null
        var logoWidth = logoSize
        var logoReadSucceeded = false
        val gameLogoUrl = game.postseasonGameLogo
        if (gameLogoUrl != null) {
            try {
                val loadedLogo = ImageIO.read(URI(gameLogoUrl).toURL())
                logoWidth = (loadedLogo.width.toDouble() / loadedLogo.height * logoSize).toInt()
                gameLogo = loadedLogo
                logoReadSucceeded = true
                return PostseasonScorebugHeader(gameLogo, logoWidth, true, loadPostseasonTitleFont(22f), logoWidth + 18)
            } catch (e: IOException) {
                Logger.error("Error loading postseason game logo: ${e.message}")
            }
        }

        if (!logoReadSucceeded && game.gameType == GameType.CONFERENCE_CHAMPIONSHIP) {
            val conference = homeTeam.conference ?: awayTeam.conference
            val logoUrl = conference?.let { conferenceRepository.findById(it).orElse(null)?.logoUrl }
            if (logoUrl != null) {
                var conferenceLogo: BufferedImage? = null
                var conferenceLogoReadSucceeded = false
                try {
                    conferenceLogo = ImageIO.read(URI(logoUrl).toURL())
                    conferenceLogoReadSucceeded = true
                } catch (e: IOException) {
                    Logger.error("Error loading conference logo ($logoUrl): ${e.message}")
                }
                return PostseasonScorebugHeader(
                    conferenceLogo,
                    logoSize,
                    conferenceLogoReadSucceeded,
                    loadPostseasonTitleFont(22f),
                    logoSize + 18,
                )
            }
        }

        return PostseasonScorebugHeader(gameLogo, logoWidth, logoReadSucceeded, loadPostseasonTitleFont(24f), null)
    }

    private fun loadPostseasonTitleFont(size: Float): Font =
        javaClass.classLoader.getResourceAsStream("Helvetica-Bold.ttf").use { stream ->
            Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(Font.BOLD, size)
        }
}
