package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import com.fcfb.arceus.model.animation.GameSituation
import java.awt.Color
import java.awt.Graphics2D

object SegmentScoreboard : ScoreboardDesign {
    private const val CROWN_HEIGHT = 0.09f
    private const val BORDER_SHARE = 0.012f
    private const val NAME_ROW_TOP = 0.12f
    private const val NAME_ROW_HEIGHT = 0.14f
    private const val DIGIT_ROW_TOP = 0.3f
    private const val DIGIT_ROW_HEIGHT = 0.38f
    private const val FOOTER_ROW_TOP = 0.75f
    private const val FOOTER_ROW_HEIGHT = 0.18f
    private const val FOOTER_LABEL_SHARE = 0.55f
    private const val DIGIT_PADDING_SHARE = 0.08f
    private const val TIME_FONT_SHARE = 0.75f

    private val faceColor = Color(14, 14, 16)
    private val borderColor = Color(92, 94, 98)
    private val labelColor = Color(245, 245, 240)
    private val redLit = Color(255, 48, 32)
    private val redUnlit = Color(22, 13, 13)
    private val amberLit = Color(255, 170, 24)
    private val amberUnlit = Color(21, 18, 13)
    private val fallbackCrownColor = Color(70, 74, 80)

    override val face: Color get() = faceColor
    override val trim: Color get() = borderColor
    override val text: Color get() = labelColor

    private val extras = ScoreboardExtras.Colors(labelColor, amberLit, amberUnlit)

    override val palette: List<Color> =
        listOf(faceColor, borderColor, labelColor, redLit, redUnlit, amberLit, amberUnlit, fallbackCrownColor)

    override fun paint(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
        homeColor: Color?,
        awayColor: Color?,
    ) {
        val bounds = layout.bounds
        val border = (bounds.width * BORDER_SHARE).toInt().coerceAtLeast(1)
        g.color = borderColor
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        g.color = faceColor
        g.fillRect(bounds.x + border, bounds.y + border, bounds.width - border * 2, bounds.height - border * 2)
        g.color = homeColor ?: fallbackCrownColor
        g.fillRect(bounds.x + border, bounds.y + border, bounds.width - border * 2, layout.rowHeight(CROWN_HEIGHT))
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
        val font = ScoreboardText.sharedFont(g, listOf(homeName to layout.sideWidth, awayName to layout.sideWidth), height)
        ScoreboardText.paintLabel(g, homeName, layout.homeCenterX, top, height, labelColor, font)
        ScoreboardText.paintLabel(g, awayName, layout.awayCenterX, top, height, labelColor, font)
        ScoreboardText.paintLabel(g, "TIME", layout.centerX, top, height, labelColor, font.deriveFont(font.size2D * TIME_FONT_SHARE))
    }

    private fun paintDigitRow(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
    ) {
        val top = layout.rowTop(DIGIT_ROW_TOP)
        val height = layout.rowHeight(DIGIT_ROW_HEIGHT).toFloat()
        val centerY = top + (height / 2f).toInt()
        val padding = 1f - DIGIT_PADDING_SHARE * 2
        val homeScore = ScoreboardText.twoDigits(situation.homeScore)
        val clock = ScoreboardText.clockText(situation.clockSeconds)
        val scoreHeight = SevenSegmentDisplay.heightToFit(homeScore, layout.scoreWidth * padding, height)
        val clockHeight = minOf(scoreHeight, SevenSegmentDisplay.heightToFit(clock, layout.centerWidth * padding, height))
        SevenSegmentDisplay.paint(g, homeScore, layout.homeScoreCenterX, centerY, scoreHeight, redLit, redUnlit)
        val awayScore = ScoreboardText.twoDigits(situation.awayScore)
        SevenSegmentDisplay.paint(g, awayScore, layout.awayScoreCenterX, centerY, scoreHeight, redLit, redUnlit)
        SevenSegmentDisplay.paint(g, clock, layout.centerX, centerY, clockHeight, redLit, redUnlit)
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
                Triple("TO GO", ScoreboardText.twoDigits(situation.yardsToGo), layout.awayScoreLeft to layout.scoreWidth),
            )
        val font =
            ScoreboardText.sharedFont(
                g,
                groups.map { (label, _, slot) -> label to (slot.second * FOOTER_LABEL_SHARE).toInt() },
                height,
            )
        val digitHeight =
            groups.minOf { (_, _, slot) ->
                SevenSegmentDisplay.heightToFit("00", slot.second * (1f - FOOTER_LABEL_SHARE) * 0.85f, height * 0.85f)
            }
        groups.forEach { (label, value, slot) ->
            val (left, width) = slot
            val labelWidth = (width * FOOTER_LABEL_SHARE).toInt()
            ScoreboardText.paintLabel(g, label, left + labelWidth / 2, top, height, labelColor, font)
            val digitCenterX = left + labelWidth + (width - labelWidth) / 2
            SevenSegmentDisplay.paint(g, value, digitCenterX, top + height / 2, digitHeight, amberLit, amberUnlit)
        }
    }
}
