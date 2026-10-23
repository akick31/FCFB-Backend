package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.GameSituation
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

object ScoreboardExtras {
    private const val MAX_TIMEOUTS = 3
    private const val INSET_SHARE = 0.08f
    private const val TIMEOUT_LABEL_SHARE = 0.36f
    private const val LAMP_SHARE = 0.3f
    private const val LAMP_ROW_CENTER = 0.7f
    private const val POSSESSION_LABEL_SHARE = 0.6f
    private const val BALL_HEIGHT_SHARE = 0.5f
    private const val POSSESSION_INSET_SHARE = 0.12f

    class Colors(
        val label: Color,
        val lit: Color,
        val unlit: Color,
        val panel: Color? = null,
    )

    class Row(
        val top: Int,
        val height: Int,
    )

    fun paintBothSides(
        g: Graphics2D,
        layout: ScoreboardLayout,
        situation: GameSituation,
        timeoutRow: Row,
        possessionRow: Row,
        colors: Colors,
        family: String = Font.SANS_SERIF,
    ) {
        layout.homeInfo?.let {
            paintTimeouts(g, it, timeoutRow, situation.homeTimeouts, colors, family)
            paintPossession(g, it, possessionRow, situation.possession == TeamSide.HOME, colors, family)
        }
        layout.awayInfo?.let {
            paintTimeouts(g, it, timeoutRow, situation.awayTimeouts, colors, family)
            paintPossession(g, it, possessionRow, situation.possession == TeamSide.AWAY, colors, family)
        }
    }

    private fun paintTimeouts(
        g: Graphics2D,
        range: IntRange,
        row: Row,
        timeouts: Int,
        colors: Colors,
        family: String,
    ) {
        val width = range.last - range.first
        if (width <= 0) return
        paintPanel(g, range, row, colors)
        val inset = (row.height * INSET_SHARE).toInt()
        val centerX = (range.first + range.last) / 2
        val labelHeight = (row.height * TIMEOUT_LABEL_SHARE).toInt()
        val font = ScoreboardText.sharedFont(g, listOf("TOL" to width - inset * 2), labelHeight, family)
        ScoreboardText.paintLabel(g, "TOL", centerX, row.top + inset, labelHeight, colors.label, font)
        val lamp = minOf((row.height * LAMP_SHARE).toInt(), (width - inset * 2) / (MAX_TIMEOUTS * 2)).coerceAtLeast(2)
        val lampsWidth = MAX_TIMEOUTS * lamp * 2 - lamp
        val lampTop = row.top + (row.height * LAMP_ROW_CENTER).toInt() - lamp / 2
        repeat(MAX_TIMEOUTS) { index ->
            g.color = if (index < timeouts) colors.lit else colors.unlit
            g.fillOval(centerX - lampsWidth / 2 + index * lamp * 2, lampTop, lamp, lamp)
        }
    }

    private fun paintPossession(
        g: Graphics2D,
        range: IntRange,
        row: Row,
        hasBall: Boolean,
        colors: Colors,
        family: String,
    ) {
        val width = range.last - range.first
        if (width <= 0) return
        paintPanel(g, range, row, colors)
        val labelWidth = (width * POSSESSION_LABEL_SHARE).toInt()
        val inset = (row.height * POSSESSION_INSET_SHARE).toInt()
        val labelHeight = row.height - inset * 2
        val font = ScoreboardText.sharedFont(g, listOf("POSS" to labelWidth), labelHeight, family)
        ScoreboardText.paintLabel(g, "POSS", range.first + labelWidth / 2, row.top + inset, labelHeight, colors.label, font)
        val ballHeight = (row.height * BALL_HEIGHT_SHARE).toInt().coerceAtLeast(2)
        val ballWidth = minOf(ballHeight * 2, width - labelWidth - 2).coerceAtLeast(2)
        g.color = if (hasBall) colors.lit else colors.unlit
        g.fillOval(
            range.first + labelWidth + (width - labelWidth - ballWidth) / 2,
            row.top + (row.height - ballHeight) / 2,
            ballWidth,
            ballHeight,
        )
    }

    private fun paintPanel(
        g: Graphics2D,
        range: IntRange,
        row: Row,
        colors: Colors,
    ) {
        colors.panel?.let {
            g.color = it
            g.fillRect(range.first, row.top, range.last - range.first, row.height)
        }
    }
}
