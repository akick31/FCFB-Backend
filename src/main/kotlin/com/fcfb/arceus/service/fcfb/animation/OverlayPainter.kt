package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.RenderingHints
import java.awt.image.BufferedImage

@Component
class OverlayPainter {
    fun applyOverlay(
        frames: List<BufferedImage>,
        overlay: OverlayType,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
    ): List<BufferedImage> {
        if (overlay == OverlayType.FLAG) {
            val offender = offendingTeam(play, homeTeam, awayTeam)
            val last = maxOf(1, frames.size - 1)
            return frames.mapIndexed { index, frame -> stampFlag(frame, offender, index.toFloat() / last) }
        }
        val label = labelFor(overlay, play) ?: return frames
        val overlayFrameCount = minOf(OVERLAY_FRAME_COUNT, frames.size)
        return frames.mapIndexed { index, frame ->
            if (index >= frames.size - overlayFrameCount) stampLabel(frame, label) else frame
        }
    }

    private fun stampLabel(
        frame: BufferedImage,
        label: String,
    ): BufferedImage {
        val g = frame.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        var textSize = TEXT_SIZE
        g.font = Font("Arial", Font.BOLD, textSize)
        while (textSize > MIN_TEXT_SIZE && g.fontMetrics.stringWidth(label) > frame.width - 4 * BACKGROUND_PADDING) {
            textSize -= 2
            g.font = Font("Arial", Font.BOLD, textSize)
        }
        val metrics = g.fontMetrics
        val width = metrics.stringWidth(label)
        val x = (frame.width - width) / 2
        val y = frame.height / 2 + metrics.ascent / 2

        g.color = Color(0, 0, 0, 140)
        g.fillRect(
            x - BACKGROUND_PADDING,
            y - metrics.ascent - BACKGROUND_PADDING / 2,
            width + BACKGROUND_PADDING * 2,
            metrics.ascent + metrics.descent + BACKGROUND_PADDING,
        )

        g.color = Color.WHITE
        g.drawString(label, x, y)
        g.dispose()
        return frame
    }

    private fun labelFor(
        overlay: OverlayType,
        play: Play,
    ): String? =
        when (overlay) {
            OverlayType.TOUCHDOWN_FLASH -> touchdownLabel(play)
            OverlayType.FIRST_DOWN_MARKER -> "FIRST DOWN"
            OverlayType.TURNOVER_FLAG -> turnoverLabel(play)
            OverlayType.SAFETY_FLASH -> "SAFETY!"
            OverlayType.KICK_GOOD -> if (play.playCall == PlayCall.FIELD_GOAL) "FIELD GOAL IS GOOD!" else "EXTRA POINT IS GOOD!"
            OverlayType.KICK_NO_GOOD -> "NO GOOD"
            OverlayType.KICK_BLOCKED -> "BLOCKED"
            OverlayType.MUFFED_BOUNCE -> "MUFFED"
            OverlayType.TOUCHBACK -> "TOUCHBACK"
            OverlayType.FLAG -> null
            OverlayType.TWO_POINT_SUCCESS -> "TWO-POINT CONVERSION GOOD!"
            OverlayType.TWO_POINT_FAILED -> "NO GOOD"
            OverlayType.SPIKE_ICON -> "SPIKE"
            OverlayType.KNEEL_ICON -> "KNEEL"
            OverlayType.DEFENSE_TWO_POINT_RETURN -> "DEFENSIVE TWO-POINT!"
            OverlayType.DEAD_PLAY -> "TIME EXPIRED"
            OverlayType.NONE -> null
        }

    private fun touchdownLabel(play: Play): String =
        when (play.actualResult) {
            ActualResult.KICK_SIX -> "KICK SIX!"
            ActualResult.TURNOVER_TOUCHDOWN -> if (play.playCall == PlayCall.PASS) "PICK SIX!" else "SCOOP AND SCORE!"
            ActualResult.RETURN_TOUCHDOWN -> "KICK RETURN TOUCHDOWN!"
            ActualResult.PUNT_RETURN_TOUCHDOWN -> "PUNT RETURN TOUCHDOWN!"
            ActualResult.KICKING_TEAM_TOUCHDOWN, ActualResult.PUNT_TEAM_TOUCHDOWN -> "RECOVERED FOR A TOUCHDOWN!"
            else -> "TOUCHDOWN!"
        }

