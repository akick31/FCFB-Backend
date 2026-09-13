package com.fcfb.arceus.service.fcfb.animation

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
        val label = labelFor(overlay) ?: return frames
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
        g.font = Font("Arial", Font.BOLD, 22)
        g.color = Color.YELLOW
        val width = g.fontMetrics.stringWidth(label)
        g.drawString(label, (frame.width - width) / 2, 30)
        g.dispose()
        return frame
    }

    private fun labelFor(overlay: OverlayType): String? =
        when (overlay) {
            OverlayType.TOUCHDOWN_FLASH -> "TOUCHDOWN"
            OverlayType.FIRST_DOWN_MARKER -> "FIRST DOWN"
            OverlayType.TURNOVER_FLAG -> "TURNOVER"
            OverlayType.SAFETY_FLASH -> "SAFETY"
            OverlayType.KICK_GOOD -> "GOOD"
            OverlayType.KICK_NO_GOOD -> "NO GOOD"
            OverlayType.KICK_BLOCKED -> "BLOCKED"
            OverlayType.MUFFED_BOUNCE -> "MUFFED"
            OverlayType.TWO_POINT_SUCCESS -> "GOOD"
            OverlayType.TWO_POINT_FAILED -> "NO GOOD"
            OverlayType.SPIKE_ICON -> "SPIKE"
            OverlayType.KNEEL_ICON -> "KNEEL"
            OverlayType.DEAD_PLAY -> null
            OverlayType.NONE -> null
        }

    companion object {
        private const val OVERLAY_FRAME_COUNT = 3
    }
}
