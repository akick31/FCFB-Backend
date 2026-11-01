package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.rendering.animation.field.FieldBackgroundPainter
import com.fcfb.arceus.service.fcfb.animation.FieldThemeService
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.appearance.AppearanceThumbnailCache
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Service
class GameFieldPreviewService(
    private val gameService: GameService,
    private val teamService: TeamService,
    private val fieldThemeService: FieldThemeService,
    private val thumbnailCache: AppearanceThumbnailCache,
) {
    fun gameField(gameId: Int): ResponseEntity<ByteArray> {
        val game = gameService.getGameById(gameId)
        val bytes =
            thumbnailCache.getOrRender(thumbnailCache.key(game.homeTeam, "GAME_FIELD_$gameId")) {
                renderField(gameId)
            }
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.IMAGE_PNG
                contentLength = bytes.size.toLong()
                cacheControl = "public, max-age=300"
                eTag = "\"${Integer.toHexString(bytes.contentHashCode())}\""
                set(FIELD_WIDTH_HEADER, FieldBackgroundPainter.WIDTH.toString())
                set(FIELD_HEIGHT_HEADER, FieldBackgroundPainter.HEIGHT.toString())
                set(FIELD_MARGIN_HEADER, FieldBackgroundPainter.MARGIN.toString())
                accessControlExposeHeaders = listOf(FIELD_WIDTH_HEADER, FIELD_HEIGHT_HEADER, FIELD_MARGIN_HEADER)
            }
        return ResponseEntity(bytes, headers, HttpStatus.OK)
    }

    private fun renderField(gameId: Int): ByteArray {
        val game = gameService.getGameById(gameId)
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)
        val firstHalfPlay =
            Play().apply {
                possession = TeamSide.HOME
                ballLocation = MIDFIELD
                quarter = 1
            }
        val theme = fieldThemeService.resolve(firstHalfPlay, game, homeTeam, awayTeam)
        return ByteArrayOutputStream().also { ImageIO.write(FieldBackgroundPainter.paint(theme), "png", it) }.toByteArray()
    }

    companion object {
        const val FIELD_WIDTH_HEADER = "X-Field-Width"
        const val FIELD_HEIGHT_HEADER = "X-Field-Height"
        const val FIELD_MARGIN_HEADER = "X-Field-Margin"
        private const val MIDFIELD = 50
    }
}