    /** A penalty reads as the yellow flag banner rather than a callout, so it stays on screen for the whole dead play. */
    private fun stampFlag(
        frame: BufferedImage,
        team: String,
        progress: Float,
    ): BufferedImage {
        val g = frame.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val restX = frame.width / 2 - REF_REST_OFFSET
        val flagRestX = restX + FLAG_OFFSET_X
        val flagRestY = frame.height / 2 + FLAG_OFFSET_Y
        val throwIn = (progress / FLAG_THROW_END).coerceIn(0f, 1f)
        drawPenaltyFlag(g, lerp(-FLAG_THROW_FROM, flagRestX, throwIn), lerp(0, flagRestY, throwIn))

        val walk = ((progress - REF_ENTER_AT) / (WALK_IN_END - REF_ENTER_AT)).coerceIn(0f, 1f)
        val armFraction = ((progress - ARM_RAISE_START) / (ARM_RAISE_END - ARM_RAISE_START)).coerceIn(0f, 1f)
        if (progress >= REF_ENTER_AT) {
            drawReferee(g, lerp(-REF_ENTRY_FROM, restX, walk), frame.height / 2, armFraction)
        }

        if (progress >= FLAG_THROW_END) {
            drawFlagBar(g, frame)
        }
        if (progress >= ARM_RAISE_END) {
            drawHeadline(g, frame, team)
        }
        g.dispose()
        return frame
    }

    /** The flag bar goes up the moment the flag lands; the call itself waits until the referee has signalled it. */
    private fun drawFlagBar(
        g: Graphics2D,
        frame: BufferedImage,
    ) {
        g.font = Font("Arial", Font.BOLD, FLAG_TEXT_SIZE)
        val flagMetrics = g.fontMetrics
        val bannerHeight = flagMetrics.ascent + flagMetrics.descent + BACKGROUND_PADDING
        val bannerY = frame.height - bannerHeight
        g.color = FLAG_COLOR
        g.fillRect(0, bannerY, frame.width, bannerHeight)
        g.color = Color.BLACK
        val flagX = (frame.width - flagMetrics.stringWidth(FLAG_TEXT)) / 2
        g.drawString(FLAG_TEXT, flagX, bannerY + flagMetrics.ascent + BACKGROUND_PADDING / 2)
    }

    private fun drawHeadline(
        g: Graphics2D,
        frame: BufferedImage,
        team: String,
    ) {
        val headline = "DELAY OF GAME ON ${team.uppercase()}"
        g.font = Font("Arial", Font.BOLD, HEADLINE_TEXT_SIZE)
        val headlineMetrics = g.fontMetrics
        g.color = HEADLINE_BACKGROUND
        g.fillRect(0, 0, frame.width, headlineMetrics.ascent + headlineMetrics.descent + BACKGROUND_PADDING)
        g.color = Color.WHITE
        val headlineX = (frame.width - headlineMetrics.stringWidth(headline)) / 2
        g.drawString(headline, headlineX, headlineMetrics.ascent + BACKGROUND_PADDING / 2)
    }

    /** The signal is both arms crossed above the head, so [armFraction] swings them up from hanging at his sides. */
    private fun drawReferee(
        g: Graphics2D,
        centerX: Int,
        centerY: Int,
        armFraction: Float,
    ) {
        val torsoX = centerX - REF_TORSO_WIDTH / 2
        val torsoY = centerY - REF_TORSO_HEIGHT / 2
        val stripeWidth = REF_TORSO_WIDTH / REF_STRIPES

        g.color = REF_SKIN
        g.fillOval(centerX - REF_HEAD_RADIUS, torsoY - REF_HEAD_RADIUS * 2 - 2, REF_HEAD_RADIUS * 2, REF_HEAD_RADIUS * 2)

        for (stripe in 0 until REF_STRIPES) {
            g.color = if (stripe % 2 == 0) Color.WHITE else Color.BLACK
            g.fillRect(torsoX + stripe * stripeWidth, torsoY, stripeWidth, REF_TORSO_HEIGHT)
        }
        g.color = Color.BLACK
        g.drawRect(torsoX, torsoY, stripeWidth * REF_STRIPES, REF_TORSO_HEIGHT)

        g.stroke = BasicStroke(REF_LIMB_WIDTH)
        g.color = REF_SKIN
        val right = torsoX + stripeWidth * REF_STRIPES
        val shoulderY = torsoY + REF_ARM_TOP
        val restY = torsoY + REF_TORSO_HEIGHT + REF_ARM_REST
        val foldY = torsoY + REF_FOLD_Y
        val leftElbowX = lerp(torsoX - REF_ARM_SPREAD, torsoX - 1, armFraction)
        val rightElbowX = lerp(right + REF_ARM_SPREAD, right + 1, armFraction)
        val elbowY = lerp(restY, foldY, armFraction)
        val upperY = elbowY + REF_FOLD_GAP
        val leftReach = lerp(leftElbowX, right + REF_FOLD_REACH, armFraction)
        val rightReach = lerp(rightElbowX, torsoX - REF_FOLD_REACH, armFraction)
        g.drawLine(torsoX + 1, shoulderY, leftElbowX, elbowY)
        g.drawLine(right - 1, shoulderY, rightElbowX, upperY)
        drawForearm(g, leftElbowX, elbowY, leftReach)
        drawForearm(g, rightElbowX, upperY, rightReach)

        g.color = Color.BLACK
        g.drawLine(centerX - 3, torsoY + REF_TORSO_HEIGHT, centerX - 5, torsoY + REF_TORSO_HEIGHT + REF_LEG)
        g.drawLine(centerX + 3, torsoY + REF_TORSO_HEIGHT, centerX + 5, torsoY + REF_TORSO_HEIGHT + REF_LEG)
    }

