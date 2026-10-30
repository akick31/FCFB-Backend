package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.model.appearance.TeamField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FieldAppearanceServiceTest {
    @Test
    fun `a team field keeps its surface and mowing pattern, and a request without them leaves them alone`() {
        val field = TeamField()

        FieldAppearanceService().applyFields(field, TeamFieldRequest(surface = "GRASS", mowingPattern = "CHECKERBOARD"))
        FieldAppearanceService().applyFields(field, TeamFieldRequest(turfColor = "#1B5E20"))

        assertEquals("GRASS", field.surface)
        assertEquals("CHECKERBOARD", field.mowingPattern)
    }
}
