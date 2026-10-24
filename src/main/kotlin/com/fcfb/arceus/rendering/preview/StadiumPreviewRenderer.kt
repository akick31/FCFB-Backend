package com.fcfb.arceus.rendering.preview

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.animation.GameSituation
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.rendering.animation.fieldgoal.GoalPostScenePainter
import java.awt.RenderingHints
import java.awt.image.BufferedImage

internal object StadiumPreviewRenderer {
    private const val CLOSE_BALL_LOCATION = 97
    private const val LONG_BALL_LOCATION = 55
    private const val VIEW_HEIGHT = 340
    private const val OUTPUT_SCALE = 0.5f

    fun closeAndLongBothEnds(theme: FieldTheme): BufferedImage {
        val sides = listOf(theme.leftSide(), theme.rightSide())
        val distances = listOf(CLOSE_BALL_LOCATION, LONG_BALL_LOCATION)
        val viewWidth = (GoalPostScenePainter.SCENE_WIDTH * OUTPUT_SCALE).toInt()
        val viewHeight = (VIEW_HEIGHT * OUTPUT_SCALE).toInt()
        val sheet = BufferedImage(viewWidth * sides.size, viewHeight * distances.size, BufferedImage.TYPE_INT_RGB)
        val graphics = sheet.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        distances.forEachIndexed { row, ballLocation ->
            sides.forEachIndexed { column, side ->
                val scene = sceneAt(theme, side, ballLocation).getSubimage(0, 0, GoalPostScenePainter.SCENE_WIDTH, VIEW_HEIGHT)
                graphics.drawImage(scene, column * viewWidth, row * viewHeight, viewWidth, viewHeight, null)
            }
        }
        graphics.dispose()
        return sheet
    }

    private fun sceneAt(
        theme: FieldTheme,
        side: TeamSide,
        ballLocation: Int,
    ): BufferedImage {
        val layout =
            GoalPostScenePainter.layoutFor(
                Play().apply {
                    playCall = PlayCall.FIELD_GOAL
                    this.ballLocation = ballLocation
                    possession = TeamSide.HOME
                },
            )
        val situation = GameSituation.sample(GameSituation.scoreboardName(theme.homeTeam), GameSituation.scoreboardName(theme.awayTeam))
        return GoalPostScenePainter.paint(
            theme,
            theme.endZoneOf(side),
            layout,
            midfieldTopOnLeft = true,
            side = side,
            situation = situation,
        )
    }
}
