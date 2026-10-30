package com.fcfb.arceus.rendering.animation.field

import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.appearance.TeamField
import com.fcfb.arceus.model.team.Team
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage

class MowingPainterTest {
    private val team = Team().apply { name = "Home" }
    private val field = Rectangle(0, 0, 200, 120)

    private fun render(
        surface: String?,
        pattern: String? = null,
    ): BufferedImage {
        val teamField =
            TeamField().apply {
                this.surface = surface
                mowingPattern = pattern
            }
        val theme = FieldTheme(FieldStyle.HOME_FIELD, team, team, null, homeField = teamField)
        val image = BufferedImage(200, 120, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = theme.turf
        g.fillRect(0, 0, 200, 120)
        MowingPainter.paintOverhead(g, theme, field, 20f)
        return image
    }

    @Test
    fun `grass alternates light and dark bands`() {
        val image = render("GRASS")

        assertNotEquals(image.getRGB(10, 60), image.getRGB(30, 60))
        assertEquals(image.getRGB(10, 60), image.getRGB(50, 60))
    }

    @Test
    fun `turf stays one solid color`() {
        val image = render("TURF")

        assertEquals(image.getRGB(10, 60), image.getRGB(30, 60))
        assertEquals(Color(image.getRGB(10, 60)), FieldTheme(FieldStyle.HOME_FIELD, team, team, null).turf)
    }

    @Test
    fun `grass without a mowing pattern stays solid`() {
        val image = render("GRASS", "NONE")

        assertEquals(image.getRGB(10, 60), image.getRGB(30, 60))
    }
}
