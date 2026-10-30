package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.team.Team
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class CrowdBackdropTest {
    private val area = BackdropArea(width = 600, groundY = 300, topY = 0, scale = 1f, netHalfWidth = 40, uprightClearHalfWidth = 60)

    private fun team(color: String) =
        Team().apply {
            name = color
            primaryColor = color
            secondaryColor = color
        }

    private fun fanPixels(fill: Double): Int {
        val theme = FieldTheme(FieldStyle.HOME_FIELD, team("#C8102E"), team("#C8102E"), null, gameId = 7, crowdFill = fill)
        val image = BufferedImage(area.width, area.groundY, BufferedImage.TYPE_INT_RGB)
        CrowdBackdrop.paint(image.createGraphics(), area, theme, TeamSide.HOME)
        var count = 0
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                if (image.getRGB(x, y) and 0xFFFFFF == 0xC8102E) count++
            }
        }
        return count
    }

    @Test
    fun `a half-full crowd paints about half the fans`() {
        val ratio = fanPixels(0.5).toDouble() / fanPixels(1.0)

        assertTrue(ratio in 0.4..0.6, "ratio was $ratio")
    }

    @Test
    fun `upper tiers empty first while the stands average the fill`() {
        val shares = (0 until 9).map { CrowdBackdrop.keepShare(0.6, it, 9) }

        assertEquals(0.6, shares.average(), 1e-9)
        assertTrue(shares.first() > shares.last())
        assertEquals(List(9) { 1.0 }, (0 until 9).map { CrowdBackdrop.keepShare(1.0, it, 9) })
    }
}
