package com.fcfb.arceus.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeasonTest {
    @Test
    fun `Season should be properly annotated`() {
        val entityAnnotation = Season::class.annotations.find { it is javax.persistence.Entity }
        val tableAnnotation = Season::class.annotations.find { it is javax.persistence.Table }

        assertNotNull(entityAnnotation, "Season should be annotated with @Entity")
        assertNotNull(tableAnnotation, "Season should be annotated with @Table")
    }

    @Test
    fun `Season default constructor should initialize with default values`() {
        val season = Season()

        assertEquals(10, season.seasonNumber)
        assertNull(season.startDate)
        assertNull(season.endDate)
        assertNull(season.nationalChampionshipWinningTeam)
        assertNull(season.nationalChampionshipLosingTeam)
        assertNull(season.nationalChampionshipWinningCoach)
        assertNull(season.nationalChampionshipLosingCoach)
        assertEquals(1, season.currentWeek)
        assertFalse(season.currentSeason)
    }

    @Test
    fun `Season parameterized constructor should set values correctly`() {
        val season =
            Season(
                seasonNumber = 15,
                startDate = "2024-01-01",
                endDate = "2024-12-31",
                nationalChampionshipWinningTeam = "Alabama",
                nationalChampionshipLosingTeam = "Georgia",
                nationalChampionshipWinningCoach = "Nick Saban",
                nationalChampionshipLosingCoach = "Kirby Smart",
                currentWeek = 12,
                currentSeason = true,
            )

        assertEquals(15, season.seasonNumber)
        assertEquals("2024-01-01", season.startDate)
        assertEquals("2024-12-31", season.endDate)
        assertEquals("Alabama", season.nationalChampionshipWinningTeam)
        assertEquals("Georgia", season.nationalChampionshipLosingTeam)
        assertEquals("Nick Saban", season.nationalChampionshipWinningCoach)
        assertEquals("Kirby Smart", season.nationalChampionshipLosingCoach)
        assertEquals(12, season.currentWeek)
        assertTrue(season.currentSeason)
    }

    @Test
    fun `Season parameterized constructor should handle null values`() {
        val season =
            Season(
                seasonNumber = 20,
                startDate = null,
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = 1,
                currentSeason = false,
            )

        assertEquals(20, season.seasonNumber)
        assertNull(season.startDate)
        assertNull(season.endDate)
        assertNull(season.nationalChampionshipWinningTeam)
        assertNull(season.nationalChampionshipLosingTeam)
        assertNull(season.nationalChampionshipWinningCoach)
        assertNull(season.nationalChampionshipLosingCoach)
        assertEquals(1, season.currentWeek)
        assertFalse(season.currentSeason)
    }

    @Test
    fun `Season properties should be mutable`() {
        val season = Season()

        season.seasonNumber = 25
        season.startDate = "2025-08-01"
        season.endDate = "2026-01-15"
        season.nationalChampionshipWinningTeam = "Texas"
        season.nationalChampionshipLosingTeam = "Michigan"
        season.nationalChampionshipWinningCoach = "Steve Sarkisian"
        season.nationalChampionshipLosingCoach = "Jim Harbaugh"
        season.currentWeek = 15
        season.currentSeason = true

        assertEquals(25, season.seasonNumber)
        assertEquals("2025-08-01", season.startDate)
        assertEquals("2026-01-15", season.endDate)
        assertEquals("Texas", season.nationalChampionshipWinningTeam)
        assertEquals("Michigan", season.nationalChampionshipLosingTeam)
        assertEquals("Steve Sarkisian", season.nationalChampionshipWinningCoach)
        assertEquals("Jim Harbaugh", season.nationalChampionshipLosingCoach)
        assertEquals(15, season.currentWeek)
        assertTrue(season.currentSeason)
    }

    @Test
    fun `Season should handle different season numbers`() {
        val season = Season()

        season.seasonNumber = 1
        assertEquals(1, season.seasonNumber)

        season.seasonNumber = 100
        assertEquals(100, season.seasonNumber)

        season.seasonNumber = 0
        assertEquals(0, season.seasonNumber)
    }

    @Test
    fun `Season should handle different week numbers`() {
        val season = Season()

        season.currentWeek = 1
        assertEquals(1, season.currentWeek)

        season.currentWeek = 15
        assertEquals(15, season.currentWeek)

        season.currentWeek = 0
        assertEquals(0, season.currentWeek)
    }

    @Test
    fun `Season should handle currentSeason boolean states`() {
        val season = Season()

        season.currentSeason = true
        assertTrue(season.currentSeason)

        season.currentSeason = false
        assertFalse(season.currentSeason)
    }

    @Test
    fun `Season should handle date strings correctly`() {
        val season = Season()

        season.startDate = "2024-08-15"
        season.endDate = "2025-01-20"

        assertEquals("2024-08-15", season.startDate)
        assertEquals("2025-01-20", season.endDate)

        season.startDate = "August 15, 2024"
        season.endDate = "January 20, 2025"

        assertEquals("August 15, 2024", season.startDate)
        assertEquals("January 20, 2025", season.endDate)
    }

    @Test
    fun `Season should handle team names with special characters`() {
        val season = Season()

        season.nationalChampionshipWinningTeam = "Texas A&M"
        season.nationalChampionshipLosingTeam = "Ole Miss"

        assertEquals("Texas A&M", season.nationalChampionshipWinningTeam)
        assertEquals("Ole Miss", season.nationalChampionshipLosingTeam)
    }

    @Test
    fun `Season should handle coach names with special characters`() {
        val season = Season()

        season.nationalChampionshipWinningCoach = "Dabo Swinney"
        season.nationalChampionshipLosingCoach = "P.J. Fleck"

        assertEquals("Dabo Swinney", season.nationalChampionshipWinningCoach)
        assertEquals("P.J. Fleck", season.nationalChampionshipLosingCoach)
    }

    @Test
    fun `Season should handle long team and coach names`() {
        val season = Season()

        val longTeamName = "A".repeat(100)
        val longCoachName = "B".repeat(100)

        season.nationalChampionshipWinningTeam = longTeamName
        season.nationalChampionshipWinningCoach = longCoachName

        assertEquals(longTeamName, season.nationalChampionshipWinningTeam)
        assertEquals(longCoachName, season.nationalChampionshipWinningCoach)
    }

    @Test
    fun `Season should be instantiable with both constructors`() {
        val season1 = Season()
        val season2 =
            Season(
                15, "2024-01-01", "2024-12-31", "Team1", "Team2", "Coach1", "Coach2", 10, true,
            )

        assertNotNull(season1)
        assertNotNull(season2)
    }

    @Test
    fun `Season should handle complete season setup`() {
        val season =
            Season().apply {
                seasonNumber = 12
                startDate = "2024-08-26"
                endDate = "2025-01-13"
                nationalChampionshipWinningTeam = "Georgia"
                nationalChampionshipLosingTeam = "Alabama"
                nationalChampionshipWinningCoach = "Kirby Smart"
                nationalChampionshipLosingCoach = "Nick Saban"
                currentWeek = 15
                currentSeason = false
            }

        assertEquals(12, season.seasonNumber)
        assertEquals("2024-08-26", season.startDate)
        assertEquals("2025-01-13", season.endDate)
        assertEquals("Georgia", season.nationalChampionshipWinningTeam)
        assertEquals("Alabama", season.nationalChampionshipLosingTeam)
        assertEquals("Kirby Smart", season.nationalChampionshipWinningCoach)
        assertEquals("Nick Saban", season.nationalChampionshipLosingCoach)
        assertEquals(15, season.currentWeek)
        assertFalse(season.currentSeason)
    }

    @Test
    fun `Season should handle championship data updates`() {
        val season = Season()

        assertNull(season.nationalChampionshipWinningTeam)
        assertNull(season.nationalChampionshipLosingTeam)

        season.nationalChampionshipWinningTeam = "Michigan"
        season.nationalChampionshipLosingTeam = "Washington"
        season.nationalChampionshipWinningCoach = "Jim Harbaugh"
        season.nationalChampionshipLosingCoach = "Kalen DeBoer"

        assertEquals("Michigan", season.nationalChampionshipWinningTeam)
        assertEquals("Washington", season.nationalChampionshipLosingTeam)
        assertEquals("Jim Harbaugh", season.nationalChampionshipWinningCoach)
        assertEquals("Kalen DeBoer", season.nationalChampionshipLosingCoach)

        season.nationalChampionshipWinningTeam = null
        season.nationalChampionshipLosingTeam = null
        season.nationalChampionshipWinningCoach = null
        season.nationalChampionshipLosingCoach = null

        assertNull(season.nationalChampionshipWinningTeam)
        assertNull(season.nationalChampionshipLosingTeam)
        assertNull(season.nationalChampionshipWinningCoach)
        assertNull(season.nationalChampionshipLosingCoach)
    }
}
