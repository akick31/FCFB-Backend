package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

private data class Slot(val dx: Int, val dy: Int)

object PlayerFormationPainter {
    private const val SNAP_WINDOW = 0.18f
    private const val OL_SPACING = 22
    private const val OL_SURGE_DISTANCE = 10
    private const val PURSUIT_WINDOW_START = 0.15f
    private const val PURSUIT_STRENGTH_NORMAL = 0.35f
    private const val PURSUIT_STRENGTH_CONVERGING = 0.85f
    private const val CARRIER_OFFSET_X = -14
    private const val CARRIER_OFFSET_Y = 10
    private const val FONT_SIZE = 22

    private val CONVERGING_RESULTS =
        setOf(
            ActualResult.LOSS,
            ActualResult.SAFETY,
            ActualResult.TURNOVER,
            ActualResult.TURNOVER_ON_DOWNS,
            ActualResult.TURNOVER_TOUCHDOWN,
            ActualResult.KICK_SIX,
            ActualResult.BLOCKED,
            ActualResult.DEFENSE_TWO_POINT,
        )

    private val DEFENSE_CARRIES_BALL =
        setOf(
            ActualResult.TURNOVER_TOUCHDOWN,
            ActualResult.KICK_SIX,
            ActualResult.DEFENSE_TWO_POINT,
            ActualResult.RETURN_TOUCHDOWN,
            ActualResult.PUNT_RETURN_TOUCHDOWN,
        )

