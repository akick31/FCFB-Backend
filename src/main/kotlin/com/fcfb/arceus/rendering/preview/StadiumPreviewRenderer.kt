package com.fcfb.arceus.rendering.preview

import com.fcfb.arceus.enums.animation.StadiumPreviewView
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.animation.GameSituation
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.rendering.animation.fieldgoal.GoalPostScenePainter
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

internal object StadiumPreviewRenderer {
    private const val CLOSE_BALL_LOCATION = 97
    private const val LONG_BALL_LOCATION = 55
    private const val VIEW_HEIGHT = 340
    private const val OUTPUT_SCALE = 0.75f
    const val GUTTER = 12
    private const val DIVIDER_WIDTH = 2
    private val GUTTER_COLOR = Color(14, 17, 22)
    val DIVIDER_COLOR = Color(214, 220, 228)

    private const val SCOREBOARD_PADDING = 0.12f
    private const val SCOREBOARD_OUTPUT_WIDTH = 600

    fun render(
        theme: FieldTheme,
        view: StadiumPreviewView,
    ): BufferedImage =
        when (view) {
            StadiumPreviewView.BOTH -> closeAndLongBothEnds(theme)
            StadiumPreviewView.LEFT -> closeAndLong(theme, theme.leftSide())
            StadiumPreviewView.RIGHT -> closeAndLong(theme, theme.rightSide())
            StadiumPreviewView.LEFT_SCOREBOARD -> scoreboardCloseUp(theme, theme.leftSide()) ?: closeAndLong(theme, theme.leftSide())
            StadiumPreviewView.RIGHT_SCOREBOARD -> scoreboardCloseUp(theme, theme.rightSide()) ?: closeAndLong(theme, theme.rightSide())
        }

    fun closeAndLongBothEnds(theme: FieldTheme): BufferedImage {
        val sides = listOf(theme.leftSide(), theme.rightSide())
        val distances = listOf(CLOSE_BALL_LOCATION, LONG_BALL_LOCATION)
        val viewWidth = (GoalPostScenePainter.SCENE_WIDTH * OUTPUT_SCALE).toInt()
        val viewHeight = (VIEW_HEIGHT * OUTPUT_SCALE).toInt()
        val sheet =
            BufferedImage(
                viewWidth * sides.size + GUTTER * (sides.size - 1),
                viewHeight * distances.size + GUTTER * (distances.size - 1),
                BufferedImage.TYPE_INT_RGB,
            )
        val graphics = sheet.createGraphics()
        graphics.color = GUTTER_COLOR
        graphics.fillRect(0, 0, sheet.width, sheet.height)
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        distances.forEachIndexed { row, ballLocation ->
            sides.forEachIndexed { column, side ->
                val scene = sceneAt(theme, side, ballLocation).getSubimage(0, 0, GoalPostScenePainter.SCENE_WIDTH, VIEW_HEIGHT)
                val x = column * (viewWidth + GUTTER)
                val y = row * (viewHeight + GUTTER)
                graphics.drawImage(scene, x, y, viewWidth, viewHeight, null)
            }
        }
        paintDividers(graphics, sheet, viewWidth, viewHeight)
        graphics.dispose()
        return sheet
    }

    private fun closeAndLong(
        theme: FieldTheme,
        side: TeamSide,
    ): BufferedImage {
        val distances = listOf(CLOSE_BALL_LOCATION, LONG_BALL_LOCATION)
        val viewWidth = GoalPostScenePainter.SCENE_WIDTH
        val sheet = BufferedImage(viewWidth, VIEW_HEIGHT * distances.size + GUTTER, BufferedImage.TYPE_INT_RGB)
        val graphics = sheet.createGraphics()
        graphics.color = GUTTER_COLOR
        graphics.fillRect(0, 0, sheet.width, sheet.height)
        distances.forEachIndexed { row, ballLocation ->
            val scene = sceneAt(theme, side, ballLocation).getSubimage(0, 0, viewWidth, VIEW_HEIGHT)
            graphics.drawImage(scene, 0, row * (VIEW_HEIGHT + GUTTER), null)
        }
        graphics.color = DIVIDER_COLOR
        graphics.fillRect(0, VIEW_HEIGHT + (GUTTER - DIVIDER_WIDTH) / 2, sheet.width, DIVIDER_WIDTH)
        graphics.dispose()
        return sheet
    }

    private fun scoreboardCloseUp(
        theme: FieldTheme,
        side: TeamSide,
    ): BufferedImage? {
        val board =
            GoalPostScenePainter.paintScoreboardAlone(theme, layoutAt(CLOSE_BALL_LOCATION), side, situationOf(theme), SCOREBOARD_PADDING)
                ?: return null
        val height = (board.height * SCOREBOARD_OUTPUT_WIDTH.toFloat() / board.width).toInt()
        val output = BufferedImage(SCOREBOARD_OUTPUT_WIDTH, height, BufferedImage.TYPE_INT_RGB)
        val graphics = output.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.drawImage(board, 0, 0, output.width, output.height, null)
        graphics.dispose()
        return output
    }

    private fun paintDividers(
        graphics: Graphics2D,
        sheet: BufferedImage,
        viewWidth: Int,
        viewHeight: Int,
    ) {
        val inset = (GUTTER - DIVIDER_WIDTH) / 2
        graphics.color = DIVIDER_COLOR
        graphics.fillRect(viewWidth + inset, 0, DIVIDER_WIDTH, sheet.height)
        graphics.fillRect(0, viewHeight + inset, sheet.width, DIVIDER_WIDTH)
    }

    private fun sceneAt(
        theme: FieldTheme,
        side: TeamSide,
        ballLocation: Int,
    ): BufferedImage =
        GoalPostScenePainter.paint(
            theme,
            theme.endZoneOf(side),
            layoutAt(ballLocation),
            midfieldTopOnLeft = true,
            side = side,
            situation = situationOf(theme),
        )

    private fun layoutAt(ballLocation: Int) =
        GoalPostScenePainter.layoutFor(
            Play().apply {
                playCall = PlayCall.FIELD_GOAL
                this.ballLocation = ballLocation
                possession = TeamSide.HOME
            },
        )

    private fun situationOf(theme: FieldTheme) =
        GameSituation.sample(GameSituation.scoreboardName(theme.homeTeam), GameSituation.scoreboardName(theme.awayTeam))
}
