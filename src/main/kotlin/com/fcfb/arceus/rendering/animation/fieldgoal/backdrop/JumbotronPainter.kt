package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.model.animation.GameSituation
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard.ScoreboardHousing
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle

object JumbotronPainter {
    private const val TOP_MARGIN = 6
    private const val MAX_LIFT = 110
    private const val MIN_POST_SHARE = 0.2f

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        mount: JumbotronMount,
        settings: JumbotronSettings,
        situation: GameSituation,
        homeColor: Color?,
        awayColor: Color?,
    ) {
        val board = fittedBoard(area, mount, settings)
        if (mount.faceBottom != null) {
            board.y = mount.faceBottom - board.height
        } else {
            val margin = area.scaled(TOP_MARGIN) + topper(settings, board)
            val restingTop = board.y
            val lift = if (mount.canLift) (MAX_LIFT * area.scale * settings.elevation).toInt() else 0
            board.y =
                when {
                    mount.fixedScale -> restingTop - lift
                    mount.keepInFrame -> maxOf(restingTop - lift, margin)
                    else -> maxOf(restingTop - lift, minOf(restingTop, margin))
                }
            PylonPainter.paint(g, board, mount.footY, settings.pylonStyle, outside = mount.spanWidth != null || settings.pylonsOutside)
        }
        StadiumScoreboardPainter.paint(g, board, area, settings, situation, homeColor, awayColor)
    }

    private fun topper(
        settings: JumbotronSettings,
        board: Rectangle,
    ): Int = ScoreboardHousing.topperHeight(settings.shape, board)

    private fun fittedBoard(
        area: BackdropArea,
        mount: JumbotronMount,
        settings: JumbotronSettings,
    ): Rectangle {
        val bottom = mount.groundY - mount.restingPost
        val board = StadiumScoreboardPainter.boardBounds(area, bottom, settings.size, settings.width, mount.spanWidth, mount.fixedScale)
        if (!mount.keepInFrame || mount.faceBottom != null) return board
        val available = mount.groundY - area.scaled(TOP_MARGIN)
        val needed = board.height + topper(settings, board) + mount.restingPost
        if (needed <= available || available <= 0) return board
        val post = maxOf(mount.restingPost, (available * MIN_POST_SHARE).toInt())
        val fit = (available - post).toFloat() / (needed - mount.restingPost)
        return StadiumScoreboardPainter.boardBounds(area, mount.groundY - post, settings.size * fit, settings.width, mount.spanWidth, false)
    }
}