    /** Outlined so a bare arm never disappears against the white stripes of the shirt it is folded across. */
    private fun drawForearm(
        g: Graphics2D,
        fromX: Int,
        y: Int,
        toX: Int,
    ) {
        g.color = Color.BLACK
        g.stroke = BasicStroke(REF_LIMB_WIDTH + 2f)
        g.drawLine(fromX, y, toX, y)
        g.color = REF_SKIN
        g.stroke = BasicStroke(REF_LIMB_WIDTH)
        g.drawLine(fromX, y, toX, y)
    }

    /** A thrown flag lands as a rumpled square of cloth with the weighted corner knotted up at one end. */
    private fun drawPenaltyFlag(
        g: Graphics2D,
        x: Int,
        y: Int,
    ) {
        val cloth = Polygon()
        cloth.addPoint(x, y + FLAG_CLOTH / 3)
        cloth.addPoint(x + FLAG_CLOTH / 2, y)
        cloth.addPoint(x + FLAG_CLOTH, y + FLAG_CLOTH / 4)
        cloth.addPoint(x + FLAG_CLOTH - 2, y + FLAG_CLOTH)
        cloth.addPoint(x + FLAG_CLOTH / 3, y + FLAG_CLOTH - 1)
        g.color = FLAG_COLOR
        g.fillPolygon(cloth)
        g.color = FLAG_OUTLINE
        g.drawPolygon(cloth)
        g.fillOval(x + FLAG_CLOTH - FLAG_KNOT, y + FLAG_CLOTH - FLAG_KNOT, FLAG_KNOT, FLAG_KNOT)
    }

    private fun lerp(
        from: Int,
        to: Int,
        fraction: Float,
    ): Int = (from + (to - from) * fraction).toInt()

    private fun offendingTeam(
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
    ): String = (if (play.possession == TeamSide.HOME) homeTeam.name else awayTeam.name).orEmpty()

    private fun turnoverLabel(play: Play): String =
        when {
            play.actualResult == ActualResult.TURNOVER_ON_DOWNS -> "TURNOVER ON DOWNS"
            play.playCall == PlayCall.PASS -> "INTERCEPTED"
            else -> "FUMBLE"
        }

    companion object {
        private const val OVERLAY_FRAME_COUNT = 3
        private const val TEXT_SIZE = 56
        private const val MIN_TEXT_SIZE = 24
        private const val BACKGROUND_PADDING = 16
        private const val FLAG_TEXT = "FLAG"
        private const val FLAG_TEXT_SIZE = 44
        private const val HEADLINE_TEXT_SIZE = 26
        private const val REF_TORSO_WIDTH = 28
        private const val REF_TORSO_HEIGHT = 34
        private const val REF_STRIPES = 7
        private const val REF_HEAD_RADIUS = 8
        private const val REF_ARM = 11
        private const val REF_LEG = 16
        private const val REF_LIMB_WIDTH = 3f
        private const val FLAG_OFFSET_X = 44
        private const val FLAG_OFFSET_Y = 30
        private const val REF_ARM_TOP = 6
        private const val REF_ARM_REST = 4
        private const val REF_FOLD_Y = 20
        private const val REF_FOLD_GAP = 5
        private const val REF_FOLD_REACH = 4
        private const val REF_ARM_SPREAD = 4
        private const val REF_REST_OFFSET = 235
        private const val REF_ENTRY_FROM = 70
        private const val FLAG_THROW_FROM = 60
        private const val FLAG_THROW_END = 0.16f
        private const val REF_ENTER_AT = 0.18f
        private const val WALK_IN_END = 0.5f
        private const val ARM_RAISE_START = 0.5f
        private const val ARM_RAISE_END = 0.66f
        private const val BANNER_AT = 0.16f
        private const val FLAG_CLOTH = 20
        private const val FLAG_KNOT = 6
        private val REF_SKIN = Color(224, 184, 148)
        private val FLAG_OUTLINE = Color(200, 170, 20)
        private val FLAG_COLOR = Color(245, 215, 40)
        private val HEADLINE_BACKGROUND = Color(0, 0, 0, 170)
    }
}
