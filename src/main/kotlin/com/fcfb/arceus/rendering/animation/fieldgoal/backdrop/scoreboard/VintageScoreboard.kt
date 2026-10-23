package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import com.fcfb.arceus.model.animation.GameSituation
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

object VintageScoreboard : ScoreboardDesign {
    private const val OUTER_TRIM_SHARE = 0.035f
    private const val INNER_TRIM_SHARE = 0.075f
    private const val NAME_ROW_TOP = 0.12f
    private const val NAME_ROW_HEIGHT = 0.15f
    private const val CLOCK_PADDING_SHARE = 0.12f
    private const val DIGIT_ROW_TOP = 0.29f
    private const val DIGIT_ROW_HEIGHT = 0.4f
    private const val FOOTER_ROW_TOP = 0.75f
    private const val FOOTER_ROW_HEIGHT = 0.17f
    private const val FOOTER_LABEL_SHARE = 0.55f
    private const val TILE_WIDTH_SHARE = 0.68f
    private const val TILE_GAP_SHARE = 0.12f
    private const val TIME_FONT_SHARE = 0.72f

    private val boardColor = Color(30, 76, 50)
    private val slotColor = Color(18, 50, 34)
    private val creamColor = Color(234, 224, 192)

    override val face: Color get() = boardColor
    override val trim: Color get() = creamColor
    override val text: Color get() = creamColor

    private val extras = ScoreboardExtras.Colors(creamColor, creamColor, boardColor, slotColor)

    override val palette: List<Color> = listOf(boardColor, slotColor, creamColor) + BulbDisplay.PALETTE

    override fun paint(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
        homeColor: Color?,
        awayColor: Color?,
    ) {
        val bounds = layout.bounds
        g.color = creamColor
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        val outer = (bounds.height * OUTER_TRIM_SHARE).toInt().coerceAtLeast(1)
        g.color = boardColor
        g.fillRect(bounds.x + outer, bounds.y + outer, bounds.width - outer * 2, bounds.height - outer * 2)
        val inner = (bounds.height * INNER_TRIM_SHARE).toInt().coerceAtLeast(2)
        g.color = creamColor
        g.drawRect(bounds.x + inner, bounds.y + inner, bounds.width - inner * 2, bounds.height - inner * 2)
        paintNameRow(g, layout, situation)
        paintDigitRow(g, layout, situation)
        paintFooterRow(g, layout, situation)
        ScoreboardExtras.paintBothSides(
            g,
            layout,
            situation,
            ScoreboardExtras.Row(layout.rowTop(DIGIT_ROW_TOP), layout.rowHeight(DIGIT_ROW_HEIGHT)),
            ScoreboardExtras.Row(layout.rowTop(FOOTER_ROW_TOP), layout.rowHeight(FOOTER_ROW_HEIGHT)),
            extras,
            Font.SERIF,
        )
    }

    private fun paintNameRow(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
    ) {
        val top = layout.rowTop(NAME_ROW_TOP)
        val height = layout.rowHeight(NAME_ROW_HEIGHT)
        val homeName = situation.homeTeamName.uppercase()
        val awayName = situation.awayTeamName.uppercase()
        val font = ScoreboardText.sharedFont(g, listOf(homeName to layout.sideWidth, awayName to layout.sideWidth), height, Font.SERIF)
        ScoreboardText.paintLabel(g, homeName, layout.homeCenterX, top, height, creamColor, font)
        ScoreboardText.paintLabel(g, awayName, layout.awayCenterX, top, height, creamColor, font)
        ScoreboardText.paintLabel(g, "TIME", layout.centerX, top, height, creamColor, font.deriveFont(font.size2D * TIME_FONT_SHARE))
    }

    private fun paintDigitRow(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
    ) {
        val top = layout.rowTop(DIGIT_ROW_TOP)
        val height = layout.rowHeight(DIGIT_ROW_HEIGHT)
        val scoreTile = tileWidth(2, layout.scoreWidth, height)
        paintTiles(g, ScoreboardText.twoDigits(situation.homeScore), layout.homeScoreCenterX, top, height, scoreTile)
        paintTiles(g, ScoreboardText.twoDigits(situation.awayScore), layout.awayScoreCenterX, top, height, scoreTile)
        paintBulbClock(g, layout, ScoreboardText.clockText(situation.clockSeconds), top, height)
    }

    private fun paintBulbClock(
        g: Graphics2D,
        layout: ScoreboardLayout,
        clock: String,
        top: Int,
        height: Int,
    ) {
        g.color = slotColor
        g.fillRect(layout.centerX - layout.centerWidth / 2, top, layout.centerWidth, height)
        val padding = height * CLOCK_PADDING_SHARE
        val pitch = BulbDisplay.pitchToFit(clock, layout.centerWidth - padding * 2, height - padding * 2)
        BulbDisplay.paint(g, clock, layout.centerX, top + height / 2, pitch)
    }

    private fun paintFooterRow(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
    ) {
        val top = layout.rowTop(FOOTER_ROW_TOP)
        val height = layout.rowHeight(FOOTER_ROW_HEIGHT)
        val groups =
            listOf(
                Triple("QTR", situation.quarter.toString().takeLast(1), layout.homeScoreLeft to layout.scoreWidth),
                Triple("DOWN", situation.down.toString().takeLast(1), layout.centerX - layout.centerWidth / 2 to layout.centerWidth),
                Triple("TO GO", ScoreboardText.twoDigits(situation.yardsToGo).trim(), layout.awayScoreLeft to layout.scoreWidth),
            )
        val font =
            ScoreboardText.sharedFont(
                g,
                groups.map { (label, _, slot) -> label to (slot.second * FOOTER_LABEL_SHARE).toInt() },
                height,
                Font.SERIF,
            )
        val tile = groups.minOf { (_, _, slot) -> tileWidth(2, (slot.second * (1f - FOOTER_LABEL_SHARE)).toInt(), height) }
        groups.forEach { (label, value, slot) ->
            val (left, width) = slot
            val labelWidth = (width * FOOTER_LABEL_SHARE).toInt()
            ScoreboardText.paintLabel(g, label, left + labelWidth / 2, top, height, creamColor, font)
            paintTiles(g, value, left + labelWidth + (width - labelWidth) / 2, top, height, tile)
        }
    }

    private fun tileWidth(
        count: Int,
        width: Int,
        height: Int,
    ): Int {
        val byHeight = (height * TILE_WIDTH_SHARE).toInt()
        val byWidth = (width / (count + (count - 1) * TILE_GAP_SHARE)).toInt()
        return minOf(byHeight, byWidth).coerceAtLeast(2)
    }

    private fun paintTiles(
        g: Graphics2D,
        text: String,
        centerX: Int,
        top: Int,
        height: Int,
        tileWidth: Int,
    ) {
        val gap = (tileWidth * TILE_GAP_SHARE).toInt().coerceAtLeast(1)
        val tileHeight = (tileWidth / TILE_WIDTH_SHARE).toInt().coerceAtMost(height)
        val tileTop = top + (height - tileHeight) / 2
        val totalWidth = text.length * tileWidth + (text.length - 1) * gap
        val font = ScoreboardText.sharedFont(g, listOf("8" to tileWidth), tileHeight, Font.SERIF)
        text.forEachIndexed { index, character ->
            val left = centerX - totalWidth / 2 + index * (tileWidth + gap)
            g.color = slotColor
            g.fillRect(left, tileTop, tileWidth, tileHeight)
            if (character != ' ') {
                ScoreboardText.paintLabel(g, character.toString(), left + tileWidth / 2, tileTop, tileHeight, creamColor, font)
            }
        }
    }
}
