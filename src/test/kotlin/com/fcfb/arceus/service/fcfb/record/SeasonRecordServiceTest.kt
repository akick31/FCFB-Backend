package com.fcfb.arceus.service.fcfb.record

import com.fcfb.arceus.enums.records.RecordScope
import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.repositories.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SeasonRecordServiceTest {
    private val recordRepository: RecordRepository = mockk()
    private val gameStatsRepository: GameStatsRepository = mockk()
    private val recordStatUtils: RecordStatUtils = mockk()
    private val userRepository: UserRepository = mockk()
    private lateinit var seasonRecordService: SeasonRecordService

    private val discordId = "111222333"

    @BeforeEach
    fun setup() {
        seasonRecordService = SeasonRecordService(recordRepository, gameStatsRepository, recordStatUtils, userRepository)
        every { recordStatUtils.calculateSeasonValue(any(), any()) } returns 100.0
    }

    @Test
    fun `attributes a season record to the coach's current username even after a mid-season rename`() {
        // The coach renamed from "flying_porygon" to "cyclone_puffin" partway through
        // the season, so the two GameStats rows carry different `coaches` strings - but
        // the same coachDiscordIds, which is what attribution should actually key on.
        val earlyGame =
            GameStats(gameId = 1, team = "Arkansas", season = 13, coaches = listOf("flying_porygon"), coachDiscordIds = listOf(discordId))
        val laterGame =
            GameStats(gameId = 2, team = "Arkansas", season = 13, coaches = listOf("cyclone_puffin"), coachDiscordIds = listOf(discordId))

        every { userRepository.findByDiscordId(discordId) } returns User().apply { username = "cyclone_puffin" }

        val saved = slot<Record>()
        every { recordRepository.save(capture(saved)) } answers { firstArg() }

        seasonRecordService.generateSeasonRecord(
            Stats.SCORE,
            listOf(earlyGame, laterGame),
            RecordType.SINGLE_SEASON,
            scopes = setOf(RecordScope.LEAGUE),
        )

        assertEquals("cyclone_puffin", saved.captured.coach)
    }

    @Test
    fun `falls back to the captured username when the discord id can't be resolved to a user`() {
        val game =
            GameStats(gameId = 1, team = "Arkansas", season = 13, coaches = listOf("some_coach"), coachDiscordIds = listOf(discordId))
        every { userRepository.findByDiscordId(discordId) } returns null

        val saved = slot<Record>()
        every { recordRepository.save(capture(saved)) } answers { firstArg() }

        seasonRecordService.generateSeasonRecord(
            Stats.SCORE,
            listOf(game),
            RecordType.SINGLE_SEASON,
            scopes = setOf(RecordScope.LEAGUE),
        )

        assertEquals("some_coach", saved.captured.coach)
    }

    @Test
    fun `falls back to matching by username when a row predates the discord id backfill`() {
        val game =
            GameStats(gameId = 1, team = "Arkansas", season = 13, coaches = listOf("legacy_coach"), coachDiscordIds = null)
        every { userRepository.findByDiscordId("legacy_coach") } returns null

        val saved = slot<Record>()
        every { recordRepository.save(capture(saved)) } answers { firstArg() }

        seasonRecordService.generateSeasonRecord(
            Stats.SCORE,
            listOf(game),
            RecordType.SINGLE_SEASON,
            scopes = setOf(RecordScope.LEAGUE),
        )

        assertEquals("legacy_coach", saved.captured.coach)
    }
}
