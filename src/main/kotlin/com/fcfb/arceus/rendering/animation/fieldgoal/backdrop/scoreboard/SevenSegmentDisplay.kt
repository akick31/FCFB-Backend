package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Polygon

object SevenSegmentDisplay {
    private const val DIGIT_WIDTH_SHARE = 0.56f
    private const val DIGIT_GAP_SHARE = 0.2f
    private const val COLON_WIDTH_SHARE = 0.26f
    private const val THICKNESS_SHARE = 0.14f

    private val segmentsByDigit: Map<Char, String> =
        mapOf(
            '0' to "abcdef",
            '1' to "bc",
            '2' to "abged",
            '3' to "abgcd",
            '4' to "fgbc",
            '5' to "afgcd",
            '6' to "afgedc",
            '7' to "abc",
            '8' to "abcdefg",
            '9' to "abcdfg",
            ' ' to "",
        )

    fun heightToFit(
        text: String,
        width: Float,
        height: Float,
    ): Float {
        val digits = text.count { it != ':' }
        val colons = text.count { it == ':' }
        val widthPerHeight = digits * DIGIT_WIDTH_SHARE + (text.length - 1) * DIGIT_GAP_SHARE + colons * COLON_WIDTH_SHARE
        return minOf(height, width / widthPerHeight)
    }

    fun paint(
        g: Graphics2D,
        text: String,
        centerX: Int,
        centerY: Int,
        digitHeight: Float,
        lit: Color,
        unlit: Color,
    ) {
        val digitWidth = digitHeight * DIGIT_WIDTH_SHARE
        val gap = digitHeight * DIGIT_GAP_SHARE
        val colonWidth = digitHeight * COLON_WIDTH_SHARE
        val totalWidth = text.sumOf { (if (it == ':') colonWidth else digitWidth).toDouble() } + gap * (text.length - 1)
        var left = centerX - totalWidth.toFloat() / 2f
        val top = centerY - digitHeight / 2f
        text.forEach { character ->
            if (character == ':') {
                paintColon(g, left, top, colonWidth, digitHeight, lit)
                left += colonWidth + gap
            } else {
                paintDigit(g, segmentsByDigit[character] ?: "", left, top, digitWidth, digitHeight, lit, unlit)
                left += digitWidth + gap
            }
        }
    }

    private fun paintDigit(
        g: Graphics2D,
        litSegments: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        lit: Color,
        unlit: Color,
    ) {
        val t = (height * THICKNESS_SHARE).coerceAtLeast(1f)
        val half = height / 2f
        "abcdefg".forEach { segment ->
            val polygon =
                when (segment) {
                    'a' -> horizontal(left, top, width, t)
                    'g' -> horizontal(left, top + half - t / 2f, width, t)
                    'd' -> horizontal(left, top + height - t, width, t)
                    'f' -> vertical(left, top, half, t)
                    'b' -> vertical(left + width - t, top, half, t)
                    'e' -> vertical(left, top + half, half, t)
                    else -> vertical(left + width - t, top + half, half, t)
                }
            g.color = if (segment in litSegments) lit else unlit
            g.fillPolygon(polygon)
        }
    }

    private fun horizontal(
        left: Float,
        top: Float,
        width: Float,
        t: Float,
    ): Polygon {
        val inset = t / 2f
        return polygon(
            left + inset to top + t / 2f,
            left + t to top,
            left + width - t to top,
            left + width - inset to top + t / 2f,
            left + width - t to top + t,
            left + t to top + t,
        )
    }

    private fun vertical(
        left: Float,
        top: Float,
        length: Float,
        t: Float,
    ): Polygon =
        polygon(
            left + t / 2f to top + t / 2f,
            left + t to top + t,
            left + t to top + length - t,
            left + t / 2f to top + length - t / 2f,
            left to top + length - t,
            left to top + t,
        )

    private fun paintColon(
        g: Graphics2D,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        lit: Color,
    ) {
        val dot = (height * THICKNESS_SHARE).toInt().coerceAtLeast(1)
        val x = (left + width / 2f - dot / 2f).toInt()
        g.color = lit
        g.fillRect(x, (top + height * 0.3f).toInt(), dot, dot)
        g.fillRect(x, (top + height * 0.65f).toInt(), dot, dot)
    }

    private fun polygon(vararg points: Pair<Float, Float>): Polygon =
        Polygon(points.map { it.first.toInt() }.toIntArray(), points.map { it.second.toInt() }.toIntArray(), points.size)
}
