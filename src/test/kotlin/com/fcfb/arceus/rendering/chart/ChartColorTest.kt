package com.fcfb.arceus.rendering.chart

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Color

class ChartColorTest {
    private val background = Color(40, 40, 40)

    // A reddish shade close enough to Color.RED to be flagged "too similar" by ColorSimilarity,
    // while still reading on its own against `background` (contrast >= 3.0).
    private val clashingRed = Color(240, 40, 40)

    @Test
    fun `keeps a visible primary color`() {
        assertEquals(Color.RED, ChartColor().select(Color.RED, Color.WHITE, background))
    }

    @Test
    fun `uses secondary for black and navy primary colors`() {
        for (primary in listOf(Color.BLACK, Color.decode("#002244"), Color.BLUE, background)) {
            assertEquals(Color.YELLOW, ChartColor().select(primary, Color.YELLOW, background))
        }
    }

    @Test
    fun `uses white when both team colors blend into background`() {
        assertEquals(Color.WHITE, ChartColor().select(Color.BLACK, Color.decode("#002244"), background))
    }

    @Test
    fun `uses secondary when primary is missing`() {
        assertEquals(Color.YELLOW, ChartColor().select(null, Color.YELLOW, background))
    }

    @Test
    fun `uses white when no visible color is available`() {
        assertEquals(Color.WHITE, ChartColor().select(null, null, background))
        assertEquals(Color.WHITE, ChartColor().select(Color.BLACK, null, background))
    }

    @Test
    fun `selectPair keeps both primaries when they are distinct`() {
        val (home, away) = ChartColor().selectPair(Color.RED, Color.WHITE, Color.YELLOW, Color.CYAN, background)
        assertEquals(Color.RED, home)
        assertEquals(Color.YELLOW, away)
    }

    @Test
    fun `selectPair sends the away team to its secondary when primaries clash`() {
        val (home, away) = ChartColor().selectPair(Color.RED, Color.WHITE, clashingRed, Color.YELLOW, background)
        assertEquals(Color.RED, home)
        assertEquals(Color.YELLOW, away)
    }

    @Test
    fun `selectPair also sends the home team to its secondary when the away secondary still blends into the background`() {
        val homePrimary = Color.RED
        val homeSecondary = Color.CYAN
        val awaySecondary = Color.BLACK

        val (home, away) = ChartColor().selectPair(homePrimary, homeSecondary, clashingRed, awaySecondary, background)

        assertEquals(homeSecondary, home)
        assertEquals(awaySecondary, away)
    }

    @Test
    fun `selectPair forces the away team to white when neither secondary reads against the background`() {
        val homePrimary = Color.RED
        val homeSecondary = Color.BLACK
        val awaySecondary = Color.BLACK

        val (home, away) = ChartColor().selectPair(homePrimary, homeSecondary, clashingRed, awaySecondary, background)

        assertEquals(homePrimary, home)
        assertEquals(Color.WHITE, away)
    }

    @Test
    fun `selectPair falls back to white for a clashing team with no secondary color`() {
        val (home, away) = ChartColor().selectPair(Color.RED, null, clashingRed, null, background)
        assertEquals(Color.RED, home)
        assertEquals(Color.WHITE, away)
    }
}
