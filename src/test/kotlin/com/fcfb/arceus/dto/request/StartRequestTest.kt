package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.system.Platform.DISCORD
import com.fcfb.arceus.enums.team.Subdivision
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StartRequestTest {
    @Test
    fun `StartRequest should be a data class`() {
        val startRequest = createTestStartRequest()

        // Data classes automatically implement equals, hashCode, toString, and copy
        assertNotNull(startRequest.toString())
        assertTrue(startRequest.toString().contains("StartRequest"))
    }

    @Test
    fun `StartRequest should create instance with all properties`() {
        val startRequest =
            StartRequest(
                homePlatform = DISCORD,
                awayPlatform = DISCORD,
                subdivision = Subdivision.FBS,
                homeTeam = "Alabama",
                awayTeam = "Georgia",
                tvChannel = TVChannel.ESPN,
                gameType = GameType.CONFERENCE_GAME,
            )

        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, startRequest.homePlatform)
        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, startRequest.awayPlatform)
        assertEquals(Subdivision.FBS, startRequest.subdivision)
        assertEquals("Alabama", startRequest.homeTeam)
        assertEquals("Georgia", startRequest.awayTeam)
        assertEquals(TVChannel.ESPN, startRequest.tvChannel)
        assertEquals(GameType.CONFERENCE_GAME, startRequest.gameType)
    }

    @Test
    fun `StartRequest should handle null TV channel`() {
        val startRequest =
            StartRequest(
                homePlatform = DISCORD,
                awayPlatform = DISCORD,
                subdivision = Subdivision.FCS,
                homeTeam = "Alabama",
                awayTeam = "Georgia",
                tvChannel = null,
                gameType = GameType.OUT_OF_CONFERENCE,
            )

        assertNull(startRequest.tvChannel)
    }

    @Test
    fun `StartRequest should have proper JsonProperty annotations`() {
        val startRequestClass = StartRequest::class.java

        // Check homePlatform annotation
        val homePlatformField = startRequestClass.declaredConstructors[0].parameters.find { it.name == "homePlatform" }
        val homePlatformAnnotation = homePlatformField?.getAnnotation(JsonProperty::class.java)
        assertEquals("homePlatform", homePlatformAnnotation?.value)

        // Check awayPlatform annotation
        val awayPlatformField = startRequestClass.declaredConstructors[0].parameters.find { it.name == "awayPlatform" }
        val awayPlatformAnnotation = awayPlatformField?.getAnnotation(JsonProperty::class.java)
        assertEquals("awayPlatform", awayPlatformAnnotation?.value)

        // Check subdivision annotation
        val subdivisionField = startRequestClass.declaredConstructors[0].parameters.find { it.name == "subdivision" }
        val subdivisionAnnotation = subdivisionField?.getAnnotation(JsonProperty::class.java)
        assertEquals("subdivision", subdivisionAnnotation?.value)

        // Check homeTeam annotation
        val homeTeamField = startRequestClass.declaredConstructors[0].parameters.find { it.name == "homeTeam" }
        val homeTeamAnnotation = homeTeamField?.getAnnotation(JsonProperty::class.java)
        assertEquals("homeTeam", homeTeamAnnotation?.value)

        // Check awayTeam annotation
        val awayTeamField = startRequestClass.declaredConstructors[0].parameters.find { it.name == "awayTeam" }
        val awayTeamAnnotation = awayTeamField?.getAnnotation(JsonProperty::class.java)
        assertEquals("awayTeam", awayTeamAnnotation?.value)

        // Check tvChannel annotation
        val tvChannelField = startRequestClass.declaredConstructors[0].parameters.find { it.name == "tvChannel" }
        val tvChannelAnnotation = tvChannelField?.getAnnotation(JsonProperty::class.java)
        assertEquals("tvChannel", tvChannelAnnotation?.value)

        // Check gameType annotation
        val gameTypeField = startRequestClass.declaredConstructors[0].parameters.find { it.name == "gameType" }
        val gameTypeAnnotation = gameTypeField?.getAnnotation(JsonProperty::class.java)
        assertEquals("gameType", gameTypeAnnotation?.value)
    }

    @Test
    fun `StartRequest should handle different Platform enum values`() {
        val discordRequest =
            createTestStartRequest().copy(
                homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD,
                awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD,
            )

        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, discordRequest.homePlatform)
        assertEquals(com.fcfb.arceus.enums.system.Platform.DISCORD, discordRequest.awayPlatform)
    }

    @Test
    fun `StartRequest should handle different Subdivision enum values`() {
        val fbsRequest = createTestStartRequest().copy(subdivision = Subdivision.FBS)
        val fcsRequest = createTestStartRequest().copy(subdivision = Subdivision.FCS)

        assertEquals(Subdivision.FBS, fbsRequest.subdivision)
        assertEquals(Subdivision.FCS, fcsRequest.subdivision)
    }

    @Test
    fun `StartRequest should handle different GameType enum values`() {
        val gameTypes =
            listOf(
                GameType.OUT_OF_CONFERENCE,
                GameType.CONFERENCE_GAME,
                GameType.CONFERENCE_CHAMPIONSHIP,
                GameType.PLAYOFFS,
                GameType.NATIONAL_CHAMPIONSHIP,
                GameType.BOWL,
                GameType.SCRIMMAGE,
            )

        gameTypes.forEach { gameType ->
            val request = createTestStartRequest().copy(gameType = gameType)
            assertEquals(gameType, request.gameType)
        }
    }

    @Test
    fun `StartRequest should handle different TVChannel enum values`() {
        val tvChannels =
            listOf(
                TVChannel.ESPN,
                TVChannel.FOX,
                TVChannel.CBS,
                TVChannel.NBC,
                TVChannel.ABC,
            )

        tvChannels.forEach { tvChannel ->
            val request = createTestStartRequest().copy(tvChannel = tvChannel)
            assertEquals(tvChannel, request.tvChannel)
        }
    }

    @Test
    fun `StartRequest should handle team names with special characters`() {
        val startRequest =
            StartRequest(
                homePlatform = DISCORD,
                awayPlatform = DISCORD,
                subdivision = Subdivision.FBS,
                homeTeam = "Texas A&M",
                awayTeam = "Ole Miss",
                tvChannel = TVChannel.ESPN,
                gameType = GameType.CONFERENCE_GAME,
            )

        assertEquals("Texas A&M", startRequest.homeTeam)
        assertEquals("Ole Miss", startRequest.awayTeam)
    }

    @Test
    fun `StartRequest should handle long team names`() {
        val longTeamName = "A".repeat(100)
        val startRequest =
            StartRequest(
                homePlatform = DISCORD,
                awayPlatform = DISCORD,
                subdivision = Subdivision.FBS,
                homeTeam = longTeamName,
                awayTeam = longTeamName,
                tvChannel = TVChannel.ESPN,
                gameType = GameType.CONFERENCE_GAME,
            )

        assertEquals(longTeamName, startRequest.homeTeam)
        assertEquals(longTeamName, startRequest.awayTeam)
    }

    @Test
    fun `StartRequest data class should support copy functionality`() {
        val original = createTestStartRequest()
        val copied =
            original.copy(
                homeTeam = "Michigan",
                awayTeam = "Ohio State",
                tvChannel = TVChannel.FOX,
            )

        assertEquals("Michigan", copied.homeTeam)
        assertEquals("Ohio State", copied.awayTeam)
        assertEquals(TVChannel.FOX, copied.tvChannel)
        assertEquals(original.homePlatform, copied.homePlatform)
        assertEquals(original.awayPlatform, copied.awayPlatform)
        assertEquals(original.subdivision, copied.subdivision)
        assertEquals(original.gameType, copied.gameType)
    }

    @Test
    fun `StartRequest data class should support equality comparison`() {
        val startRequest1 = createTestStartRequest()
        val startRequest2 = createTestStartRequest()
        val startRequest3 = startRequest1.copy(homeTeam = "Different Team")

        assertEquals(startRequest1, startRequest2)
        assertTrue(startRequest1 != startRequest3)
    }

    @Test
    fun `StartRequest should be immutable`() {
        val startRequest = createTestStartRequest()

        // All properties should be val (immutable)
        // This is enforced by the data class declaration
        assertNotNull(startRequest.homePlatform)
        assertNotNull(startRequest.awayPlatform)
        assertNotNull(startRequest.subdivision)
        assertNotNull(startRequest.homeTeam)
        assertNotNull(startRequest.awayTeam)
        assertNotNull(startRequest.gameType)
        // tvChannel can be null
    }

    @Test
    fun `StartRequest should handle championship games`() {
        val championshipRequest =
            StartRequest(
                homePlatform = DISCORD,
                awayPlatform = DISCORD,
                subdivision = Subdivision.FBS,
                homeTeam = "Alabama",
                awayTeam = "Georgia",
                tvChannel = TVChannel.ESPN,
                gameType = GameType.NATIONAL_CHAMPIONSHIP,
            )

        assertEquals(GameType.NATIONAL_CHAMPIONSHIP, championshipRequest.gameType)
        assertEquals(TVChannel.ESPN, championshipRequest.tvChannel)
        assertEquals(Subdivision.FBS, championshipRequest.subdivision)
    }

    @Test
    fun `StartRequest should handle playoff games`() {
        val playoffRequest =
            StartRequest(
                homePlatform = DISCORD,
                awayPlatform = DISCORD,
                subdivision = Subdivision.FBS,
                homeTeam = "Michigan",
                awayTeam = "Washington",
                tvChannel = TVChannel.ESPN,
                gameType = GameType.PLAYOFFS,
            )

        assertEquals(GameType.PLAYOFFS, playoffRequest.gameType)
        assertEquals(TVChannel.ESPN, playoffRequest.tvChannel)
    }

    @Test
    fun `StartRequest should handle scrimmage games`() {
        val scrimmageRequest =
            StartRequest(
                homePlatform = DISCORD,
                awayPlatform = DISCORD,
                subdivision = Subdivision.FCS,
                homeTeam = "Team A",
                awayTeam = "Team B",
                tvChannel = null,
                gameType = GameType.SCRIMMAGE,
            )

        assertEquals(GameType.SCRIMMAGE, scrimmageRequest.gameType)
        assertNull(scrimmageRequest.tvChannel)
        assertEquals(Subdivision.FCS, scrimmageRequest.subdivision)
    }

    private fun createTestStartRequest(): StartRequest {
        return StartRequest(
            homePlatform = DISCORD,
            awayPlatform = DISCORD,
            subdivision = Subdivision.FBS,
            homeTeam = "Alabama",
            awayTeam = "Georgia",
            tvChannel = TVChannel.ESPN,
            gameType = GameType.CONFERENCE_GAME,
        )
    }
}
