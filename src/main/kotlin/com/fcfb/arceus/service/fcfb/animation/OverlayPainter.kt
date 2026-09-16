package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Font
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
            OverlayType.TOUCHDOWN_FLASH -> "TOUCHDOWN!"
            OverlayType.FIRST_DOWN_MARKER -> "FIRST DOWN"
            OverlayType.TURNOVER_FLAG -> turnoverLabel(play)
            OverlayType.SAFETY_FLASH -> "SAFETY!"
            OverlayType.KICK_GOOD -> if (play.playCall == PlayCall.FIELD_GOAL) "FIELD GOAL IS GOOD!" else "EXTRA POINT IS GOOD!"
            OverlayType.KICK_NO_GOOD -> "NO GOOD"
            OverlayType.KICK_BLOCKED -> "BLOCKED"
            OverlayType.MUFFED_BOUNCE -> "MUFFED"
            OverlayType.TWO_POINT_SUCCESS -> "TWO-POINT CONVERSION GOOD!"
            OverlayType.TWO_POINT_FAILED -> "NO GOOD"
            OverlayType.SPIKE_ICON -> "SPIKE"
            OverlayType.KNEEL_ICON -> "KNEEL"
            OverlayType.DEFENSE_TWO_POINT_RETURN -> "DEFENSIVE TWO-POINT!"
            OverlayType.DEAD_PLAY -> "TIME EXPIRED"
            OverlayType.NONE -> null
        }

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
    }
}
