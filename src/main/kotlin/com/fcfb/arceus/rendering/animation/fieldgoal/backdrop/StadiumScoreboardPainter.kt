package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.ScoreboardStyle
import com.fcfb.arceus.model.animation.GameSituation
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard.ClassicScoreboard
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard.ScoreboardDesign
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard.ScoreboardHousing
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard.ScoreboardLayout
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard.SegmentScoreboard
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard.VideoScoreboard
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard.VintageScoreboard
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints

object StadiumScoreboardPainter {
    private const val REFERENCE_HEIGHT = 135
    private const val MIN_BOARD_SCALE = 0.65f
    private const val SIDE_WIDTH_SHARE = 0.74f
    private const val NET_CLEARANCE = 4
    private const val MAX_WIDTH_SHARE = 0.98f

    fun paletteOf(style: ScoreboardStyle): List<Color> = designOf(style).palette + PylonPainter.PALETTE + ScoreboardHousing.PALETTE

    fun boardBounds(
        area: BackdropArea,
        bottomY: Int,
        sizeFactor: Float,
        widthFactor: Float,
        spanWidth: Int?,
        fixedScale: Boolean,
    ): Rectangle {
        val boardScale = if (fixedScale) area.scale else area.scale.coerceAtLeast(MIN_BOARD_SCALE)
        val height = (REFERENCE_HEIGHT * boardScale * sizeFactor).toInt()
        val naturalWidth = 2 * (centerHalfWidth(area) + (height * SIDE_WIDTH_SHARE).toInt())
        val widest = maxOf((naturalWidth * widthFactor).toInt(), spanWidth ?: 0)
        val width = if (fixedScale) widest else minOf(widest, (area.width * MAX_WIDTH_SHARE).toInt())
        return Rectangle(area.centerX - width / 2, bottomY - height, width, height)
    }

    fun paint(
        g: Graphics2D,
        bounds: Rectangle,
        area: BackdropArea,
        settings: JumbotronSettings,
        situation: GameSituation,
        homeColor: Color?,
        awayColor: Color?,
    ) {
        val previousAntialias = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        val design = designOf(settings.style)
        val shape = settings.shape
        val layout = ScoreboardLayout.of(bounds, centerHalfWidth(area), area.uprightClearHalfWidth)
        val casing = settings.casing?.let { ScoreboardHousing.withCasing(bounds) }
        ScoreboardHousing.paintTopper(g, shape, casing ?: bounds, design, settings.title, settings.logo, settings.topperColor)
        casing?.let { ScoreboardHousing.paintCasing(g, shape, it, settings.casing) }
        val outline = ScoreboardHousing.outlineOf(shape, bounds)
        val previousClip = g.clip
        outline?.let { g.clip(it) }
        design.paint(g, layout, situation, homeColor, awayColor)
        g.clip = previousClip
        outline?.let { ScoreboardHousing.paintTrim(g, it, bounds, design) }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAntialias)
    }

    private fun centerHalfWidth(area: BackdropArea): Int = area.netHalfWidth + area.scaled(NET_CLEARANCE)

    private fun designOf(style: ScoreboardStyle): ScoreboardDesign =
        when (style) {
            ScoreboardStyle.CLASSIC -> ClassicScoreboard
            ScoreboardStyle.VIDEO -> VideoScoreboard
            ScoreboardStyle.VINTAGE -> VintageScoreboard
            ScoreboardStyle.SEGMENT -> SegmentScoreboard
        }
}