    fun draw(
        image: BufferedImage,
        play: Play,
        startAbs: Int,
        ballX: Int,
        ballY: Int,
        t: Float,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
    ) {
        val losX = FieldCoordinateMapper.toPixelX(startAbs.coerceIn(0, 100), FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
        val behindSign = if (play.possession == TeamSide.HOME) -1 else 1
        val snapProgress = (t / SNAP_WINDOW).coerceIn(0f, 1f)
        val pursuitProgress = ((t - PURSUIT_WINDOW_START) / (1f - PURSUIT_WINDOW_START)).coerceIn(0f, 1f)
        val pursuitStrength = if (play.actualResult in CONVERGING_RESULTS) PURSUIT_STRENGTH_CONVERGING else PURSUIT_STRENGTH_NORMAL

        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.font = Font("Arial", Font.BOLD, FONT_SIZE)

        val offenseSlots = offensiveSlots(offensivePlaybook)
        val carrierIndex = carrierSlotIndex(play.playCall)
        val ballCarriedByDefense = play.actualResult in DEFENSE_CARRIES_BALL

        offenseSlots.forEachIndexed { index, slot ->
            val surge = if (index < 5) behindSign * -OL_SURGE_DISTANCE * snapProgress else 0f
            val (x, y) =
                if (index == carrierIndex && !ballCarriedByDefense) {
                    trackBall(slot, losX, behindSign, snapProgress, ballX, ballY)
                } else {
                    (losX + behindSign * slot.dx + surge).toInt() to (FieldBackgroundPainter.HEIGHT / 2 + slot.dy)
                }
            drawMarker(g, x, y, "X")
        }

        val defenseSlots = defensiveSlots(defensivePlaybook)
        val defenderCarrierIndex = if (ballCarriedByDefense) defenseSlots.indices.last else -1
        defenseSlots.forEachIndexed { index, slot ->
            val baseX = losX - behindSign * slot.dx
            val baseY = FieldBackgroundPainter.HEIGHT / 2 + slot.dy
            val (x, y) =
                if (index == defenderCarrierIndex) {
                    ballX + CARRIER_OFFSET_X to ballY + CARRIER_OFFSET_Y
                } else {
                    val pull = pursuitProgress * pursuitStrength
                    (baseX + (ballX - baseX) * pull).toInt() to (baseY + (ballY - baseY) * pull).toInt()
                }
            drawMarker(g, x, y, "O")
        }

        g.dispose()
    }

    fun drawKickoff(
        image: BufferedImage,
        play: Play,
        startAbs: Int,
        ballX: Int,
        ballY: Int,
        t: Float,
        onside: Boolean,
    ) {
        val losX = FieldCoordinateMapper.toPixelX(startAbs.coerceIn(0, 100), FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
        val behindSign = if (play.possession == TeamSide.HOME) -1 else 1
        val centerY = FieldBackgroundPainter.HEIGHT / 2
        val chaseProgress = t

        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.font = Font("Arial", Font.BOLD, FONT_SIZE)

        val spread = if (onside) ONSIDE_SPREAD else KICK_LINE_SPREAD
        for (i in 0 until 10) {
            val dy = (i - 4.5) * spread
            val x = (losX - behindSign * KICK_LINE_DEPTH + (ballX - losX) * chaseProgress * KICK_CHASE_FACTOR).toInt()
            drawMarker(g, x, centerY + dy.toInt(), "X")
        }

        val wallDepthAbs = if (onside) ONSIDE_RETURN_DEPTH else RETURN_WALL_DEPTH
        val wallX = losX + behindSign * wallDepthAbs
        for (i in 0 until 9) {
            val dy = (i - 4) * spread
            drawMarker(g, wallX, centerY + dy.toInt(), "O")
        }
        drawMarker(g, ballX + CARRIER_OFFSET_X, ballY + CARRIER_OFFSET_Y, "O")

        g.dispose()
    }

    private fun trackBall(
        formationSlot: Slot,
        losX: Int,
        behindSign: Int,
        snapProgress: Float,
        ballX: Int,
        ballY: Int,
    ): Pair<Int, Int> {
        val originX = losX + behindSign * formationSlot.dx
        val originY = FieldBackgroundPainter.HEIGHT / 2 + formationSlot.dy
        val x = originX + (ballX - originX) * snapProgress
        val y = originY + (ballY - originY) * snapProgress
        return (x.toInt() + CARRIER_OFFSET_X) to (y.toInt() + CARRIER_OFFSET_Y)
    }

    private fun drawMarker(
        g: java.awt.Graphics2D,
        x: Int,
        y: Int,
        label: String,
    ) {
        g.color = FieldBackgroundPainter.LINE_COLOR
        val width = g.fontMetrics.stringWidth(label)
        g.drawString(label, x - width / 2, y + g.fontMetrics.ascent / 2)
    }

    private fun carrierSlotIndex(playCall: PlayCall?): Int =
        when (playCall) {
            PlayCall.PASS -> 7
            else -> 6
        }

    private fun offensiveSlots(playbook: OffensivePlaybook): List<Slot> {
        val ol = (0 until 5).map { Slot(0, (it - 2) * OL_SPACING) }
        val skill =
            when (playbook) {
                OffensivePlaybook.FLEXBONE ->
                    listOf(Slot(60, 0), Slot(35, -45), Slot(35, 45), Slot(0, -160), Slot(0, 160), Slot(0, 90))
                OffensivePlaybook.SPREAD, OffensivePlaybook.AIR_RAID ->
                    listOf(Slot(65, 0), Slot(50, 25), Slot(0, -170), Slot(0, -100), Slot(0, 100), Slot(0, 170))
                OffensivePlaybook.PRO, OffensivePlaybook.WEST_COAST ->
                    listOf(Slot(10, 0), Slot(50, -20), Slot(40, 20), Slot(0, -140), Slot(0, 140), Slot(0, 70))
            }
        return ol + skill
    }

    private fun defensiveSlots(playbook: DefensivePlaybook): List<Slot> {
        val (dl, lb, db) =
            when (playbook) {
                DefensivePlaybook.FOUR_THREE -> Triple(4, 3, 4)
                DefensivePlaybook.THREE_FOUR -> Triple(3, 4, 4)
                DefensivePlaybook.FIVE_TWO -> Triple(5, 2, 4)
                DefensivePlaybook.FOUR_FOUR -> Triple(4, 4, 3)
                DefensivePlaybook.THREE_THREE_FIVE -> Triple(3, 3, 5)
            }
        val dlSlots = spread(dl, DL_DEPTH, DL_WIDTH)
        val lbSlots = spread(lb, LB_DEPTH, LB_WIDTH)
        val dbSlots = spread(db, DB_DEPTH, DB_WIDTH)
        return dlSlots + lbSlots + dbSlots
    }

    private fun spread(
        count: Int,
        depth: Int,
        width: Int,
    ): List<Slot> {
        if (count == 1) return listOf(Slot(depth, 0))
        val step = width * 2 / (count - 1)
        return (0 until count).map { Slot(depth, -width + it * step) }
    }

    private const val DL_DEPTH = 8
    private const val DL_WIDTH = 60
    private const val LB_DEPTH = 55
    private const val LB_WIDTH = 90
    private const val DB_DEPTH = 130
    private const val DB_WIDTH = 170

    private const val KICK_LINE_DEPTH = 5
    private const val KICK_LINE_SPREAD = 40
    private const val KICK_CHASE_FACTOR = 0.9
    private const val ONSIDE_SPREAD = 14
    private const val RETURN_WALL_DEPTH = 45
    private const val ONSIDE_RETURN_DEPTH = 15
}
