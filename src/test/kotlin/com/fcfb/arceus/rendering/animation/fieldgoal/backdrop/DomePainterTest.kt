package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.DomeStyle
import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.animation.GameSituation
import com.fcfb.arceus.model.appearance.StadiumDesign
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.rendering.animation.fieldgoal.GoalPostScenePainter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.awt.Color
import java.awt.image.BufferedImage

class DomePainterTest {
    private val team =
        Team().apply {
            name = "Idaho"
            primaryColor = "#B3A369"
            secondaryColor = "#000000"
        }
    private val area = BackdropArea(width = 800, groundY = 250, topY = 0, scale = 1f, netHalfWidth = 0, uprightClearHalfWidth = 0)

    private fun theme(design: StadiumDesign) = FieldTheme(FieldStyle.HOME_FIELD, team, team, null, stadium = design)

    @ParameterizedTest
    @EnumSource(value = DomeStyle::class, names = ["SUITES", "FABRIC"])
    fun `no sky shows under a closed dome`(style: DomeStyle) {
        val design =
            StadiumDesign().apply {
                stadiumBackdrop = "NONE"
                wallEnabled = false
                domeStyle = style.name
            }
        val play =
            Play().apply {
                playCall = PlayCall.FIELD_GOAL
                ballLocation = 80
                possession = TeamSide.HOME
            }
        val scene =
            GoalPostScenePainter.paint(
                theme(design),
                theme(design).endZoneOf(TeamSide.HOME),
                GoalPostScenePainter.layoutFor(play),
                true,
                TeamSide.HOME,
                GameSituation.sample("Idaho", "Idaho"),
            )
        val sky = GoalPostScenePainter.SKY_COLOR.rgb
        var skyPixels = 0
        for (x in 0 until scene.width) for (y in 0 until scene.height) if (scene.getRGB(x, y) == sky) skyPixels++

        assertEquals(0, skyPixels)
    }

    @org.junit.jupiter.api.Test
    fun `windowed domes leave the windows open and wall between them`() {
        val image = BufferedImage(800, 250, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.MAGENTA
        g.fillRect(0, 0, 800, 250)
        DomePainter.paint(g, area, DomeStyle.WINDOWED, 250)
        val first = DomePainter.windows(area, 250).first()
        val betweenX = first.x + first.width + 10

        assertEquals(Color.MAGENTA.rgb, image.getRGB(first.x + first.width / 4, first.centerY.toInt()))
        assertNotEquals(Color.MAGENTA.rgb, image.getRGB(betweenX, first.centerY.toInt()))
    }

    @org.junit.jupiter.api.Test
    fun `domes have no perimeter fence`() {
        val design =
            StadiumDesign().apply {
                stadiumBackdrop = "NONE"
                wallEnabled = false
                domeStyle = "SUITES"
            }
        val image = BufferedImage(800, 300, BufferedImage.TYPE_INT_RGB)
        StadiumBackdropPainter.paintForeground(image.createGraphics(), area.copy(behindWall = false), theme(design), TeamSide.HOME)

        var painted = 0
        for (x in 0 until image.width) for (y in 0 until image.height) if (image.getRGB(x, y) and 0xFFFFFF != 0) painted++
        assertTrue(painted == 0, "fence pixels: $painted")
    }
}
