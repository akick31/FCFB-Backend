package com.fcfb.arceus.repositories.season

import com.fcfb.arceus.model.season.Season
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeasonRepositoryTest {
    private lateinit var seasonRepository: SeasonRepository

    @BeforeEach
    fun setUp() {
        seasonRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and findById`() {
        val season =
            createTestSeason(
                seasonNumber = 2024,
                currentSeason = true,
            )

        every { seasonRepository.save(any()) } returns season
        every { seasonRepository.findById(2024) } returns java.util.Optional.of(season)

        val savedSeason = seasonRepository.save(season)
        val foundSeason = seasonRepository.findById(savedSeason.seasonNumber).get()

        assertNotNull(foundSeason)
        assertEquals(2024, foundSeason.seasonNumber)
        assertTrue(foundSeason.currentSeason)
    }

    @Test
    fun `test getCurrentSeason`() {
        val currentSeason =
            createTestSeason(
                seasonNumber = 2024,
                currentSeason = true,
            )

        every { seasonRepository.getCurrentSeason() } returns currentSeason

        val foundCurrentSeason = seasonRepository.getCurrentSeason()

        assertNotNull(foundCurrentSeason)
        assertEquals(2024, foundCurrentSeason!!.seasonNumber)
        assertTrue(foundCurrentSeason.currentSeason)
    }

    @Test
    fun `test getCurrentSeason returns null when no current season`() {
        every { seasonRepository.getCurrentSeason() } returns null

        val foundCurrentSeason = seasonRepository.getCurrentSeason()

        assertNull(foundCurrentSeason)
    }

    @Test
    fun `test getPreviousSeason`() {
        val previousSeason =
            createTestSeason(
                seasonNumber = 2023,
                currentSeason = false,
            )

        every { seasonRepository.getPreviousSeason() } returns previousSeason

        val foundPreviousSeason = seasonRepository.getPreviousSeason()

        assertNotNull(foundPreviousSeason)
        assertEquals(2023, foundPreviousSeason!!.seasonNumber)
        assertFalse(foundPreviousSeason.currentSeason)
    }

    @Test
    fun `test getPreviousSeason returns null when no previous seasons`() {
        every { seasonRepository.getPreviousSeason() } returns null

        val foundPreviousSeason = seasonRepository.getPreviousSeason()

        assertNull(foundPreviousSeason)
    }

    @Test
    fun `test findAll`() {
        val season1 = createTestSeason(seasonNumber = 2024, currentSeason = true)
        val season2 = createTestSeason(seasonNumber = 2023, currentSeason = false)
        val seasons = listOf(season1, season2)

        every { seasonRepository.findAll() } returns seasons

        val foundSeasons = seasonRepository.findAll()

        assertEquals(2, foundSeasons.count())
        assertTrue(foundSeasons.any { it.seasonNumber == 2024 })
        assertTrue(foundSeasons.any { it.seasonNumber == 2023 })
    }

    @Test
    fun `test count`() {
        every { seasonRepository.count() } returns 5L

        val count = seasonRepository.count()

        assertEquals(5L, count)
    }

    @Test
    fun `test existsById`() {
        every { seasonRepository.existsById(2024) } returns true
        every { seasonRepository.existsById(2025) } returns false

        assertTrue(seasonRepository.existsById(2024))
        assertFalse(seasonRepository.existsById(2025))
    }

    @Test
    fun `test findAllById`() {
        val season1 = createTestSeason(seasonNumber = 2024, currentSeason = true)
        val season2 = createTestSeason(seasonNumber = 2023, currentSeason = false)
        val seasons = listOf(season1, season2)
        val ids = listOf(2024, 2023)

        every { seasonRepository.findAllById(ids) } returns seasons

        val foundSeasons = seasonRepository.findAllById(ids)

        assertEquals(2, foundSeasons.count())
        assertTrue(foundSeasons.any { it.seasonNumber == 2024 })
        assertTrue(foundSeasons.any { it.seasonNumber == 2023 })
    }

    @Test
    fun `test delete`() {
        val season = createTestSeason(seasonNumber = 2024, currentSeason = true)
        every { seasonRepository.delete(season) } returns Unit

        seasonRepository.delete(season)

        verify { seasonRepository.delete(season) }
    }

    @Test
    fun `test deleteById`() {
        every { seasonRepository.deleteById(2024) } returns Unit

        seasonRepository.deleteById(2024)

        verify { seasonRepository.deleteById(2024) }
    }

    @Test
    fun `test deleteAll`() {
        every { seasonRepository.deleteAll() } returns Unit

        seasonRepository.deleteAll()

        verify { seasonRepository.deleteAll() }
    }

    @Test
    fun `test saveAll`() {
        val season1 = createTestSeason(seasonNumber = 2024, currentSeason = true)
        val season2 = createTestSeason(seasonNumber = 2023, currentSeason = false)
        val seasons = listOf(season1, season2)

        every { seasonRepository.saveAll(seasons) } returns seasons

        val savedSeasons = seasonRepository.saveAll(seasons)

        assertEquals(2, savedSeasons.count())
        assertTrue(savedSeasons.any { it.seasonNumber == 2024 })
        assertTrue(savedSeasons.any { it.seasonNumber == 2023 })
    }

    private fun createTestSeason(
        seasonNumber: Int = 2024,
        currentSeason: Boolean = true,
    ): Season {
        return Season().apply {
            this.seasonNumber = seasonNumber
            this.currentSeason = currentSeason
        }
    }
}
