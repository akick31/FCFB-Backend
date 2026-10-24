package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.StadiumPreviewRequest
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.rendering.preview.StadiumPreviewRenderer
import com.fcfb.arceus.service.fcfb.animation.FieldThemeService
import com.fcfb.arceus.service.fcfb.team.TeamService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Service
class StadiumPreviewService(
    private val stadiumService: StadiumService,
    private val teamService: TeamService,
    private val fieldThemeService: FieldThemeService,
) {
    fun preview(request: StadiumPreviewRequest): ResponseEntity<ByteArray> {
        val home = teamFor(request.team, request.venue) ?: placeholder(PLACEHOLDER_HOME, PLACEHOLDER_HOME_COLORS)
        val away = request.opponent?.let { teamService.getTeamByName(it) } ?: placeholder(PLACEHOLDER_AWAY, PLACEHOLDER_AWAY_COLORS)
        val game =
            Game().apply {
                homeTeam = home.name.orEmpty()
                awayTeam = away.name.orEmpty()
                gameType = GameType.OUT_OF_CONFERENCE
            }
        val play =
            Play().apply {
                possession = TeamSide.HOME
                quarter = 1
            }
        val design = stadiumService.draftDesign(request.venue, request.design)
        val theme = fieldThemeService.resolve(play, game, home, away, stadiumOverride = design)
        val bytes =
            ByteArrayOutputStream().also {
                ImageIO.write(
                    StadiumPreviewRenderer.closeAndLongBothEnds(theme),
                    "png",
                    it,
                )
            }.toByteArray()
        val headers = HttpHeaders().apply { contentType = MediaType.IMAGE_PNG }
        return ResponseEntity(bytes, headers, HttpStatus.OK)
    }

    private fun teamFor(
        team: String?,
        venue: String?,
    ): Team? = team?.let { teamService.getTeamByName(it) } ?: venue?.let { stadiumService.teamsAt(it).firstOrNull() }

    private fun placeholder(
        name: String,
        colors: Pair<String, String>,
    ): Team =
        Team().apply {
            this.name = name
            abbreviation = name
            primaryColor = colors.first
            secondaryColor = colors.second
        }

    private companion object {
        const val PLACEHOLDER_HOME = "HOME"
        const val PLACEHOLDER_AWAY = "AWAY"
        val PLACEHOLDER_HOME_COLORS = "#1f3b73" to "#c8102e"
        val PLACEHOLDER_AWAY_COLORS = "#3a3a3a" to "#d9d9d9"
    }
}
