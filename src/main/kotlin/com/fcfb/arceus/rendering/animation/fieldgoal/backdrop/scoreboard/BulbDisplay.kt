package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import java.awt.Color
import java.awt.Graphics2D

object BulbDisplay {
    private const val DIGIT_COLUMNS = 5
    private const val ROWS = 7
    private const val COMPACT_COLUMNS = 3
    private const val COMPACT_ROWS = 5
    private const val COLON_COLUMNS = 1
    private const val CELL_GAP = 1
    private const val BULB_SHARE = 0.9f

    private val litColor = Color(255, 188, 40)
    private val unlitColor = Color(24, 22, 21)

    val PALETTE: List<Color> = listOf(litColor, unlitColor)

    private val digitGlyphs: Map<Char, List<String>> =
        mapOf(
            '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
            '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
            '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
            '3' to listOf("11111", "00010", "00100", "00010", "00001", "10001", "01110"),
            '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
            '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
            '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
            '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
            '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
            '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
            ' ' to List(ROWS) { "00000" },
        )
    private val colonGlyph = listOf("0", "0", "1", "0", "1", "0", "0")

    private val compactGlyphs: Map<Char, List<String>> =
        mapOf(
            '0' to listOf("111", "101", "101", "101", "111"),
            '1' to listOf("010", "110", "010", "010", "111"),
            '2' to listOf("111", "001", "111", "100", "111"),
            '3' to listOf("111", "001", "111", "001", "111"),
            '4' to listOf("101", "101", "111", "001", "001"),
            '5' to listOf("111", "100", "111", "001", "111"),
            '6' to listOf("111", "100", "111", "101", "111"),
            '7' to listOf("111", "001", "010", "010", "010"),
            '8' to listOf("111", "101", "111", "101", "111"),
            '9' to listOf("111", "101", "111", "001", "111"),
            ' ' to List(COMPACT_ROWS) { "000" },
        )

    fun columnsFor(
        text: String,
        compact: Boolean = false,
    ): Int {
        val digitColumns = if (compact) COMPACT_COLUMNS else DIGIT_COLUMNS
        return text.sumOf { (if (it == ':') COLON_COLUMNS else digitColumns) + CELL_GAP } - CELL_GAP
    }

    fun pitchToFit(
        text: String,
        width: Float,
        height: Float,
        compact: Boolean = false,
    ): Float = minOf(width / columnsFor(text, compact), height / if (compact) COMPACT_ROWS else ROWS)

    fun paint(
        g: Graphics2D,
        text: String,
        centerX: Int,
        centerY: Int,
        pitch: Float,
        compact: Boolean = false,
    ) {
        val bulb = (pitch * BULB_SHARE).coerceAtLeast(1f)
        var column = 0f
        val glyphs = if (compact) compactGlyphs else digitGlyphs
        val left = centerX - columnsFor(text, compact) * pitch / 2f
        val top = centerY - (if (compact) COMPACT_ROWS else ROWS) * pitch / 2f
        text.forEach { character ->
            val glyph = if (character == ':') colonGlyph else glyphs[character] ?: glyphs.getValue(' ')
            glyph.forEachIndexed { row, bits ->
                bits.forEachIndexed { bitColumn, bit ->
                    val x = left + (column + bitColumn) * pitch + (pitch - bulb) / 2f
                    val y = top + row * pitch + (pitch - bulb) / 2f
                    val lit = bit == '1'
                    if (lit || character != ':') paintBulb(g, x, y, bulb, lit)
                }
            }
            column += glyph.first().length + CELL_GAP
        }
    }

    private fun paintBulb(
        g: Graphics2D,
        x: Float,
        y: Float,
        bulb: Float,
        lit: Boolean,
    ) {
        g.color = if (lit) litColor else unlitColor
        val size = bulb.toInt().coerceAtLeast(1)
        g.fillOval(x.toInt(), y.toInt(), size, size)
    }
}
