package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.AppearancePreviewRequest
import com.fcfb.arceus.dto.request.BowlFieldRequest
import com.fcfb.arceus.dto.request.PostseasonFieldRequest
import com.fcfb.arceus.dto.request.PostseasonPreviewRequest
import com.fcfb.arceus.dto.request.TeamColorsRequest
import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.dto.request.TeamUniformRequest
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.ConferenceChampionshipField
import com.fcfb.arceus.model.appearance.PlayoffField
import com.fcfb.arceus.model.appearance.PostseasonField
import com.fcfb.arceus.model.appearance.TeamField
import com.fcfb.arceus.model.appearance.TeamUniformHistory
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.rendering.preview.FieldPreviewRenderer
import com.fcfb.arceus.rendering.preview.UniformPreviewRenderer
import com.fcfb.arceus.rendering.scorebug.EspnScorebugRenderer
import com.fcfb.arceus.service.fcfb.animation.FieldThemeService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.appearance.AppearanceThumbnailCache
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Service
class AppearancePreviewService(
    private val teamService: TeamService,
    private val teamAppearanceService: TeamAppearanceService,
    private val espnScorebugRenderer: EspnScorebugRenderer,
    private val gameRepository: com.fcfb.arceus.repositories.game.GameRepository,
    private val fieldThemeService: FieldThemeService,
    private val bowlFieldService: BowlFieldService,
    private val playoffFieldService: PlayoffFieldService,
    private val conferenceChampionshipFieldService: ConferenceChampionshipFieldService,
    private val fieldAppearanceService: FieldAppearanceService,
    private val postseasonFieldUpdateService: PostseasonFieldUpdateService,
    private val thumbnailCache: AppearanceThumbnailCache,
) {
    fun preview(request: AppearancePreviewRequest): ResponseEntity<ByteArray> = respond(renderImage(request))

    fun thumbnail(
        team: String,
        view: String,
    ): ResponseEntity<ByteArray> {
        val bytes =
            thumbnailCache.getOrRender(thumbnailCache.key(team, view)) {
                toPng(renderImage(AppearancePreviewRequest(team = team, view = view)))
            }
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.IMAGE_PNG
                contentLength = bytes.size.toLong()
                cacheControl = "public, max-age=300"
                eTag = "\"${Integer.toHexString(bytes.contentHashCode())}\""
            }
        return ResponseEntity(bytes, headers, HttpStatus.OK)
    }

    private fun renderImage(request: AppearancePreviewRequest): BufferedImage {
        val homeTeam = teamService.getTeamByName(request.team)
        applyDraftColors(homeTeam, request.colors)
        applyDraftLogos(homeTeam, request)
        val awayTeam = request.opponent?.let { teamService.getTeamByName(it) } ?: homeTeam
        if (request.view.uppercase() == SCOREBUG_VIEW) {
            return scorebugImage(homeTeam, awayTeam)
        }
        val field = draftField(request.team, request.field)
        val uniform = draftUniform(request.team, request.uniform)
        val theme = themeFor(homeTeam, awayTeam, field, uniform)
        return when (request.view.uppercase()) {
            HELMET_VIEW -> UniformPreviewRenderer.helmetImage(theme)
            SECONDARY_HELMET_VIEW -> UniformPreviewRenderer.secondaryHelmetImage(theme)
            UNIFORM_VIEW -> UniformPreviewRenderer.uniformImage(theme, away = false)
            AWAY_UNIFORM_VIEW -> UniformPreviewRenderer.uniformImage(theme, away = true)
            JERSEY_VIEW -> UniformPreviewRenderer.jerseyImage(theme, away = false)
            AWAY_JERSEY_VIEW -> UniformPreviewRenderer.jerseyImage(theme, away = true)
            LEFT_WALL_VIEW -> FieldPreviewRenderer.wallImage(theme, theme.leftSide())
            RIGHT_WALL_VIEW -> FieldPreviewRenderer.wallImage(theme, theme.rightSide())
            else -> FieldPreviewRenderer.fieldWithWallImage(theme)
        }
    }

    private fun applyDraftColors(
        team: Team,
        colors: TeamColorsRequest?,
    ) {
        colors ?: return
        colors.primaryColor?.let { team.primaryColor = it }
        colors.secondaryColor?.let { team.secondaryColor = it }
        colors.tertiaryColor?.let { team.tertiaryColor = it }
    }

    private fun applyDraftLogos(
        team: Team,
        request: AppearancePreviewRequest,
    ) {
        request.logo?.let { team.logo = it.ifBlank { null } }
        request.logoDark?.let { team.logoDark = it.ifBlank { null } }
        request.secondaryLogo?.let { team.secondaryLogo = it.ifBlank { null } }
    }

    private fun scorebugImage(
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage = espnScorebugRenderer.render(sampleGame(homeTeam, awayTeam), homeTeam, awayTeam)

    private fun sampleGame(
        homeTeam: Team,
        awayTeam: Team,
    ): Game =
        Game().apply {
            this.homeTeam = homeTeam.name.orEmpty()
            this.awayTeam = awayTeam.name.orEmpty()
            homeScore = SAMPLE_HOME_SCORE
            awayScore = SAMPLE_AWAY_SCORE
            homeWins = 0
            homeLosses = 0
            awayWins = 0
            awayLosses = 0
            homeTimeouts = 3
            awayTimeouts = 3
            quarter = 3
            clock = "7:00"
            down = 2
            yardsToGo = 7
            ballLocation = 45
            possession = TeamSide.HOME
            currentPlayType = PlayType.NORMAL
            gameStatus = GameStatus.IN_PROGRESS
        }

    private fun themeFor(
        homeTeam: Team,
        awayTeam: Team,
        field: TeamField,
        uniform: TeamUniformHistory,
    ): FieldTheme {
        val game =
            Game().apply {
                this.homeTeam = homeTeam.name.orEmpty()
                this.awayTeam = awayTeam.name.orEmpty()
                gameType = GameType.OUT_OF_CONFERENCE
            }
        val play =
            Play().apply {
                possession = TeamSide.HOME
                ballLocation = PAT_BALL_LOCATION
                quarter = 1
            }
        return fieldThemeService.resolve(play, game, homeTeam, awayTeam, uniform, null, teamFieldOverride = field)
    }

    private fun draftField(
        team: String,
        draft: TeamFieldRequest?,
    ): TeamField {
        val stored = teamAppearanceService.getField(team)
        if (draft == null) return stored
        val clone = TeamField().apply { this.team = stored.team }
        stored.copyAppearanceInto(clone)
        return fieldAppearanceService.applyFields(clone, draft)
    }

    private fun draftUniform(
        team: String,
        draft: TeamUniformRequest?,
    ): TeamUniformHistory {
        val stored = teamAppearanceService.getUniform(team)
        return TeamUniformHistory().apply {
            this.team = stored.team
            primaryColor = stored.primaryColor
            secondaryColor = stored.secondaryColor
            tertiaryColor = draft?.tertiaryColor ?: stored.tertiaryColor
            helmetColor = draft?.helmetColor ?: stored.helmetColor
            secondaryHelmetColor = draft?.secondaryHelmetColor ?: stored.secondaryHelmetColor
            helmetNumberColor = draft?.helmetNumberColor ?: stored.helmetNumberColor
            helmetNumberFont = if (draft != null) draft.helmetNumberFont else stored.helmetNumberFont
            facemaskColor = draft?.facemaskColor ?: stored.facemaskColor
            helmetLogoMode = draft?.helmetLogoMode ?: stored.helmetLogoMode
            helmetLogoSource = draft?.helmetLogoSource ?: stored.helmetLogoSource
            jerseyColor = draft?.jerseyColor ?: stored.jerseyColor
            numberColor = draft?.numberColor ?: stored.numberColor
            jerseyNumberFont = if (draft != null) draft.jerseyNumberFont else stored.jerseyNumberFont
            numberOutlineColor = if (draft != null) draft.numberOutlineColor else stored.numberOutlineColor
            awayNumberColor = draft?.awayNumberColor ?: stored.awayNumberColor
            awayNumberOutlineColor = if (draft != null) draft.awayNumberOutlineColor else stored.awayNumberOutlineColor
            altFacemaskColor = draft?.altFacemaskColor
            altHelmetNumberColor = draft?.altHelmetNumberColor
            altHelmetLogoMode = draft?.altHelmetLogoMode ?: stored.altHelmetLogoMode
            altHelmetLogoSource = draft?.altHelmetLogoSource ?: stored.altHelmetLogoSource
            altHasLogo = draft?.altHasLogo ?: stored.altHasLogo
            altLogoUrl = draft?.altLogoUrl
            altLogoSize = draft?.altLogoSize ?: stored.altLogoSize
            altLogoX = draft?.altLogoX ?: stored.altLogoX
            altLogoY = draft?.altLogoY ?: stored.altLogoY
            altLogoRotation = draft?.altLogoRotation ?: stored.altLogoRotation
            altHasStripe = draft?.altHasStripe ?: stored.altHasStripe
            altStripeColor = draft?.altStripeColor
            altStripeType = draft?.altStripeType ?: stored.altStripeType
            altSecondaryStripeColor = draft?.altSecondaryStripeColor
            pantsColor = draft?.pantsColor ?: stored.pantsColor
            awayPantsColor = draft?.awayPantsColor ?: stored.awayPantsColor
            numberTopText = if (draft != null) draft.numberTopText else stored.numberTopText
            awayNumberTopText = if (draft != null) draft.awayNumberTopText else stored.awayNumberTopText
            bumperColor = if (draft != null) draft.bumperColor else stored.bumperColor
            logoUrl = draft?.logoUrl ?: stored.logoUrl
            hasLogo = draft?.hasLogo ?: stored.hasLogo
            hasStripe = draft?.hasStripe ?: stored.hasStripe
            stripeColor = draft?.stripeColor ?: stored.stripeColor
            stripeType = draft?.stripeType ?: stored.stripeType
            secondaryStripeColor = draft?.secondaryStripeColor ?: stored.secondaryStripeColor
            logoSize = draft?.logoSize ?: stored.logoSize
            logoX = draft?.logoX ?: stored.logoX
            logoY = draft?.logoY ?: stored.logoY
            logoRotation = draft?.logoRotation ?: stored.logoRotation
        }
    }

    fun previewPostseason(request: PostseasonPreviewRequest): ResponseEntity<ByteArray> {
        val category = request.category.uppercase()
        val game =
            when (category) {
                "BOWL" -> gameRepository.getLastBowlGame(request.key)
                "PLAYOFF" ->
                    if (request.key.equals(NATIONAL_CHAMPIONSHIP, ignoreCase = true)) {
                        gameRepository.getLastNationalChampionshipGame()
                    } else {
                        gameRepository.getLastPlayoffGame(request.key)
                    }
                "CCG" -> gameRepository.getLastConferenceChampionshipGame(request.key)
                else -> null
            } ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        val home = teamService.getTeamByName(game.homeTeam)
        val away = teamService.getTeamByName(game.awayTeam)
        val play =
            Play().apply {
                possession = TeamSide.HOME
                ballLocation = PAT_BALL_LOCATION
                quarter = 1
            }
        val bowlOverride = if (category == "BOWL") draftBowl(request.key, request.bowl) else null
        val postseasonOverride =
            when (category) {
                "PLAYOFF" ->
                    draftPostseason(
                        playoffFieldService.getField(request.key),
                        PlayoffField().apply { round = request.key },
                        request.postseason,
                    )
                "CCG" ->
                    draftPostseason(
                        conferenceChampionshipFieldService.getField(request.key),
                        ConferenceChampionshipField().apply { conference = request.key },
                        request.postseason,
                    )
                else -> null
            }
        val theme = fieldThemeService.resolve(play, game, home, away, null, null, bowlOverride, postseasonOverride)
        return respond(FieldPreviewRenderer.fieldWithWallImage(theme))
    }

    private fun draftBowl(
        key: String,
        request: BowlFieldRequest?,
    ): BowlField {
        val stored = bowlFieldService.getField(key)
        if (request == null) return stored
        val clone = BowlField().apply { bowl = stored.bowl }
        stored.copyInto(clone)
        bowlFieldService.applyFields(clone, request)
        return clone
    }

    private fun <T : PostseasonField> draftPostseason(
        stored: PostseasonField,
        target: T,
        request: PostseasonFieldRequest?,
    ): T {
        stored.copyInto(target)
        request?.let { postseasonFieldUpdateService.applyFields(target, it) }
        return target
    }

    private fun respond(image: BufferedImage): ResponseEntity<ByteArray> {
        val bytes = toPng(image)
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.IMAGE_PNG
                contentLength = bytes.size.toLong()
            }
        return ResponseEntity(bytes, headers, HttpStatus.OK)
    }

    private fun toPng(image: BufferedImage): ByteArray = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()

    companion object {
        private const val HELMET_VIEW = "HELMET"
        private const val UNIFORM_VIEW = "UNIFORM"
        private const val SECONDARY_HELMET_VIEW = "SECONDARY_HELMET"
        private const val AWAY_UNIFORM_VIEW = "AWAY_UNIFORM"
        private const val JERSEY_VIEW = "JERSEY"
        private const val AWAY_JERSEY_VIEW = "AWAY_JERSEY"
        private const val LEFT_WALL_VIEW = "LEFT_WALL"
        private const val RIGHT_WALL_VIEW = "RIGHT_WALL"
        private const val SCOREBUG_VIEW = "SCOREBUG"
        private const val SAMPLE_HOME_SCORE = 21
        private const val SAMPLE_AWAY_SCORE = 17
        private const val PAT_BALL_LOCATION = 97
        private const val NATIONAL_CHAMPIONSHIP = "National Championship"
    }
}
