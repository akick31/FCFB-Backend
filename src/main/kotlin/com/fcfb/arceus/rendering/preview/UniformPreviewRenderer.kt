package com.fcfb.arceus.rendering.preview

import com.fcfb.arceus.enums.animation.HelmetLogoMode
import com.fcfb.arceus.enums.animation.LogoSource
import com.fcfb.arceus.enums.animation.PlayerPose
import com.fcfb.arceus.model.animation.FieldGoalFigure
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.rendering.animation.appearance.HelmetFields
import com.fcfb.arceus.rendering.animation.appearance.HelmetSprite
import com.fcfb.arceus.rendering.animation.appearance.Uniforms
import com.fcfb.arceus.rendering.animation.assets.LogoLoader
import com.fcfb.arceus.rendering.animation.fieldgoal.FieldGoalPlayerPainter
import java.awt.RenderingHints
import java.awt.image.BufferedImage

internal object UniformPreviewRenderer {
    fun uniformImage(
        theme: FieldTheme,
        away: Boolean,
    ): BufferedImage {
        val home = if (away) Uniforms().awayJerseyPreview(theme.homeTeam, theme.homeUniform) else theme.uniforms().first
        val logoUrl = if (away) altDecalUrl(theme) else theme.homeLogoUrl()
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

    fun jerseyImage(
        theme: FieldTheme,
        away: Boolean,
    ): BufferedImage {
        val uniform = if (away) Uniforms().awayJerseyPreview(theme.homeTeam, theme.homeUniform) else theme.uniforms().first
        val canvas = BufferedImage(JERSEY_WIDTH, JERSEY_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val figure =
            FieldGoalFigure(
                x = JERSEY_WIDTH / 2f,
                footY = JERSEY_HEIGHT - PLAYER_FOOT_MARGIN,
                scale = JERSEY_PLAYER_SCALE,
                uniform = uniform,
                number = PLAYER_NUMBER,
                pose = PlayerPose.STANDING,
                facingCamera = true,
            )
        FieldGoalPlayerPainter.draw(canvas, figure)
        return canvas
    }

    fun helmetImage(theme: FieldTheme): BufferedImage {
        val (home, _) = theme.uniforms()
        val logo = if (home.helmetLogoMode.drawsLogo) LogoLoader.loadFirst(theme.homeLogoUrl(), theme.homeTeam.scorebugLogo) else null
        return HelmetSprite.render(home, logo, HELMET_PREVIEW_SIZE).facingRight
    }

    fun secondaryHelmetImage(theme: FieldTheme): BufferedImage {
        val uniform = Uniforms().secondaryHelmet(theme.homeTeam, theme.homeUniform)
        val logo = if (uniform.helmetLogoMode.drawsLogo) LogoLoader.loadFirst(altDecalUrl(theme), theme.homeTeam.scorebugLogo) else null
        return HelmetSprite.render(uniform, logo, HELMET_PREVIEW_SIZE).facingRight
    }

    private fun altDecalUrl(theme: FieldTheme): String? {
        val snapshot = theme.homeUniform
        val team = theme.homeTeam
        val mode = HelmetLogoMode.from(HelmetFields().helmetLogoMode(snapshot, true), HelmetFields().hasLogo(snapshot, true))
        if (mode == HelmetLogoMode.UPLOAD) return HelmetFields().logoUrl(snapshot, true)
        return when (LogoSource.from(HelmetFields().helmetLogoSource(snapshot, true))) {
            LogoSource.SECONDARY -> team.secondaryLogo
            else -> team.logo
        }
    }

    private const val JERSEY_WIDTH = 200
    private const val JERSEY_HEIGHT = 240
    private const val JERSEY_PLAYER_SCALE = 2.7f
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
