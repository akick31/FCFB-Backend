package com.fcfb.arceus.controllers.history

import com.fcfb.arceus.dto.response.WeekHighlightsResponse
import com.fcfb.arceus.dto.response.WeekHistoryResponse
import com.fcfb.arceus.service.fcfb.history.WeekHistoryService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class WeekHistoryControllerTest {
    private val service: WeekHistoryService = mockk()
    private val mockMvc = MockMvcBuilders.standaloneSetup(WeekHistoryController(service)).build()

    @Test
    fun `week returns the service summary`() {
        every { service.week(11, 4) } returns
            WeekHistoryResponse(
                season = 11,
                week = 4,
                games = emptyList(),
                highlights = WeekHighlightsResponse(emptyList(), emptyList(), null, 7, 8, 9, null, emptyList(), null),
            )

        mockMvc.perform(get("/api/v1/arceus/history/week").param("season", "11").param("week", "4"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.week").value(4))
            .andExpect(jsonPath("$.highlights.closestGameId").value(7))
    }

    @Test
    fun `weeks returns the finished weeks`() {
        every { service.finishedWeeks(11) } returns listOf(1, 2)

        mockMvc.perform(get("/api/v1/arceus/history/weeks").param("season", "11"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[1]").value(2))
    }
}
