package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.Offseason
import com.fcfb.arceus.service.fcfb.OffseasonService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class OffseasonControllerTest {
    private lateinit var mockMvc: MockMvc
    private val offseasonService: OffseasonService = mockk()
    private lateinit var offseasonController: OffseasonController

    @BeforeEach
    fun setup() {
        offseasonController = OffseasonController(offseasonService)
        mockMvc = MockMvcBuilders.standaloneSetup(offseasonController).build()
    }

    @Test
    fun `should get current offseason successfully`() {
        val offseason = Offseason(startDate = "01/01/2024 00:00:00", endDate = null)
        every { offseasonService.getCurrentOffseason() } returns offseason

        mockMvc.perform(get("/api/v1/arceus/offseason").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.startDate").value(offseason.startDate))
            .andExpect(jsonPath("$.endDate").isEmpty)
    }

    @Test
    fun `should return an empty body when there is no current offseason`() {
        every { offseasonService.getCurrentOffseason() } returns null

        mockMvc.perform(get("/api/v1/arceus/offseason").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().string(""))
    }
}
