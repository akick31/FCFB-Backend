package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.BowlField
import com.fcfb.arceus.model.Team
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.awt.Color

class BowlFieldStylingTest {
    private val home = team("Home", "#CC0000", "#FFFFFF")
    private val away = team("Away", "#0000CC", "#FFFF00")
    private val turf = FieldBackgroundPainter.TURF_COLOR

    @Test
    fun `team per side outlines each half with the team defending it`() {
        val styling = styling(BowlField())

        assertEquals(Color.decode("#CC0000"), styling.yardNumberOutline(30, top = true, home, away))
        assertEquals(Color.decode("#0000CC"), styling.yardNumberOutline(70, top = true, home, away))
    }

    @Test
    fun `team per side gives the top number at midfield to home and the bottom to away`() {
        val styling = styling(BowlField())

        assertEquals(Color.decode("#CC0000"), styling.yardNumberOutline(50, top = true, home, away))
        assertEquals(Color.decode("#0000CC"), styling.yardNumberOutline(50, top = false, home, away))
    }

    @Test
    fun `fixed yard number source paints one color everywhere`() {
        val field =
            BowlField().apply {
                yardNumberSource = "FIXED"
                yardNumberOutlineColor = "#123456"
            }

        assertEquals(Color.decode("#123456"), styling(field).yardNumberOutline(30, top = true, home, away))
        assertEquals(Color.decode("#123456"), styling(field).yardNumberOutline(70, top = false, home, away))
    }

    @Test
    fun `red zone marker follows the defending team unless a color is set or it is disabled`() {
        val enabled = { BowlField().apply { redZoneEnabled = true } }
        assertEquals(Color.decode("#CC0000"), styling(enabled()).redZoneBorder(20, home, away))
        assertEquals(Color.decode("#0000CC"), styling(enabled()).redZoneBorder(80, home, away))
        assertEquals(Color.decode("#00FF00"), styling(enabled().apply { redZoneBorderColor = "#00FF00" }).redZoneBorder(20, home, away))
        assertNull(styling(BowlField()).redZoneBorder(20, home, away))
    }

    @Test
    fun `end zone fill and logo follow the stored settings`() {
        val field =
            BowlField().apply {
                endZoneFill = "SECONDARY"
                leftEndZoneLogoUrl = "left.png"
                rightEndZoneLogoUrl = "right.png"
            }
        val styling = styling(field)

        assertEquals(Color.decode("#FFFFFF"), styling.endZoneOf(home, isHome = true).fill)
        assertEquals("left.png", styling.endZoneOf(home, isHome = true).logoUrl)
        assertEquals("right.png", styling.endZoneOf(away, isHome = false).logoUrl)
    }

    @Test
    fun `end zone fill none leaves the turf showing`() {
        assertNull(styling(BowlField().apply { endZoneFill = "NONE" }).endZoneOf(home, isHome = true).fill)
    }

    @Test
    fun `sideline accent belongs to the home team on the left setting`() {
        val field =
            BowlField().apply {
                leftOobLineColor = "#111111"
                rightOobLineColor = "#222222"
            }

        assertEquals(Color.decode("#111111"), styling(field).sidelineAccent(homeEnd = true))
        assertEquals(Color.decode("#222222"), styling(field).sidelineAccent(homeEnd = false))
        assertNull(styling(BowlField()).sidelineAccent(homeEnd = true))
    }

    private fun styling(field: BowlField) = BowlFieldStyling(field, home, away, turf)

    private fun team(
        name: String,
        primary: String,
        secondary: String,
    ) = Team().apply {
        this.name = name
        primaryColor = primary
        secondaryColor = secondary
    }
}
