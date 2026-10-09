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
}
