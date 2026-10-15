package com.fcfb.arceus.service.fcfb.team

import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.model.log.CoachTransactionLog
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.service.fcfb.account.NewSignupService
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.log.CoachTransactionLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TeamCoachServiceTest {
    private val teamService: TeamService = mockk()
    private val userService: UserService = mockk()
    private val coachTransactionLogService: CoachTransactionLogService = mockk()
    private val newSignupService: NewSignupService = mockk()
    private lateinit var teamCoachService: TeamCoachService

    private val discordId = "111222333"

    private fun vacantTeam(name: String) =
        Team().apply {
            this.name = name
            coachUsernames = null
            coachNames = null
            coachDiscordTags = null
            coachDiscordIds = null
        }

    private fun userDto(team: String?) =
        UserDTO(
            id = 1L,
            username = "cyclone_puffin",
            coachName = "Dick Nutter VI",
            discordTag = "cyclone_puffin",
            discordId = discordId,
            position = CoachPosition.HEAD_COACH,
            role = UserRole.USER,
            team = team,
            delayOfGameInstances = 0,
            wins = 0,
            losses = 0,
            winPercentage = 0.0,
            conferenceWins = 0,
            conferenceLosses = 0,
            conferenceChampionshipWins = 0,
            conferenceChampionshipLosses = 0,
            bowlWins = 0,
            bowlLosses = 0,
            playoffWins = 0,
            playoffLosses = 0,
            nationalChampionshipWins = 0,
            nationalChampionshipLosses = 0,
            offensivePlaybook = OffensivePlaybook.AIR_RAID,
            defensivePlaybook = DefensivePlaybook.FOUR_THREE,
            averageResponseTime = 0.0,
            delayOfGameWarningOptOut = false,
            scoutingReportHiddenColumns = null,
        )

    @BeforeEach
    fun setup() {
        teamCoachService =
            TeamCoachService(
                teamService,
                userService,
                coachTransactionLogService,
                newSignupService,
            )
        every { newSignupService.getNewSignupByDiscordId(any()) } returns null
        every { userService.updateUser(any()) } answers { firstArg() }
        every { coachTransactionLogService.logCoachTransaction(any()) } answers { firstArg() }
        every { teamService.saveTeam(any()) } answers { firstArg() }
    }

    @Test
    fun `hireCoach onto a vacant team logs coach_discord_ids`() {
        val destination = vacantTeam("Arkansas")
        every { teamService.getTeamByName("Arkansas") } returns destination
        every { userService.getUserDTOByDiscordId(discordId) } returns userDto(team = null)

        val logged = slot<CoachTransactionLog>()
        every { coachTransactionLogService.logCoachTransaction(capture(logged)) } answers { firstArg() }

        runBlocking { teamCoachService.hireCoach("Arkansas", discordId, CoachPosition.HEAD_COACH, "admin") }

        assertEquals(mutableListOf(discordId), logged.captured.coachDiscordIds)
        assertEquals(mutableListOf("cyclone_puffin"), logged.captured.coach)
    }

    @Test
    fun `hireCoach fires the incoming coach from their previous team first`() {
        val previousTeam =
            Team().apply {
                name = "Richmond"
                coachUsernames = mutableListOf("cyclone_puffin")
                coachNames = mutableListOf("Dick Nutter VI")
                coachDiscordTags = mutableListOf("cyclone_puffin")
                coachDiscordIds = mutableListOf(discordId)
            }
        val destination = vacantTeam("Arkansas")
        every { teamService.getTeamByName("Richmond") } returns previousTeam
        every { teamService.getTeamByName("Arkansas") } returns destination
        every { userService.getUserDTOByDiscordId(discordId) } returns userDto(team = "Richmond")

        val logs = mutableListOf<CoachTransactionLog>()
        every { coachTransactionLogService.logCoachTransaction(capture(logs)) } answers { firstArg() }

        runBlocking { teamCoachService.hireCoach("Arkansas", discordId, CoachPosition.HEAD_COACH, "admin") }

        val firedLog = logs.first { it.transaction == TransactionType.FIRED }
        val hiredLog = logs.first { it.transaction == TransactionType.HIRED }
        assertEquals("Richmond", firedLog.team)
        assertEquals(mutableListOf(discordId), firedLog.coachDiscordIds)
        assertEquals("Arkansas", hiredLog.team)
        assertEquals(mutableListOf(discordId), hiredLog.coachDiscordIds)
    }

    @Test
    fun `fireSingleCoach logs coach_discord_ids for only the fired coach`() {
        val team =
            Team().apply {
                name = "Arkansas"
                coachUsernames = mutableListOf("cyclone_puffin", "oc_guy")
                coachNames = mutableListOf("Dick Nutter VI", "OC Guy")
                coachDiscordTags = mutableListOf("cyclone_puffin", "oc_guy")
                coachDiscordIds = mutableListOf(discordId, "999888777")
            }
        every { teamService.getTeamByName("Arkansas") } returns team
        every { userService.getUserDTOByDiscordId(discordId) } returns userDto(team = "Arkansas")

        val logged = slot<CoachTransactionLog>()
        every { coachTransactionLogService.logCoachTransaction(capture(logged)) } answers { firstArg() }

        teamCoachService.fireSingleCoach("Arkansas", discordId, CoachPosition.HEAD_COACH, "admin")

        assertEquals(mutableListOf(discordId), logged.captured.coachDiscordIds)
        assertEquals(mutableListOf("999888777"), team.coachDiscordIds)
    }
}
