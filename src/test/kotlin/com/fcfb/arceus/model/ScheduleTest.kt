package com.fcfb.arceus.model

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.team.Subdivision
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScheduleTest {
    @Test
    fun `Schedule should be properly annotated`() {
        val entityAnnotation = Schedule::class.annotations.find { it is javax.persistence.Entity }
        val tableAnnotation = Schedule::class.annotations.find { it is javax.persistence.Table }

        assertNotNull(entityAnnotation, "Schedule should be annotated with @Entity")
        assertNotNull(tableAnnotation, "Schedule should be annotated with @Table")
    }

    @Test
    fun `Schedule default constructor should initialize with default values`() {
        val schedule = Schedule()

        assertEquals(0, schedule.id)
        assertEquals(0, schedule.season)
        assertEquals(0, schedule.week)
        assertNull(schedule.tvChannel)
        assertEquals(false, schedule.started)
        assertEquals(false, schedule.finished)
    }

    @Test
    fun `Schedule properties should be mutable`() {
        val schedule = Schedule()

        schedule.id = 123
        schedule.season = 2024
        schedule.week = 5
        schedule.subdivision = Subdivision.FBS
        schedule.homeTeam = "Home Team"
        schedule.awayTeam = "Away Team"
        schedule.tvChannel = TVChannel.ESPN
        schedule.gameType = GameType.CONFERENCE_GAME
        schedule.started = true
        schedule.finished = false

        assertEquals(123, schedule.id)
        assertEquals(2024, schedule.season)
        assertEquals(5, schedule.week)
        assertEquals(Subdivision.FBS, schedule.subdivision)
        assertEquals("Home Team", schedule.homeTeam)
        assertEquals("Away Team", schedule.awayTeam)
        assertEquals(TVChannel.ESPN, schedule.tvChannel)
        assertEquals(GameType.CONFERENCE_GAME, schedule.gameType)
        assertEquals(true, schedule.started)
        assertEquals(false, schedule.finished)
    }

    @Test
    fun `Schedule should handle different Subdivision enum values`() {
        val schedule = Schedule()

        schedule.subdivision = Subdivision.FBS
        assertEquals(Subdivision.FBS, schedule.subdivision)

        schedule.subdivision = Subdivision.FCS
        assertEquals(Subdivision.FCS, schedule.subdivision)
    }

    @Test
    fun `Schedule should handle different GameType enum values`() {
        val schedule = Schedule()

        schedule.gameType = GameType.CONFERENCE_GAME
        assertEquals(GameType.CONFERENCE_GAME, schedule.gameType)

        schedule.gameType = GameType.PLAYOFFS
        assertEquals(GameType.PLAYOFFS, schedule.gameType)

        schedule.gameType = GameType.BOWL
        assertEquals(GameType.BOWL, schedule.gameType)
    }

    @Test
    fun `Schedule should handle different TVChannel enum values`() {
        val schedule = Schedule()

        schedule.tvChannel = TVChannel.ESPN
        assertEquals(TVChannel.ESPN, schedule.tvChannel)

        schedule.tvChannel = TVChannel.FOX
        assertEquals(TVChannel.FOX, schedule.tvChannel)

        schedule.tvChannel = TVChannel.CBS
        assertEquals(TVChannel.CBS, schedule.tvChannel)

        schedule.tvChannel = null
        assertNull(schedule.tvChannel)
    }

    @Test
    fun `Schedule should handle null TVChannel`() {
        val schedule = Schedule()

        schedule.tvChannel = null
        assertNull(schedule.tvChannel)
    }

    @Test
    fun `Schedule should handle boolean states correctly`() {
        val schedule = Schedule()

        schedule.started = true
        assertEquals(true, schedule.started)

        schedule.started = false
        assertEquals(false, schedule.started)

        schedule.started = null
        assertNull(schedule.started)

        schedule.finished = true
        assertEquals(true, schedule.finished)

        schedule.finished = false
        assertEquals(false, schedule.finished)

        schedule.finished = null
        assertNull(schedule.finished)
    }

    @Test
    fun `Schedule should be instantiable`() {
        val schedule = Schedule()
        assertNotNull(schedule, "Schedule should be instantiable")
    }

    @Test
    fun `Schedule should handle team names with special characters`() {
        val schedule = Schedule()

        schedule.homeTeam = "Team with Spaces & Special Characters!"
        schedule.awayTeam = "Another-Team_123"

        assertEquals("Team with Spaces & Special Characters!", schedule.homeTeam)
        assertEquals("Another-Team_123", schedule.awayTeam)
    }

    @Test
    fun `Schedule should handle long team names`() {
        val schedule = Schedule()

        val longTeamName = "A".repeat(100)
        schedule.homeTeam = longTeamName
        schedule.awayTeam = longTeamName

        assertEquals(longTeamName, schedule.homeTeam)
        assertEquals(longTeamName, schedule.awayTeam)
    }

    @Test
    fun `Schedule should handle different season and week values`() {
        val schedule = Schedule()

        schedule.season = 2023
        assertEquals(2023, schedule.season)

        schedule.season = 2024
        assertEquals(2024, schedule.season)

        schedule.season = 2025
        assertEquals(2025, schedule.season)

        schedule.week = 1
        assertEquals(1, schedule.week)

        schedule.week = 15
        assertEquals(15, schedule.week)

        schedule.week = 0
        assertEquals(0, schedule.week)
    }

    @Test
    fun `Schedule should handle game state combinations`() {
        val schedule = Schedule()

        schedule.started = false
        schedule.finished = false
        assertFalse(schedule.started!!)
        assertFalse(schedule.finished!!)

        schedule.started = true
        schedule.finished = false
        assertEquals(true, schedule.started)
        assertFalse(schedule.finished!!)

        schedule.started = true
        schedule.finished = true
        assertEquals(true, schedule.started)
        assertEquals(true, schedule.finished)
    }

    @Test
    fun `Schedule should handle complete game setup`() {
        val schedule =
            Schedule().apply {
                id = 1
                season = 2024
                week = 10
                subdivision = Subdivision.FBS
                homeTeam = "Alabama"
                awayTeam = "Auburn"
                tvChannel = TVChannel.CBS
                gameType = GameType.CONFERENCE_GAME
                started = false
                finished = false
            }

        assertEquals(1, schedule.id)
        assertEquals(2024, schedule.season)
        assertEquals(10, schedule.week)
        assertEquals(Subdivision.FBS, schedule.subdivision)
        assertEquals("Alabama", schedule.homeTeam)
        assertEquals("Auburn", schedule.awayTeam)
        assertEquals(TVChannel.CBS, schedule.tvChannel)
        assertEquals(GameType.CONFERENCE_GAME, schedule.gameType)
        assertEquals(false, schedule.started)
        assertEquals(false, schedule.finished)
    }
}
