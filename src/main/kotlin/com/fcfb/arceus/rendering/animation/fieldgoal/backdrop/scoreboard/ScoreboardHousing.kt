package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import com.fcfb.arceus.enums.animation.ScoreboardShape
import com.fcfb.arceus.rendering.animation.assets.LogoFit
import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.Rectangle
import java.awt.Shape
import java.awt.geom.Arc2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

object ScoreboardHousing {
    private const val CORNER_SHARE = 0.28f
    private const val CHAMFER_SHARE = 0.18f
    private const val ARCH_WIDTH_SHARE = 0.5f
    private const val ARCH_HEIGHT_SHARE = 0.34f
    private const val MARQUEE_WIDTH_SHARE = 0.46f
    private const val MARQUEE_HEIGHT_SHARE = 0.26f
    private const val TRIM_SHARE = 0.012f
    private const val TITLE_HEIGHT_SHARE = 0.55f
    private const val TITLE_WIDTH_SHARE = 0.8f
    private const val LOGO_SHARE = 0.8f
    private const val ARCH_TITLE_NARROWING = 0.75f

    fun topperHeight(
        shape: ScoreboardShape,
        board: Rectangle,
    ): Int =
        when (shape) {
            ScoreboardShape.ARCHED -> (board.height * ARCH_HEIGHT_SHARE).toInt()
            ScoreboardShape.MARQUEE -> (board.height * MARQUEE_HEIGHT_SHARE).toInt()
            else -> 0
        }

    fun outlineOf(
        shape: ScoreboardShape,
        board: Rectangle,
    ): Shape? =
        when (shape) {
            ScoreboardShape.ROUNDED -> {
                val corner = (board.height * CORNER_SHARE).toDouble()
                RoundRectangle2D.Float(
                    board.x.toFloat(),
                    board.y.toFloat(),
                    board.width.toFloat(),
                    board.height.toFloat(),
                    corner.toFloat(),
                    corner.toFloat(),
                )
            }
            ScoreboardShape.CHAMFERED -> chamfered(board)
            else -> null
        }

    fun paintTopper(
        g: Graphics2D,
        shape: ScoreboardShape,
        board: Rectangle,
        design: ScoreboardDesign,
        title: String,
        logo: BufferedImage?,
    ) {
        val height = topperHeight(shape, board)
        if (height <= 0) return
        val topper =
            when (shape) {
                ScoreboardShape.ARCHED -> {
                    val width = (board.width * ARCH_WIDTH_SHARE).toInt()
                    Arc2D.Float(
                        (board.centerX - width / 2).toFloat(),
                        (board.y - height).toFloat(),
                        width.toFloat(),
                        height * 2f,
                        0f,
                        180f,
                        Arc2D.CHORD,
                    )
                }
                ScoreboardShape.MARQUEE -> {
                    val width = (board.width * MARQUEE_WIDTH_SHARE).toInt()
                    Rectangle(board.centerX.toInt() - width / 2, board.y - height, width, height)
                }
                else -> return
            }
        g.color = design.face
        g.fill(topper)
        paintTrim(g, topper, board, design)
        if (logo != null) {
            val logoBox = (height * LOGO_SHARE).toInt()
            val centerY = if (shape == ScoreboardShape.ARCHED) board.y - height / 2 + height / 8 else board.y - height / 2
            LogoFit().draw(g, logo, board.centerX.toInt(), centerY, logoBox)
            return
        }
        paintTitle(g, shape, board, topper.bounds, height, design, title)
    }

    private fun paintTitle(
        g: Graphics2D,
        shape: ScoreboardShape,
        board: Rectangle,
        bounds: Rectangle,
        height: Int,
        design: ScoreboardDesign,
        title: String,
    ) {
        val titleHeight = (height * TITLE_HEIGHT_SHARE).toInt()
        val titleTop =
            if (shape == ScoreboardShape.ARCHED) board.y - titleHeight - height / 8 else board.y - height + (height - titleHeight) / 2
        val archNarrowing = if (shape == ScoreboardShape.ARCHED) ARCH_TITLE_NARROWING else 1f
        val font = ScoreboardText.sharedFont(g, listOf(title to (bounds.width * TITLE_WIDTH_SHARE * archNarrowing).toInt()), titleHeight)
        ScoreboardText.paintLabel(g, title, board.centerX.toInt(), titleTop, titleHeight, design.text, font)
    }

    fun paintTrim(
        g: Graphics2D,
        outline: Shape,
        board: Rectangle,
        design: ScoreboardDesign,
    ) {
        val previous = g.stroke
        g.stroke = BasicStroke((board.width * TRIM_SHARE).coerceAtLeast(1f))
        g.color = design.trim
        g.draw(outline)
        g.stroke = previous
    }

    private fun chamfered(board: Rectangle): Polygon {
        val cut = (board.height * CHAMFER_SHARE).toInt()
        val right = board.x + board.width
        val bottom = board.y + board.height
        return Polygon(
            intArrayOf(board.x + cut, right - cut, right, right, right - cut, board.x + cut, board.x, board.x),
            intArrayOf(board.y, board.y, board.y + cut, bottom - cut, bottom, bottom, bottom - cut, board.y + cut),
            8,
        )
    }
}
