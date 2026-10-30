package com.fcfb.arceus.util

import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.StadiumDesign
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PropertyCopyTest {
    @Test
    fun `every editable property is copied except the ones named`() {
        val source =
            BowlField().apply {
                bowl = "Rose Bowl Game"
                venue = "Rose Bowl"
                wallText = "THE GRANDDADDY"
            }
        val target = BowlField().apply { bowl = "Sugar Bowl" }

        source.copyInto(target)

        assertEquals("Sugar Bowl", target.bowl)
        assertEquals("Rose Bowl", target.venue)
        assertEquals("THE GRANDDADDY", target.wallText)
    }

    @Test
    fun `a stadium copy carries every setting`() {
        val source =
            StadiumDesign().apply {
                stadiumBackdrop = "LAKE"
                rightJumbotronStyle = "SEGMENT"
                jumbotronWidth = 1.5
                facilityFullWidth = true
            }
        val target = StadiumDesign()

        source.copyInto(target)

        assertEquals("LAKE", target.stadiumBackdrop)
        assertEquals("SEGMENT", target.rightJumbotronStyle)
        assertEquals(1.5, target.jumbotronWidth)
        assertEquals(true, target.facilityFullWidth)
    }
}
