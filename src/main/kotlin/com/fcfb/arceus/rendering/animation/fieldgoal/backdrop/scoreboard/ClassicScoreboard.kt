package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import com.fcfb.arceus.model.animation.GameSituation
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Rectangle

object ClassicScoreboard : ScoreboardDesign {
    private const val TIME_FONT_SHARE = 0.68f
    private const val TRIM_SHARE = 0.012f
    private const val NAME_ROW_TOP = 0.07f
    private const val NAME_ROW_HEIGHT = 0.16f
    private const val DIGIT_ROW_TOP = 0.27f
    private const val DIGIT_ROW_HEIGHT = 0.44f
    private const val FOOTER_ROW_TOP = 0.76f
    private const val FOOTER_ROW_HEIGHT = 0.18f
    private const val PANEL_PADDING_SHARE = 0.12f
    private const val FOOTER_LABEL_SHARE = 0.55f
    private const val LIGHT_PLATE_THRESHOLD = 150

    private val cabinetColor = Color(20, 22, 24)
    private val trimColor = Color(186, 188, 192)
    private val labelColor = Color(240, 240, 234)
    private val fallbackPlateColor = Color(70, 74, 80)
    private val panelColor = Color(8, 8, 9)

    override val face: Color get() = cabinetColor
    override val trim: Color get() = trimColor
    override val text: Color get() = labelColor

    private val extras = ScoreboardExtras.Colors(labelColor, BulbDisplay.PALETTE.first(), BulbDisplay.PALETTE.last(), panelColor)

    override val palette: List<Color> =
        listOf(cabinetColor, trimColor, labelColor, fallbackPlateColor, panelColor) + BulbDisplay.PALETTE

    override fun paint(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
        homeColor: Color?,
        awayColor: Color?,
    ) {
        paintCabinet(g, layout.bounds)
        paintNameRow(g, layout, situation, homeColor, awayColor)
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

    private fun paintCabinet(
        g: Graphics2D,
        bounds: Rectangle,
    ) {
        g.color = cabinetColor
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        val trim = (bounds.width * TRIM_SHARE).toInt().coerceAtLeast(1)
        val previousStroke = g.stroke
        g.stroke = BasicStroke(trim.toFloat())
        g.color = trimColor
        g.drawRect(bounds.x + trim / 2, bounds.y + trim / 2, bounds.width - trim, bounds.height - trim)
        g.stroke = previousStroke
    }

    private fun paintNameRow(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
        homeColor: Color?,
        awayColor: Color?,
    ) {
        val top = layout.rowTop(NAME_ROW_TOP)
        val height = layout.rowHeight(NAME_ROW_HEIGHT)
        val homeName = situation.homeTeamName.uppercase()
        val awayName = situation.awayTeamName.uppercase()
        val font = ScoreboardText.sharedFont(g, listOf(homeName to layout.sideWidth, awayName to layout.sideWidth), height)
        paintPlate(g, homeName, homeColor, layout.homeSide.first, top, layout.sideWidth, height, font)
        paintPlate(g, awayName, awayColor, layout.awaySide.first, top, layout.sideWidth, height, font)
        val timeFont = font.deriveFont(font.size2D * TIME_FONT_SHARE)
        ScoreboardText.paintLabel(g, "TIME", layout.centerX, top, height, labelColor, timeFont)
    }

    private fun paintDigitRow(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
    ) {
        val top = layout.rowTop(DIGIT_ROW_TOP)
        val height = layout.rowHeight(DIGIT_ROW_HEIGHT)
        val scorePitch =
            paintPanel(g, ScoreboardText.twoDigits(situation.homeScore), layout.homeScoreCenterX, top, layout.scoreWidth, height, null)
        paintPanel(g, ScoreboardText.twoDigits(situation.awayScore), layout.awayScoreCenterX, top, layout.scoreWidth, height, scorePitch)
        paintPanel(g, ScoreboardText.clockText(situation.clockSeconds), layout.centerX, top, layout.centerWidth, height, scorePitch)
    }

    private fun paintPanel(
        g: Graphics2D,
        text: String,
        centerX: Int,
        top: Int,
        width: Int,
        height: Int,
        maxPitch: Float?,
        compact: Boolean = false,
    ): Float {
        g.color = panelColor
        g.fillRect(centerX - width / 2, top, width, height)
        val padding = height * PANEL_PADDING_SHARE
        val fitted = BulbDisplay.pitchToFit(text, width - padding * 2, height - padding * 2, compact)
        val pitch = maxPitch?.let { minOf(it, fitted) } ?: fitted
        BulbDisplay.paint(g, text, centerX, top + height / 2, pitch, compact)
        return pitch
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
        val padding = 1f - PANEL_PADDING_SHARE * 2
        val pitch =
            groups.minOf { (_, _, slot) ->
                BulbDisplay.pitchToFit("00", slot.second * (1f - FOOTER_LABEL_SHARE) * padding, height * padding, compact = true)
            }
        groups.forEach { (label, value, slot) ->
            val (left, width) = slot
            val labelWidth = (width * FOOTER_LABEL_SHARE).toInt()
            ScoreboardText.paintLabel(g, label, left + labelWidth / 2, top, height, labelColor, font)
            paintPanel(g, value, left + labelWidth + (width - labelWidth) / 2, top, width - labelWidth, height, pitch, compact = true)
        }
    }

    private fun paintPlate(
        g: Graphics2D,
        text: String,
        plateColor: Color?,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        font: Font,
    ) {
        val fill = plateColor ?: fallbackPlateColor
        g.color = fill
        g.fillRect(left, top, width, height)
        val lightness = (fill.red + fill.green + fill.blue) / 3
        val textColor = if (lightness > LIGHT_PLATE_THRESHOLD) cabinetColor else labelColor
        ScoreboardText.paintLabel(g, text, left + width / 2, top, height, textColor, font)
    }
}
