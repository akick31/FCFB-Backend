package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.ScorebugResponse
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.game.TVChannel.CBS_SPORTS_NETWORK
import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.service.fcfb.scorebug.CbsScorebugRenderer
import com.fcfb.arceus.service.fcfb.scorebug.CwScorebugRenderer
import com.fcfb.arceus.service.fcfb.scorebug.EspnScorebugRenderer
import com.fcfb.arceus.service.fcfb.scorebug.FoxScorebugRenderer
import com.fcfb.arceus.service.fcfb.scorebug.NbcScorebugRenderer
import com.fcfb.arceus.service.fcfb.scorebug.PacTwelveScorebugRenderer
import com.fcfb.arceus.service.fcfb.scorebug.PostseasonScorebugRenderer
import com.fcfb.arceus.service.fcfb.scorebug.ScorebugRenderer
import com.fcfb.arceus.service.fcfb.scorebug.TntScorebugRenderer
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
import java.awt.image.BufferedImage
import java.io.File
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
    @Value("\${images.path}")
    private val imagePath: String,
) {
    private val postseasonGameTypes =
        setOf(GameType.BOWL, GameType.PLAYOFFS, GameType.NATIONAL_CHAMPIONSHIP, GameType.CONFERENCE_CHAMPIONSHIP)
    private val foxChannels = setOf(TVChannel.FOX, TVChannel.FS1, TVChannel.FS2, TVChannel.BIG_TEN_NETWORK)
    private val cbsChannels = setOf(TVChannel.CBS, CBS_SPORTS_NETWORK)
    private val tntChannels = setOf(TVChannel.TNT)
    private val nbcChannels = setOf(TVChannel.NBC, TVChannel.PEACOCK)
    private val cwChannels = setOf(TVChannel.THE_CW)
    private val pacTwelveChannels = setOf(TVChannel.PAC_12_NETWORK)

    /**
     * Get the scorebug for a game filtered
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
     */
    fun getScorebugByGameId(gameId: Int): ResponseEntity<ByteArray> {
        val game = gameService.getGameById(gameId)
        generateScorebug(game)

        try {
            val scorebug = File("$imagePath/scorebugs/${game.gameId}_scorebug.png").readBytes()

            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.IMAGE_PNG
                    contentLength = scorebug.size.toLong()
                }

            return ResponseEntity(scorebug, headers, HttpStatus.OK)
        } catch (e: Exception) {
            Logger.error("Error fetching scorebug image: ${e.message}")
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * Get the latest scorebug image for a game without generating
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
     * Generates a scorebug image for the game, dispatching to the appropriate renderer
     */
    fun generateScorebug(game: Game): BufferedImage {
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)

        val renderer: ScorebugRenderer =
            when {
                game.gameType in postseasonGameTypes -> postseasonRenderer
                game.tvChannel in foxChannels -> foxRenderer
                game.tvChannel in cbsChannels -> cbsRenderer
                game.tvChannel in tntChannels -> tntRenderer
                game.tvChannel in nbcChannels -> nbcRenderer
                game.tvChannel in cwChannels -> cwRenderer
                game.tvChannel in pacTwelveChannels -> pacTwelveRenderer
                else -> espnRenderer
            }

        val image = renderer.render(game, homeTeam, awayTeam)

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
}
