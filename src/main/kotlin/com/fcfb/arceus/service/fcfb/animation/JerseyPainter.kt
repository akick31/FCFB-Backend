package com.fcfb.arceus.service.fcfb.animation

import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.geom.AffineTransform

object JerseyPainter {
    private const val NUMBER_SIZE = 18f

    fun drawNumber(
        g: Graphics2D,
        figure: FieldGoalFigure,
        shoulderY: Float,
    ) {
        val scale = figure.scale
        val font = NumberFonts.font(figure.uniform.jerseyNumberFont, NUMBER_SIZE * scale)
        val number = font.createGlyphVector(g.fontRenderContext, figure.number.toString()).outline
        val bounds = number.bounds2D
        val widthScale = minOf(1.0, 26 * scale / bounds.width)
        val transform = AffineTransform()
        transform.translate(figure.x.toDouble(), (shoulderY + 22 * scale).toDouble())
        transform.scale(widthScale, 1.0)
        transform.translate(-bounds.centerX, -bounds.maxY)
        val shape = transform.createTransformedShape(number)
        figure.uniform.numberOutline?.let { outline ->
            g.color = outline
            g.stroke = BasicStroke(scale * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.draw(shape)
        }
        g.color = figure.uniform.number
        g.fill(shape)
        drawChestText(g, figure, shoulderY)
    }

    private fun drawChestText(
        g: Graphics2D,
        figure: FieldGoalFigure,
        shoulderY: Float,
    ) {
        val text = figure.uniform.numberTopText?.takeIf { it.isNotBlank() } ?: return
        val scale = figure.scale
        val font = NumberFonts.font(figure.uniform.jerseyNumberFont, 4.5f * scale)
        val shape = font.createGlyphVector(g.fontRenderContext, text).outline
        val bounds = shape.bounds2D
        val availableWidth = 24f * scale
        val transform = AffineTransform()
        transform.translate(figure.x.toDouble(), (shoulderY + 7 * scale).toDouble())
        transform.scale(minOf(1.0, availableWidth / bounds.width), 1.0)
        transform.translate(-bounds.centerX, -bounds.maxY)
        val fitted = transform.createTransformedShape(shape)
        figure.uniform.numberOutline?.let { outline ->
            g.color = outline
            g.stroke = BasicStroke(scale * 0.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.draw(fitted)
        }
        g.color = figure.uniform.number
        g.fill(fitted)
    }
}
