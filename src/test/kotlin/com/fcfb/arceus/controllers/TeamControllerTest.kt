package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.util.GlobalExceptionHandler
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.Optional

class TeamControllerTest {
    private lateinit var mockMvc: MockMvc
    private val teamService: TeamService = mockk()
    private lateinit var teamController: TeamController

    @BeforeEach
    fun setup() {
        teamController = TeamController(teamService)
        mockMvc =
            MockMvcBuilders.standaloneSetup(teamController)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    private fun sampleTeam(
        id: Int = 1,
        subdivision: Subdivision = Subdivision.FCS,
        offensivePlaybook: OffensivePlaybook = OffensivePlaybook.AIR_RAID,
        defensivePlaybook: DefensivePlaybook = DefensivePlaybook.FIVE_TWO,
        conference: Conference = Conference.COLONIAL,
    ): Team {
        return Team(
            logo = "logo$id.png",
            scorebugLogo = "scorebug$id.png",
            coachUsernames = mutableListOf("c$id"),
            coachNames = mutableListOf("Coach $id"),
            coachDiscordTags = mutableListOf("c$id#000"),
            coachDiscordIds = mutableListOf("id1"),
            name = "Team$id",
            shortName = "T$id",
            abbreviation = "T$id",
            primaryColor = "#111111",
            secondaryColor = "#222222",
            currentWins = 1,
            currentLosses = 1,
            subdivision = subdivision,
            offensivePlaybook = offensivePlaybook,
            defensivePlaybook = defensivePlaybook,
            conference = conference,
            coachesPollRanking = 5,
            playoffCommitteeRanking = 6,
            currentConferenceWins = 1,
            currentConferenceLosses = 0,
            overallWins = 10,
            overallLosses = 2,
            overallConferenceWins = 6,
            overallConferenceLosses = 1,
            conferenceChampionshipWins = 1,
            conferenceChampionshipLosses = 0,
            bowlWins = 0,
            bowlLosses = 0,
            playoffWins = 1,
            playoffLosses = 0,
            nationalChampionshipWins = 0,
            nationalChampionshipLosses = 0,
            isTaken = false,
            active = true,
            currentElo = 1500.0,
            overallElo = 1500.0,
        )
    }

    @Test
    fun `should get team by id`() {
        val team = sampleTeam()
        every { teamService.getTeamById(1) } returns Optional.ofNullable(team) as Optional<Team?>

        mockMvc.perform(get("/api/v1/arceus/team/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Team1"))
            .andExpect(jsonPath("$.conference").value("COLONIAL"))
    }

    @Test
    fun `should get all teams`() {
        val t1 =
            sampleTeam(
                1,
                subdivision = Subdivision.FCFB,
                conference = Conference.SEC,
                offensivePlaybook = OffensivePlaybook.AIR_RAID,
                defensivePlaybook = DefensivePlaybook.FOUR_THREE,
            )
        val t2 =
            sampleTeam(
                2,
                subdivision = Subdivision.FBS,
                conference = Conference.ACC,
                offensivePlaybook = OffensivePlaybook.PRO,
                defensivePlaybook = DefensivePlaybook.THREE_FOUR,
            )
        every { teamService.getAllTeams() } returns listOf(t1, t2)

        mockMvc.perform(get("/api/v1/arceus/team"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Team1"))
            .andExpect(jsonPath("$[1].name").value("Team2"))
    }

    @Test
    fun `should get team by name`() {
        val team = sampleTeam()
        every { teamService.getTeamByName("Team1") } returns team

        mockMvc.perform(get("/api/v1/arceus/team/name").param("name", "Team1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.abbreviation").value("T1"))
    }

    @Test
    fun `should create team`() {
        val team = sampleTeam()
        every { teamService.createTeam(any()) } returns team

        val body =
            """
            {
              "logo":"logo1.png","scorebugLogo":"scorebug1.png",
              "coachUsernames":["c1"],"coachNames":["Coach 1"],
              "coachDiscordTags":["c1#000"],"coachDiscordIds":["id1"],
              "name":"Team1","shortName":"T1","abbreviation":"T1",
              "primaryColor":"#111111","secondaryColor":"#222222",
              "currentWins":1,"currentLosses":1,
              "subdivision":"FCS","offensivePlaybook":"AIR_RAID",
              "defensivePlaybook":"FIVE_TWO","conference":"COLONIAL",
              "coachesPollRanking":5,"playoffCommitteeRanking":6,
              "currentConferenceWins":1,"currentConferenceLosses":0,
              "overallWins":10,"overallLosses":2,
              "overallConferenceWins":6,"overallConferenceLosses":1,
              "conferenceChampionshipWins":1,"conferenceChampionshipLosses":0,
              "bowlWins":0,"bowlLosses":0,"playoffWins":1,"playoffLosses":0,
              "nationalChampionshipWins":0,"nationalChampionshipLosses":0,
              "isTaken":false,"active":true
            }
            """.trimIndent()

        mockMvc.perform(
            post("/api/v1/arceus/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Team1"))
    }

    @Test
    fun `should update team`() {
        val team = sampleTeam()
        every { teamService.updateTeam(any()) } returns team

        val body =
            """
            { "name":"Team1","abbreviation":"T1", "isTaken":false, "active":true, "logo":"logo1.png","scorebugLogo":"scorebug1.png",
              "coachUsernames":["c1"],"coachNames":["Coach 1"],
              "coachDiscordTags":["c1#000"],"coachDiscordIds":["id1"],
              "shortName":"T1","primaryColor":"#111111","secondaryColor":"#222222",
              "currentWins":1,"currentLosses":1,
              "subdivision":"FCS","offensivePlaybook":"AIR_RAID",
              "defensivePlaybook":"FIVE_TWO","conference":"COLONIAL",
              "coachesPollRanking":5,"playoffCommitteeRanking":6,
              "currentConferenceWins":1,"currentConferenceLosses":0,
              "overallWins":10,"overallLosses":2,
              "overallConferenceWins":6,"overallConferenceLosses":1,
              "conferenceChampionshipWins":1,"conferenceChampionshipLosses":0,
              "bowlWins":0,"bowlLosses":0,"playoffWins":1,"playoffLosses":0,
              "nationalChampionshipWins":0,"nationalChampionshipLosses":0
            }
            """.trimIndent()

        mockMvc.perform(
            put("/api/v1/arceus/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.abbreviation").value("T1"))
    }

    @Test
    fun `should hire coach`() {
        coEvery {
            teamService.hireCoach(any(), any(), any(), any())
        } returns sampleTeam()

        mockMvc.perform(
            post("/api/v1/arceus/team/hire")
                .param("team", "Team1")
                .param("discordId", "id1")
                .param("coachPosition", "HEAD_COACH")
                .param("processedBy", "admin"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should hire interim coach`() {
        coEvery {
            teamService.hireInterimCoach(any(), any(), any())
        } returns sampleTeam()

        mockMvc.perform(
            post("/api/v1/arceus/team/hire/interim")
                .param("team", "Team1")
                .param("discordId", "id1")
                .param("processedBy", "admin"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should fire coach`(): Unit =
        runBlocking {
            every {
                teamService.fireCoach(any(), any())
            } returns sampleTeam()

            mockMvc.perform(
                post("/api/v1/arceus/team/fire")
                    .param("team", "Team1")
                    .param("processedBy", "admin"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isTaken", not(true)))
        }

    @Test
    fun `should get open teams`() {
        val openList = listOf("A", "B", "C")
        every { teamService.getOpenTeams() } returns openList

        mockMvc.perform(get("/api/v1/arceus/team/open"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("A"))
            .andExpect(jsonPath("$[2]").value("C"))
    }

    @Test
    fun `should delete team`() {
        every { teamService.deleteTeam(1) } returns OK

        mockMvc.perform(delete("/api/v1/arceus/team/1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should error when get team by id missing`() {
        every { teamService.getTeamById(1) } throws RuntimeException("Team not found")

        mockMvc.perform(get("/api/v1/arceus/team/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("Team not found"))
    }
}
