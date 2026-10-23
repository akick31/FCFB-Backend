package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import com.fcfb.arceus.model.animation.GameSituation
import java.awt.Color
import java.awt.Graphics2D

object VideoScoreboard : ScoreboardDesign {
    private const val FRAME_SHARE = 0.018f
    private const val PANEL_TOP = 0.08f
    private const val PANEL_HEIGHT = 0.62f
    private const val NAME_SHARE = 0.3f
    private const val TIME_LABEL_TOP = 0.1f
    private const val TIME_LABEL_HEIGHT = 0.13f
    private const val CLOCK_TOP = 0.26f
    private const val CLOCK_HEIGHT = 0.4f
    private const val FOOTER_TOP = 0.76f
    private const val FOOTER_HEIGHT = 0.18f
    private const val LIGHT_PANEL_THRESHOLD = 150

    private val frameColor = Color(64, 66, 70)
    private val screenColor = Color(6, 6, 8)
    private val footerColor = Color(30, 32, 36)
    private val white = Color(250, 250, 250)
    private val yellow = Color(255, 204, 0)
    private val darkText = Color(16, 16, 18)
    private val fallbackPanelColor = Color(56, 60, 68)

    override val face: Color get() = screenColor
    override val trim: Color get() = frameColor
    override val text: Color get() = white

    private val extras = ScoreboardExtras.Colors(Color(250, 250, 250), Color(255, 204, 0), Color(40, 42, 46))

    override val palette: List<Color> =
        listOf(frameColor, screenColor, footerColor, white, yellow, darkText, fallbackPanelColor, Color(40, 42, 46))

    override fun paint(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
        homeColor: Color?,
        awayColor: Color?,
    ) {
        val bounds = layout.bounds
        val frame = (bounds.width * FRAME_SHARE).toInt().coerceAtLeast(2)
        g.color = frameColor
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        g.color = screenColor
        g.fillRect(bounds.x + frame, bounds.y + frame, bounds.width - frame * 2, bounds.height - frame * 2)
        paintTeamPanel(g, layout, situation.homeTeamName, situation.homeScore, homeColor, layout.homeScoreLeft)
        paintTeamPanel(g, layout, situation.awayTeamName, situation.awayScore, awayColor, layout.awayScoreLeft)
        paintClock(g, layout, situation)
        paintFooter(g, layout, situation, frame)
        ScoreboardExtras.paintBothSides(
            g,
            layout,
            situation,
            ScoreboardExtras.Row(layout.rowTop(PANEL_TOP), layout.rowHeight(PANEL_HEIGHT)),
            ScoreboardExtras.Row(layout.rowTop(FOOTER_TOP), layout.rowHeight(FOOTER_HEIGHT)),
            extras,
        )
    }

    private fun paintTeamPanel(
        g: Graphics2D,
        layout: ScoreboardLayout,
        name: String,
        score: Int,
        color: Color?,
        left: Int,
    ) {
        val top = layout.rowTop(PANEL_TOP)
        val height = layout.rowHeight(PANEL_HEIGHT)
        val fill = color ?: fallbackPanelColor
        g.color = fill
        g.fillRect(left, top, layout.scoreWidth, height)
        val textColor = if ((fill.red + fill.green + fill.blue) / 3 > LIGHT_PANEL_THRESHOLD) darkText else white
        val nameHeight = (height * NAME_SHARE).toInt()
        val nameFont = ScoreboardText.sharedFont(g, listOf(name.uppercase() to layout.scoreWidth), nameHeight)
        ScoreboardText.paintLabel(g, name.uppercase(), left + layout.scoreWidth / 2, top, nameHeight, textColor, nameFont)
        val scoreHeight = height - nameHeight
        val scoreFont = ScoreboardText.sharedFont(g, listOf("88" to layout.scoreWidth), scoreHeight)
        ScoreboardText.paintLabel(g, score.toString(), left + layout.scoreWidth / 2, top + nameHeight, scoreHeight, textColor, scoreFont)
    }

    private fun paintClock(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
    ) {
        val labelTop = layout.rowTop(TIME_LABEL_TOP)
        val labelHeight = layout.rowHeight(TIME_LABEL_HEIGHT)
        val labelFont = ScoreboardText.sharedFont(g, listOf("TIME" to layout.centerWidth), labelHeight)
        ScoreboardText.paintLabel(g, "TIME", layout.centerX, labelTop, labelHeight, white, labelFont)
        val clock = ScoreboardText.clockText(situation.clockSeconds)
        val clockTop = layout.rowTop(CLOCK_TOP)
        val clockHeight = layout.rowHeight(CLOCK_HEIGHT)
        val clockFont = ScoreboardText.sharedFont(g, listOf("88:88" to layout.centerWidth), clockHeight)
        ScoreboardText.paintLabel(g, clock, layout.centerX, clockTop, clockHeight, white, clockFont)
    }

    private fun paintFooter(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
        frame: Int,
    ) {
        val top = layout.rowTop(FOOTER_TOP)
        val height = layout.rowHeight(FOOTER_HEIGHT)
        g.color = footerColor
        g.fillRect(layout.bounds.x + frame, top, layout.bounds.width - frame * 2, height)
        val quarter = "${ScoreboardText.ordinal(situation.quarter)} QTR"
        val downAndDistance = "${ScoreboardText.ordinal(situation.down)} & ${situation.yardsToGo}"
        val font = ScoreboardText.sharedFont(g, listOf(quarter to layout.scoreWidth, downAndDistance to layout.scoreWidth), height)
        ScoreboardText.paintLabel(g, quarter, layout.homeScoreCenterX, top, height, white, font)
        ScoreboardText.paintLabel(g, downAndDistance, layout.awayScoreCenterX, top, height, yellow, font)
    }
}
