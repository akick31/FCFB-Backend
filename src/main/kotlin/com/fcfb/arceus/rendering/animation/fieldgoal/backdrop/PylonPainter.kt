package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.PylonStyle
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle

object PylonPainter {
    private const val TWIN_WIDTH_SHARE = 0.025f
    private const val INSET_SHARE = 0.2f
    private const val TRUSS_WIDTH_SHARE = 0.06f
    private const val BRICK_WIDTH_SHARE = 0.07f
    private const val TRUSS_SEGMENT = 1.2f
    private const val BRICK_COURSE = 4

    private val steelPost = Color(72, 74, 80)
    private val truss = Color(96, 100, 108)
    private val brick = Color(150, 74, 56)
    private val mortar = Color(122, 60, 46)
    private val stoneCap = Color(204, 194, 174)
    private val concrete = Color(168, 166, 160)
    private val concreteShadow = Color(138, 136, 130)

    val PALETTE: List<Color> = listOf(steelPost, truss, brick, mortar, stoneCap, concrete, concreteShadow)

    fun paint(
        g: Graphics2D,
        board: Rectangle,
        groundY: Int,
        style: PylonStyle,
        outside: Boolean,
    ) {
        val top = board.y + board.height
        val height = groundY - top
        if (height <= 0) return
        val inset = if (outside) 0 else (board.width * INSET_SHARE).toInt()
        when (style) {
            PylonStyle.STEEL -> paintTwin(g, board, top, height, inset)
            PylonStyle.CONCRETE ->
                pairs(
                    board,
                    inset,
                    (board.width * BRICK_WIDTH_SHARE).toInt(),
                ).forEach { paintConcrete(g, it, top, height) }
            PylonStyle.TRUSS -> pairs(board, inset, (board.width * TRUSS_WIDTH_SHARE).toInt()).forEach { paintTruss(g, it, top, height) }
            PylonStyle.BRICK -> pairs(board, inset, (board.width * BRICK_WIDTH_SHARE).toInt()).forEach { paintBrick(g, it, top, height) }
        }
    }

    private fun pairs(
        board: Rectangle,
        inset: Int,
        width: Int,
    ): List<IntRange> =
        listOf(
            board.x + inset until board.x + inset + width,
            board.x + board.width - inset - width until board.x + board.width - inset,
        )

    private fun paintTwin(
        g: Graphics2D,
        board: Rectangle,
        top: Int,
        height: Int,
        inset: Int,
    ) {
        val width = (board.width * TWIN_WIDTH_SHARE).toInt().coerceAtLeast(2)
        g.color = steelPost
        pairs(board, inset, width).forEach { g.fillRect(it.first, top, width, height) }
        if (inset > 0) g.fillRect(board.x + inset, top + height / 3, board.width - inset * 2, (width / 2).coerceAtLeast(1))
    }

    private fun paintTruss(
        g: Graphics2D,
        span: IntRange,
        top: Int,
        height: Int,
    ) {
        val width = (span.last - span.first).coerceAtLeast(3)
        val segment = (width * TRUSS_SEGMENT).toInt().coerceAtLeast(3)
        g.color = truss
        g.fillRect(span.first, top, 1, height)
        g.fillRect(span.last, top, 1, height)
        var y = top
        while (y + segment <= top + height) {
            g.drawLine(span.first, y, span.last, y + segment)
            g.drawLine(span.last, y, span.first, y + segment)
            g.drawLine(span.first, y, span.last, y)
            y += segment
        }
    }

    private fun paintBrick(
        g: Graphics2D,
        span: IntRange,
        top: Int,
        height: Int,
    ) {
        val width = (span.last - span.first).coerceAtLeast(3)
        g.color = brick
        g.fillRect(span.first, top, width, height)
        g.color = mortar
        var y = top + BRICK_COURSE
        while (y < top + height) {
            g.fillRect(span.first, y, width, 1)
            y += BRICK_COURSE
        }
        g.color = stoneCap
        g.fillRect(span.first - 1, top, width + 2, (width / 4).coerceAtLeast(2))
    }

    private fun paintConcrete(
        g: Graphics2D,
        span: IntRange,
        top: Int,
        height: Int,
    ) {
        val width = (span.last - span.first).coerceAtLeast(3)
        g.color = concrete
        g.fillRect(span.first, top, width, height)
        g.color = concreteShadow
        g.fillRect(span.last - width / 4, top, width / 4, height)
    }
}
