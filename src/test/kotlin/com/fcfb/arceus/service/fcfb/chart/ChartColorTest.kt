package com.fcfb.arceus.service.fcfb.chart

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Color

class ChartColorTest {
    private val background = Color(40, 40, 40)

    @Test
    fun `keeps a visible primary color`() {
        assertEquals(Color.RED, ChartColor.select(Color.RED, Color.WHITE, background))
    }

    @Test
    fun `uses secondary for black and navy primary colors`() {
        for (primary in listOf(Color.BLACK, Color.decode("#002244"), Color.BLUE, background)) {
            assertEquals(Color.YELLOW, ChartColor.select(primary, Color.YELLOW, background))
        }
    }

    @Test
    fun `uses white when both team colors blend into background`() {
        assertEquals(Color.WHITE, ChartColor.select(Color.BLACK, Color.decode("#002244"), background))
    }

    @Test
    fun `uses secondary when primary is missing`() {
        assertEquals(Color.YELLOW, ChartColor.select(null, Color.YELLOW, background))
    }

    @Test
    fun `uses white when no visible color is available`() {
        assertEquals(Color.WHITE, ChartColor.select(null, null, background))
        assertEquals(Color.WHITE, ChartColor.select(Color.BLACK, null, background))
    }

    private val darkSurface = Color.decode("#102433")

    private fun matchup(
        homePrimary: String,
        homeSecondary: String,
        awayPrimary: String,
        awaySecondary: String,
    ) = ChartColor.selectMatchup(
        Color.decode(homePrimary),
        Color.decode(homeSecondary),
        Color.decode(awayPrimary),
        Color.decode(awaySecondary),
        darkSurface,
    )

    @Test
    fun `keeps both primaries when they contrast with the background and each other`() {
        assertEquals(Color.decode("#e03030") to Color.decode("#3399ff"), matchup("#e03030", "#000000", "#3399ff", "#ffffff"))
    }

    @Test
    fun `moves the away team to its secondary when both primaries are the same red`() {
        assertEquals(Color.decode("#e03030") to Color.decode("#ffcc00"), matchup("#e03030", "#ffffff", "#e53a3a", "#ffcc00"))
    }

    @Test
    fun `skips a dark purple primary on the dark surface`() {
        assertEquals(Color.decode("#c0c0c0"), matchup("#3b1a6b", "#c0c0c0", "#0033a0", "#ffffff").first)
    }

    @Test
    fun `falls back to an accent when nothing distinct is visible`() {
        assertEquals(Color.decode("#3d8bd9"), matchup("#ffffff", "#ffffff", "#fefefe", "#fdfdfd").second)
    }

    @Test
    fun `does not pair silver against white`() {
        assertEquals(Color.WHITE to Color.decode("#f0405a"), matchup("#ffffff", "#000000", "#c0c0c0", "#f0405a"))
    }

    @Test
    fun `uses the accent when both teams are neutral`() {
        assertEquals(Color.decode("#3d8bd9"), matchup("#ffffff", "#000000", "#c0c0c0", "#d0d0d0").second)
    }

    @Test
    fun `never gives the away team the same blue as a blue home team`() {
        assertEquals(Color.WHITE, matchup("#3d8bd9", "#3d8bd9", "#111111", "#151515").second)
    }

    @Test
    fun `avoids an accent that matches the home brand blue even when home shows white`() {
        assertEquals(Color.WHITE to Color.decode("#e0a526"), matchup("#002e5d", "#ffffff", "#512888", "#d1d1d1"))
    }
}
