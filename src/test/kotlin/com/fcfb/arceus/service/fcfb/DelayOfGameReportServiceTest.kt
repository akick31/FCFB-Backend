package com.fcfb.arceus.service.fcfb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.PlayRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DelayOfGameReportServiceTest {
    private val playRepository: PlayRepository = mockk()
    private val userService: UserService = mockk()
    private lateinit var delayOfGameReportService: DelayOfGameReportService

    private fun user(
        discordId: String,
        username: String,
        team: String?,
    ) = User().apply {
        this.discordId = discordId
        this.username = username
        this.discordTag = "$username#0001"
        this.team = team
    }

    @BeforeEach
    fun setup() {
        delayOfGameReportService = DelayOfGameReportService(playRepository, userService, ObjectMapper())
        every { userService.findUserByDiscordId("111") } returns user("111", "cyclone_puffin", "Ohio State")
        every { userService.findUserByDiscordId("222") } returns user("222", "gale_otter", "Ohio State")
        every { userService.findUserByDiscordId("333") } returns user("333", "dune_marlin", "Michigan")
    }

    @Test
    fun `test each coach on the penalized team gets the delay of game count`() {
        every { playRepository.getDelayOfGameCoachCountsByWeek(11, 2) } returns
            listOf(arrayOf("[\"111\",\"222\"]", "Ohio State", 3))

        val report = delayOfGameReportService.getUserDelayOfGameInstances(11, 2)

        assertEquals(2, report.size)
        assertTrue(report.all { it.delayOfGameInstances == 3 })
        assertEquals(setOf("cyclone_puffin", "gale_otter"), report.map { it.username }.toSet())
    }

    @Test
    fun `test counts are summed per coach and sorted descending`() {
        every { playRepository.getDelayOfGameCoachCountsBySeason(11) } returns
            listOf(
                arrayOf("[\"111\"]", "Ohio State", 1),
                arrayOf("[\"111\",\"222\"]", "Ohio State", 2),
                arrayOf("[\"333\"]", "Michigan", 4),
            )

        val report = delayOfGameReportService.getUserDelayOfGameInstances(11, null)

        assertEquals(listOf("dune_marlin", "cyclone_puffin", "gale_otter"), report.map { it.username })
        assertEquals(listOf(4, 3, 2), report.map { it.delayOfGameInstances })
    }

    @Test
    fun `test coaches without a user record are left out`() {
        every { userService.findUserByDiscordId("999") } returns null
        every { playRepository.getDelayOfGameCoachCountsByWeek(11, 2) } returns
            listOf(arrayOf("[\"999\",\"111\"]", "Ohio State", 1))

        val report = delayOfGameReportService.getUserDelayOfGameInstances(11, 2)

        assertEquals(listOf("cyclone_puffin"), report.map { it.username })
    }
}
