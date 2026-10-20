package com.fcfb.arceus.rendering.preview

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.animation.GameSituation
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.rendering.animation.field.FieldBackgroundPainter
import com.fcfb.arceus.rendering.animation.fieldgoal.GoalPostScenePainter
import java.awt.RenderingHints
import java.awt.image.BufferedImage

internal object FieldPreviewRenderer {
    fun fieldWithWallImage(theme: FieldTheme): BufferedImage {
        val field = FieldBackgroundPainter.paint(theme, FIELD_PREVIEW_ZOOM)
        val wall = scaleToWidth(wallImage(theme, theme.leftSide()), field.width)
        val canvas = BufferedImage(field.width, field.height + wall.height, BufferedImage.TYPE_INT_RGB)
        val graphics = canvas.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(field, 0, 0, null)
        graphics.drawImage(wall, 0, field.height, null)
        graphics.dispose()
        return canvas
    }

    fun wallImage(
        theme: FieldTheme,
        side: TeamSide,
    ): BufferedImage {
        val layout =
            GoalPostScenePainter.layoutFor(
                Play().apply {
                    playCall = PlayCall.PAT
                    ballLocation = PAT_BALL_LOCATION
                    possession = TeamSide.HOME
                },
            )
        val situation = GameSituation.sample(theme.homeTeam.name.orEmpty(), theme.awayTeam.name.orEmpty())
        return GoalPostScenePainter.paint(
            theme,
            theme.endZoneOf(side),
            layout,
            midfieldTopOnLeft = true,
            side = side,
            situation = situation,
        )
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

    private const val FIELD_PREVIEW_ZOOM = 0.6f
    private const val PAT_BALL_LOCATION = 97
}
