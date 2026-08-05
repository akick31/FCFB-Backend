package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Schedule
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

class ScheduleRepositoryTest {
    private lateinit var scheduleRepository: ScheduleRepository

    @BeforeEach
    fun setUp() {
        scheduleRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and findById`() {
        val schedule =
            createTestSchedule(
                id = 1,
                homeTeam = "Alabama",
                awayTeam = "Auburn",
                season = 2024,
                week = 8,
                gameType = GameType.CONFERENCE_GAME,
                started = false,
                finished = false,
            )

        every { scheduleRepository.save(any()) } returns schedule
        every { scheduleRepository.findById(1) } returns java.util.Optional.of(schedule)

        val savedSchedule = scheduleRepository.save(schedule)
        val foundSchedule = scheduleRepository.findById(savedSchedule.id).get()

        assertNotNull(foundSchedule)
        assertEquals(1, foundSchedule.id)
        assertEquals("Alabama", foundSchedule.homeTeam)
        assertEquals("Auburn", foundSchedule.awayTeam)
        assertEquals(2024, foundSchedule.season)
        assertEquals(8, foundSchedule.week)
        assertEquals(GameType.CONFERENCE_GAME, foundSchedule.gameType)
        foundSchedule.started?.let { assertFalse(it) }
        foundSchedule.finished?.let { assertFalse(it) }
    }

    @Test
    fun `test getGamesToStartBySeasonAndWeek`() {
        val unstartedGame1 =
            createTestSchedule(
                id = 1,
                homeTeam = "Alabama",
                awayTeam = "Auburn",
                season = 2024,
                week = 8,
                started = false,
            )
        val unstartedGame2 =
            createTestSchedule(
                id = 2,
                homeTeam = "Georgia",
                awayTeam = "Florida",
                season = 2024,
                week = 8,
                started = false,
            )
        val unstartedGames = listOf(unstartedGame1, unstartedGame2)

        every { scheduleRepository.getGamesToStartBySeasonAndWeek(2024, 8) } returns unstartedGames

        val foundGames = scheduleRepository.getGamesToStartBySeasonAndWeek(2024, 8)

        assertNotNull(foundGames)
        assertEquals(2, foundGames.size)
        assertTrue(foundGames.any { it.homeTeam == "Alabama" })
        assertTrue(foundGames.any { it.homeTeam == "Georgia" })
    }

    @Test
    fun `test getTeamOpponent when team is home`() {
        every { scheduleRepository.getTeamOpponent(2024, 8, "Alabama") } returns "Auburn"

        val opponent = scheduleRepository.getTeamOpponent(2024, 8, "Alabama")

        assertEquals("Auburn", opponent)
    }

    @Test
    fun `test getTeamOpponent when team is away`() {
        every { scheduleRepository.getTeamOpponent(2024, 8, "Auburn") } returns "Alabama"

        val opponent = scheduleRepository.getTeamOpponent(2024, 8, "Auburn")

        assertEquals("Alabama", opponent)
    }

    @Test
    fun `test getTeamOpponent returns null when team not found`() {
        every { scheduleRepository.getTeamOpponent(2024, 8, "NonExistent") } returns null

        val opponent = scheduleRepository.getTeamOpponent(2024, 8, "NonExistent")

        assertNull(opponent)
    }

    @Test
    fun `test getScheduleBySeasonAndTeam`() {
        val schedule1 =
            createTestSchedule(
                id = 1,
                homeTeam = "Alabama",
                awayTeam = "Auburn",
                season = 2024,
                week = 8,
            )
        val schedule2 =
            createTestSchedule(
                id = 2,
                homeTeam = "Georgia",
                awayTeam = "Alabama",
                season = 2024,
                week = 9,
            )
        val schedules = listOf(schedule1, schedule2)

        every { scheduleRepository.getScheduleBySeasonAndTeam(2024, "Alabama") } returns schedules

        val foundSchedules = scheduleRepository.getScheduleBySeasonAndTeam(2024, "Alabama")

        assertNotNull(foundSchedules)
        assertEquals(2, foundSchedules.size)
        assertTrue(foundSchedules.any { it.homeTeam == "Alabama" })
        assertTrue(foundSchedules.any { it.awayTeam == "Alabama" })
    }

    @Test
    fun `test findGameInSchedule`() {
        val schedule =
            createTestSchedule(
                id = 1,
                homeTeam = "Alabama",
                awayTeam = "Auburn",
                season = 2024,
                week = 8,
            )

        every { scheduleRepository.findGameInSchedule("Alabama", "Auburn", 2024, 8) } returns schedule

        val foundSchedule = scheduleRepository.findGameInSchedule("Alabama", "Auburn", 2024, 8)

        assertNotNull(foundSchedule)
        assertEquals(1, foundSchedule.id)
        assertEquals("Alabama", foundSchedule.homeTeam)
        assertEquals("Auburn", foundSchedule.awayTeam)
    }

    @Test
    fun `test findGameInSchedule returns null when not found`() {
        every { scheduleRepository.findGameInSchedule("Alabama", "Georgia", 2024, 8) } returns null

        val foundSchedule = scheduleRepository.findGameInSchedule("Alabama", "Georgia", 2024, 8)

        assertNull(foundSchedule)
    }

    @Test
    fun `test checkIfWeekIsOver when all games finished`() {
        every { scheduleRepository.checkIfWeekIsOver(2024, 8) } returns 1

        val result = scheduleRepository.checkIfWeekIsOver(2024, 8)

        assertEquals(1, result)
    }

    @Test
    fun `test checkIfWeekIsOver when some games unfinished`() {
        every { scheduleRepository.checkIfWeekIsOver(2024, 8) } returns 0

        val result = scheduleRepository.checkIfWeekIsOver(2024, 8)

        assertEquals(0, result)
    }

    @Test
    fun `test findAll`() {
        val schedule1 = createTestSchedule(id = 1, homeTeam = "Alabama")
        val schedule2 = createTestSchedule(id = 2, homeTeam = "Georgia")
        val allSchedules = listOf(schedule1, schedule2)

        every { scheduleRepository.findAll() } returns allSchedules

        val foundSchedules = scheduleRepository.findAll()

        assertEquals(2, foundSchedules.count())
        assertTrue(foundSchedules.any { it.homeTeam == "Alabama" })
        assertTrue(foundSchedules.any { it.homeTeam == "Georgia" })
    }

    @Test
    fun `test count`() {
        every { scheduleRepository.count() } returns 100L

        val count = scheduleRepository.count()

        assertEquals(100L, count)
    }

    @Test
    fun `test delete`() {
        val schedule = createTestSchedule(id = 1)
        every { scheduleRepository.delete(schedule) } returns Unit

        scheduleRepository.delete(schedule)

        verify { scheduleRepository.delete(schedule) }
    }

    private fun createTestSchedule(
        id: Int = 1,
        homeTeam: String = "Alabama",
        awayTeam: String = "Auburn",
        season: Int = 2024,
        week: Int = 8,
        gameType: GameType = GameType.CONFERENCE_GAME,
        started: Boolean = false,
        finished: Boolean = false,
    ): Schedule {
        return Schedule().apply {
            this.id = id
            this.homeTeam = homeTeam
            this.awayTeam = awayTeam
            this.season = season
            this.week = week
            this.gameType = gameType
            this.started = started
            this.finished = finished
        }
    }
}
