package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.AppearancePreviewRequest
import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.dto.request.TeamUniformRequest
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamField
import com.fcfb.arceus.model.TeamUniformHistory
import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.service.fcfb.animation.FieldStyle
import com.fcfb.arceus.service.fcfb.animation.FieldTheme
import com.fcfb.arceus.service.fcfb.animation.HelmetSprite
import com.fcfb.arceus.service.fcfb.animation.LogoLoader
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
) {
    fun preview(request: AppearancePreviewRequest): ResponseEntity<ByteArray> {
        val homeTeam = teamService.getTeamByName(request.team)
        val awayTeam = request.opponent?.let { teamService.getTeamByName(it) } ?: homeTeam
        val field = draftField(request.team, request.field)
        val uniform = draftUniform(request.team, request.uniform)
        val theme = themeFor(homeTeam, awayTeam, field, uniform)
        val image =
            when (request.view.uppercase()) {
                HELMET_VIEW -> helmetImage(theme)
                else -> FieldBackgroundPainter.paint(theme)
            }
        return respond(image)
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
    ): FieldTheme =
        FieldTheme(
            style = FieldStyle.HOME_FIELD,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            centerLogoUrl = field.midfieldLogoUrl ?: homeTeam.scorebugLogo,
            turf = FieldBackgroundPainter.parseColor(field.turfColor),
            homeUniform = uniform,
            wallCaption = field.wallText,
            homeField = field,
        )

    private fun helmetImage(theme: FieldTheme): BufferedImage {
        val (home, _) = theme.uniforms()
        val logo = if (home.helmetLogoMode.drawsLogo) LogoLoader.load(theme.homeLogoUrl()) else null
        return HelmetSprite.render(home, logo, HELMET_PREVIEW_SIZE).facingRight
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
            endZoneFont = draft.endZoneFont ?: stored.endZoneFont
            midfieldLogoUrl = draft.midfieldLogoUrl ?: stored.midfieldLogoUrl
            quarterLogoUrl = draft.quarterLogoUrl ?: stored.quarterLogoUrl
            fieldNumberOutlineColor = draft.fieldNumberOutlineColor
            redZoneBorderColor = draft.redZoneBorderColor
            oobLineColor = draft.oobLineColor
            wallColor = draft.wallColor ?: stored.wallColor
            wallDesign = draft.wallDesign ?: stored.wallDesign
            wallText = draft.wallText
            wallTextOutlineColor = draft.wallTextOutlineColor
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
            jerseyColor = draft?.jerseyColor ?: stored.jerseyColor
            numberColor = draft?.numberColor ?: stored.numberColor
            numberOutlineColor = draft?.numberOutlineColor ?: stored.numberOutlineColor
            pantsColor = draft?.pantsColor ?: stored.pantsColor
            logoUrl = draft?.logoUrl ?: stored.logoUrl
            hasLogo = draft?.hasLogo ?: stored.hasLogo
            hasStripe = draft?.hasStripe ?: stored.hasStripe
            stripeColor = draft?.stripeColor ?: stored.stripeColor
        }
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
        private const val HELMET_PREVIEW_SIZE = 220
    }
}
