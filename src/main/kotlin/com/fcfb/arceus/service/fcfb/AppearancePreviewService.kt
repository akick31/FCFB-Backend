package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.AppearancePreviewRequest
import com.fcfb.arceus.dto.request.PostseasonPreviewRequest
import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.dto.request.TeamUniformRequest
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.ConferenceChampionshipField
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.PlayoffField
import com.fcfb.arceus.model.PostseasonField
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamField
import com.fcfb.arceus.model.TeamUniformHistory
import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.service.fcfb.animation.FieldGoalFigure
import com.fcfb.arceus.service.fcfb.animation.FieldGoalPlayerPainter
import com.fcfb.arceus.service.fcfb.animation.FieldTheme
import com.fcfb.arceus.service.fcfb.animation.GoalPostScenePainter
import com.fcfb.arceus.service.fcfb.animation.HelmetFields
import com.fcfb.arceus.service.fcfb.animation.HelmetLogoMode
import com.fcfb.arceus.service.fcfb.animation.HelmetSprite
import com.fcfb.arceus.service.fcfb.animation.LogoLoader
import com.fcfb.arceus.service.fcfb.animation.LogoSource
import com.fcfb.arceus.service.fcfb.animation.PlayerPose
import com.fcfb.arceus.service.fcfb.animation.Uniforms
import com.fcfb.arceus.service.fcfb.scorebug.EspnScorebugRenderer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Service
class AppearancePreviewService(
    private val teamService: TeamService,
    private val teamAppearanceService: TeamAppearanceService,
    private val espnScorebugRenderer: EspnScorebugRenderer,
    private val gameRepository: com.fcfb.arceus.repositories.GameRepository,
    private val fieldThemeResolver: com.fcfb.arceus.service.fcfb.animation.FieldThemeResolver,
    private val bowlFieldService: BowlFieldService,
    private val playoffFieldService: PlayoffFieldService,
    private val conferenceChampionshipFieldService: ConferenceChampionshipFieldService,
) {
    fun preview(request: AppearancePreviewRequest): ResponseEntity<ByteArray> {
        val homeTeam = teamService.getTeamByName(request.team)
        applyDraftColors(homeTeam, request.colors)
        applyDraftLogos(homeTeam, request)
        val awayTeam = request.opponent?.let { teamService.getTeamByName(it) } ?: homeTeam
        if (request.view.uppercase() == SCOREBUG_VIEW) {
            return respond(scorebugImage(homeTeam, awayTeam))
        }
        val field = draftField(request.team, request.field)
        val uniform = draftUniform(request.team, request.uniform)
        val theme = themeFor(homeTeam, awayTeam, field, uniform)
        val image =
            when (request.view.uppercase()) {
                HELMET_VIEW -> helmetImage(theme)
                SECONDARY_HELMET_VIEW -> secondaryHelmetImage(theme)
                UNIFORM_VIEW -> uniformImage(theme, away = false)
                AWAY_UNIFORM_VIEW -> uniformImage(theme, away = true)
                else -> fieldWithWallImage(theme)
            }
        return respond(image)
    }

    /** Draft colors are applied to the detached team instance only, so the preview never persists unsaved edits. */
    private fun applyDraftColors(
        team: Team,
        colors: com.fcfb.arceus.dto.request.TeamColorsRequest?,
    ) {
        colors ?: return
        colors.primaryColor?.let { team.primaryColor = it }
        colors.secondaryColor?.let { team.secondaryColor = it }
        colors.tertiaryColor?.let { team.tertiaryColor = it }
    }

    /** Draft logos applied to the detached team for the Logo tab preview; never persisted. */
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

    /** The detailed helmet beside a uniformed player, so the uniform editor and the admin list show the whole kit at once. */
    private fun uniformImage(
        theme: FieldTheme,
        away: Boolean,
    ): BufferedImage {
        val (homeUniform, awayUniform) = theme.uniforms()
        val home = if (away) awayUniform else homeUniform
        val logoUrl = if (away) theme.awayLogoUrl() else theme.homeLogoUrl()
        val logo = if (home.helmetLogoMode.drawsLogo) LogoLoader.loadFirst(logoUrl, theme.homeTeam.scorebugLogo) else null
        val helmet = HelmetSprite.render(home, logo, COMBINED_HELMET_SIZE).facingRight
        val canvas = BufferedImage(COMBINED_WIDTH, COMBINED_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val graphics = canvas.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.drawImage(helmet, HELMET_INSET, (COMBINED_HEIGHT - helmet.height) / 2, null)
        graphics.dispose()
        val figure =
            FieldGoalFigure(
                x = PLAYER_CENTER_X,
                footY = COMBINED_HEIGHT - PLAYER_FOOT_MARGIN,
                scale = PLAYER_SCALE,
                uniform = home,
                number = PLAYER_NUMBER,
                pose = PlayerPose.STANDING,
                facingCamera = true,
            )
        FieldGoalPlayerPainter.draw(canvas, figure)
        return canvas
    }

    /**
     * Built directly rather than through `FieldThemeResolver`, so `wallCaption` has to be set here — leaving it null
     * silently renders the text wall designs blank.
     */
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
        return fieldThemeResolver.resolve(play, game, homeTeam, awayTeam, uniform, null, teamFieldOverride = field)
    }

    private fun helmetImage(theme: FieldTheme): BufferedImage {
        val (home, _) = theme.uniforms()
        val logo = if (home.helmetLogoMode.drawsLogo) LogoLoader.loadFirst(theme.homeLogoUrl(), theme.homeTeam.scorebugLogo) else null
        return HelmetSprite.render(home, logo, HELMET_PREVIEW_SIZE).facingRight
    }

    private fun secondaryHelmetImage(theme: FieldTheme): BufferedImage {
        val uniform = Uniforms.secondaryHelmet(theme.homeTeam, theme.homeUniform)
        val logo = if (uniform.helmetLogoMode.drawsLogo) LogoLoader.loadFirst(altDecalUrl(theme), theme.homeTeam.scorebugLogo) else null
        return HelmetSprite.render(uniform, logo, HELMET_PREVIEW_SIZE).facingRight
    }

    private fun altDecalUrl(theme: FieldTheme): String? {
        val snapshot = theme.homeUniform
        val team = theme.homeTeam
        val mode = HelmetLogoMode.from(HelmetFields.helmetLogoMode(snapshot, true), HelmetFields.hasLogo(snapshot, true))
        if (mode == HelmetLogoMode.UPLOAD) return HelmetFields.logoUrl(snapshot, true)
        return when (LogoSource.from(HelmetFields.helmetLogoSource(snapshot, true))) {
            LogoSource.SECONDARY -> team.secondaryLogo
            else -> team.logo
        }
    }

    /** Never persisted: a preview that wrote to the repository would save edits the user has not approved. */
    private fun draftField(
        team: String,
        draft: TeamFieldRequest?,
    ): TeamField {
        val stored = teamAppearanceService.getField(team)
        if (draft == null) return stored
        return TeamField().apply {
            this.team = stored.team
            turfColor = draft.turfColor ?: stored.turfColor
            endZoneColor = draft.endZoneColor ?: stored.endZoneColor
            endZoneTextColor = draft.endZoneTextColor
            endZoneOutlineColor = draft.endZoneOutlineColor
            endZoneTextLeft = draft.endZoneTextLeft
            endZoneTextRight = draft.endZoneTextRight
            endZoneLogoEnabled = draft.endZoneLogoEnabled ?: stored.endZoneLogoEnabled
            endZoneLogoSource = draft.endZoneLogoSource ?: stored.endZoneLogoSource
            endZoneLogoUrl = draft.endZoneLogoUrl
            endZoneLogoSize = draft.endZoneLogoSize ?: stored.endZoneLogoSize
            endZoneOutlineEnabled = draft.endZoneOutlineEnabled ?: stored.endZoneOutlineEnabled
            wallLogoSource = draft.wallLogoSource ?: stored.wallLogoSource
            wallLogoUrl = draft.wallLogoUrl
            recolorConferenceLogo = draft.recolorConferenceLogo ?: stored.recolorConferenceLogo
            midfieldLogoSize = draft.midfieldLogoSize ?: stored.midfieldLogoSize
            leftEndZoneFont = draft.leftEndZoneFont ?: stored.leftEndZoneFont
            rightEndZoneFont = draft.rightEndZoneFont ?: stored.rightEndZoneFont
            rightWallDesign = draft.rightWallDesign ?: stored.rightWallDesign
            rightWallText = draft.rightWallText
            yardNumberFont = draft.yardNumberFont ?: stored.yardNumberFont
            conferenceLogoColorMap = draft.conferenceLogoColorMap
            endZoneFont = draft.endZoneFont ?: stored.endZoneFont
            midfieldLogoUrl = draft.midfieldLogoUrl ?: stored.midfieldLogoUrl
            midfieldLogoSource = draft.midfieldLogoSource ?: stored.midfieldLogoSource
            quarterLogoUrl = draft.quarterLogoUrl ?: stored.quarterLogoUrl
            quarterLogoSource = draft.quarterLogoSource ?: stored.quarterLogoSource
            fieldNumberOutlineColor = draft.fieldNumberOutlineColor
            redZoneBorderColor = draft.redZoneBorderColor
            oobLineColor = draft.oobLineColor
            wallColor = draft.wallColor ?: stored.wallColor
            wallDesign = draft.wallDesign ?: stored.wallDesign
            wallText = draft.wallText
            wallTextOutlineColor = draft.wallTextOutlineColor
            goalPostColor = draft.goalPostColor ?: stored.goalPostColor
            goalPostStyle = draft.goalPostStyle ?: stored.goalPostStyle
        }
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
            facemaskColor = draft?.facemaskColor ?: stored.facemaskColor
            helmetLogoMode = draft?.helmetLogoMode ?: stored.helmetLogoMode
            helmetLogoSource = draft?.helmetLogoSource ?: stored.helmetLogoSource
            jerseyColor = draft?.jerseyColor ?: stored.jerseyColor
            numberColor = draft?.numberColor ?: stored.numberColor
            numberOutlineColor = draft?.numberOutlineColor ?: stored.numberOutlineColor
            awayNumberColor = draft?.awayNumberColor ?: stored.awayNumberColor
            awayNumberOutlineColor = draft?.awayNumberOutlineColor ?: stored.awayNumberOutlineColor
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

    /** The overhead field above the end-zone wall and goal posts, so the field preview shows both at once. */
    private fun fieldWithWallImage(theme: FieldTheme): BufferedImage {
        val field = FieldBackgroundPainter.paint(theme, FIELD_PREVIEW_ZOOM)
        val layout =
            GoalPostScenePainter.layoutFor(
                Play().apply {
                    playCall = PlayCall.PAT
                    ballLocation = PAT_BALL_LOCATION
                    possession = TeamSide.HOME
                },
            )
        val wall =
            scaleToWidth(GoalPostScenePainter.paint(theme, theme.endZoneOf(TeamSide.HOME), layout, midfieldTopOnLeft = true), field.width)
        val canvas = BufferedImage(field.width, field.height + wall.height, BufferedImage.TYPE_INT_RGB)
        val graphics = canvas.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(field, 0, 0, null)
        graphics.drawImage(wall, 0, field.height, null)
        graphics.dispose()
        return canvas
    }

    private fun scaleToWidth(
        image: BufferedImage,
        width: Int,
    ): BufferedImage {
        if (image.width == width) return image
        val height = image.height * width / image.width
        val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = scaled.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(image, 0, 0, width, height, null)
        graphics.dispose()
        return scaled
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
        val theme = fieldThemeResolver.resolve(play, game, home, away, null, null, bowlOverride, postseasonOverride)
        return respond(fieldWithWallImage(theme))
    }

    private fun draftBowl(
        key: String,
        request: com.fcfb.arceus.dto.request.BowlFieldRequest?,
    ): com.fcfb.arceus.model.BowlField {
        val stored = bowlFieldService.getField(key)
        return com.fcfb.arceus.model.BowlField().apply {
            bowl = stored.bowl
            turfColor = request?.turfColor ?: stored.turfColor
            endZoneFill = request?.endZoneFill ?: stored.endZoneFill
            endZoneFont = request?.endZoneFont ?: stored.endZoneFont
            leftEndZoneLogoUrl = request?.leftEndZoneLogoUrl ?: stored.leftEndZoneLogoUrl
            rightEndZoneLogoUrl = request?.rightEndZoneLogoUrl ?: stored.rightEndZoneLogoUrl
            leftEndZoneText = request?.leftEndZoneText ?: stored.leftEndZoneText
            rightEndZoneText = request?.rightEndZoneText ?: stored.rightEndZoneText
            leftEndZoneLogoSource = request?.leftEndZoneLogoSource ?: stored.leftEndZoneLogoSource
            rightEndZoneLogoSource = request?.rightEndZoneLogoSource ?: stored.rightEndZoneLogoSource
            leftEndZoneFont = request?.leftEndZoneFont ?: stored.leftEndZoneFont
            rightEndZoneFont = request?.rightEndZoneFont ?: stored.rightEndZoneFont
            rightWallDesign = request?.rightWallDesign ?: stored.rightWallDesign
            rightWallText = request?.rightWallText ?: stored.rightWallText
            yardNumberFont = request?.yardNumberFont ?: stored.yardNumberFont
            showConferenceLogos = request?.showConferenceLogos ?: stored.showConferenceLogos
            yardNumberSource = request?.yardNumberSource ?: stored.yardNumberSource
            yardNumberOutlineColor = request?.yardNumberOutlineColor ?: stored.yardNumberOutlineColor
            leftOobLineColor = request?.leftOobLineColor ?: stored.leftOobLineColor
            rightOobLineColor = request?.rightOobLineColor ?: stored.rightOobLineColor
            redZoneEnabled = request?.redZoneEnabled ?: stored.redZoneEnabled
            redZoneBorderColor = request?.redZoneBorderColor ?: stored.redZoneBorderColor
            wallColor = request?.wallColor ?: stored.wallColor
            wallDesign = request?.wallDesign ?: stored.wallDesign
            wallText = request?.wallText ?: stored.wallText
            wallTextOutlineColor = request?.wallTextOutlineColor ?: stored.wallTextOutlineColor
            goalPostColor = request?.goalPostColor ?: stored.goalPostColor
            goalPostStyle = request?.goalPostStyle ?: stored.goalPostStyle
        }
    }

    private fun <T : PostseasonField> draftPostseason(
        stored: PostseasonField,
        target: T,
        request: com.fcfb.arceus.dto.request.PostseasonFieldRequest?,
    ): T {
        target.turfColor = request?.turfColor ?: stored.turfColor
        target.endZoneFont = request?.endZoneFont ?: stored.endZoneFont
        target.centerLogoUrl = request?.centerLogoUrl ?: stored.centerLogoUrl
        target.wallColor = request?.wallColor ?: stored.wallColor
        target.wallDesign = request?.wallDesign ?: stored.wallDesign
        target.wallText = request?.wallText ?: stored.wallText
        target.wallTextOutlineColor = request?.wallTextOutlineColor ?: stored.wallTextOutlineColor
        target.goalPostColor = request?.goalPostColor ?: stored.goalPostColor
        target.goalPostStyle = request?.goalPostStyle ?: stored.goalPostStyle
        target.yardNumberOutlineColor = request?.yardNumberOutlineColor ?: stored.yardNumberOutlineColor
        target.redZoneBorderColor = request?.redZoneBorderColor ?: stored.redZoneBorderColor
        target.sidelineAccentColor = request?.sidelineAccentColor ?: stored.sidelineAccentColor
        target.leftSidelineColor = request?.leftSidelineColor ?: stored.leftSidelineColor
        target.rightSidelineColor = request?.rightSidelineColor ?: stored.rightSidelineColor
        target.leftRedZoneColor = request?.leftRedZoneColor ?: stored.leftRedZoneColor
        target.rightRedZoneColor = request?.rightRedZoneColor ?: stored.rightRedZoneColor
        target.leftEndZoneText = request?.leftEndZoneText ?: stored.leftEndZoneText
        target.rightEndZoneText = request?.rightEndZoneText ?: stored.rightEndZoneText
        target.leftEndZoneLogoUrl = request?.leftEndZoneLogoUrl ?: stored.leftEndZoneLogoUrl
        target.rightEndZoneLogoUrl = request?.rightEndZoneLogoUrl ?: stored.rightEndZoneLogoUrl
        target.leftEndZoneLogoSource = request?.leftEndZoneLogoSource ?: stored.leftEndZoneLogoSource
        target.rightEndZoneLogoSource = request?.rightEndZoneLogoSource ?: stored.rightEndZoneLogoSource
        target.leftEndZoneFont = request?.leftEndZoneFont ?: stored.leftEndZoneFont
        target.rightEndZoneFont = request?.rightEndZoneFont ?: stored.rightEndZoneFont
        target.rightWallDesign = request?.rightWallDesign ?: stored.rightWallDesign
        target.rightWallText = request?.rightWallText ?: stored.rightWallText
        target.yardNumberFont = request?.yardNumberFont ?: stored.yardNumberFont
        target.yardNumberSource = request?.yardNumberSource ?: stored.yardNumberSource
        return target
    }

    private fun respond(image: BufferedImage): ResponseEntity<ByteArray> {
        val bytes = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.IMAGE_PNG
                contentLength = bytes.size.toLong()
            }
        return ResponseEntity(bytes, headers, HttpStatus.OK)
    }

    companion object {
        private const val HELMET_VIEW = "HELMET"
        private const val UNIFORM_VIEW = "UNIFORM"
        private const val SECONDARY_HELMET_VIEW = "SECONDARY_HELMET"
        private const val AWAY_UNIFORM_VIEW = "AWAY_UNIFORM"
        private const val SCOREBUG_VIEW = "SCOREBUG"
        private const val SAMPLE_HOME_SCORE = 21
        private const val SAMPLE_AWAY_SCORE = 17
        private const val FIELD_PREVIEW_ZOOM = 0.6f
        private const val PAT_BALL_LOCATION = 97
        private const val NATIONAL_CHAMPIONSHIP = "National Championship"
        private const val HELMET_PREVIEW_SIZE = 220
        private const val COMBINED_WIDTH = 420
        private const val COMBINED_HEIGHT = 240
        private const val COMBINED_HELMET_SIZE = 200
        private const val HELMET_INSET = 12
        private const val PLAYER_CENTER_X = 320f
        private const val PLAYER_FOOT_MARGIN = 18f
        private const val PLAYER_SCALE = 2.3f
        private const val PLAYER_NUMBER = 1
    }
}
