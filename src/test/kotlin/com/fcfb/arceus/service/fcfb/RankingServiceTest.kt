package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.ranking.PollType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Ranking
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.RankingRepository
import com.fcfb.arceus.repositories.TeamRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RankingServiceTest {
    private val rankingRepository: RankingRepository = mockk()
    private val teamRepository: TeamRepository = mockk()
    private val gameRepository: GameRepository = mockk()
    private lateinit var rankingService: RankingService

    private val season = 12
    private val week = 3

    @BeforeEach
    fun setup() {
        rankingService = RankingService(rankingRepository, teamRepository, gameRepository)
        every { rankingRepository.existsForWeek(season, week, PollType.PLAYOFF_COMMITTEE.name) } returns 0
        every { rankingRepository.findBySeasonWeekAndPollType(season, week, PollType.COACHES_POLL.name) } returns emptyList()
        every { rankingRepository.findBySeasonWeekAndPollType(season, week, PollType.PLAYOFF_COMMITTEE.name) } returns emptyList()
    }

    private fun ranking(
        pollType: PollType,
        rank: Int,
        teamId: Int,
    ) = Ranking(season, week, pollType, rank, teamId, 0, 0)

    @Test
    fun `game ranks come from the coaches poll when no playoff poll is uploaded for the week`() {
        every { rankingRepository.findBySeasonWeekAndPollType(season, week, PollType.COACHES_POLL.name) } returns
            listOf(ranking(PollType.COACHES_POLL, 4, 10), ranking(PollType.COACHES_POLL, 9, 20))

        assertEquals(4 to 9, rankingService.getTeamRanks(season, week, 10, 20))
    }

    @Test
    fun `game ranks come from the playoff committee poll when one is uploaded for the week`() {
        every { rankingRepository.existsForWeek(season, week, PollType.PLAYOFF_COMMITTEE.name) } returns 12
        every { rankingRepository.findBySeasonWeekAndPollType(season, week, PollType.PLAYOFF_COMMITTEE.name) } returns
            listOf(ranking(PollType.PLAYOFF_COMMITTEE, 1, 10), ranking(PollType.PLAYOFF_COMMITTEE, 2, 20))
        every { rankingRepository.findBySeasonWeekAndPollType(season, week, PollType.COACHES_POLL.name) } returns
            listOf(ranking(PollType.COACHES_POLL, 7, 10), ranking(PollType.COACHES_POLL, 8, 20))

        assertEquals(1 to 2, rankingService.getTeamRanks(season, week, 10, 20))
    }

    @Test
    fun `a team missing from the poll has no rank`() {
        every { rankingRepository.findBySeasonWeekAndPollType(season, week, PollType.COACHES_POLL.name) } returns
            listOf(ranking(PollType.COACHES_POLL, 4, 10))

        val (homeRank, awayRank) = rankingService.getTeamRanks(season, week, 10, 20)

        assertEquals(4, homeRank)
        assertNull(awayRank)
    }

    @Test
    fun `latest rankings are the most recently uploaded week of the poll`() {
        every { rankingRepository.findLatest(PollType.COACHES_POLL.name) } returns ranking(PollType.COACHES_POLL, 1, 10)
        every { rankingRepository.findBySeasonWeekAndPollType(season, week, PollType.COACHES_POLL.name) } returns
            listOf(ranking(PollType.COACHES_POLL, 1, 10))
        every { teamRepository.findById(10) } returns java.util.Optional.empty()

        val latest = rankingService.getLatestRankings("COACHES_POLL")

        assertEquals(listOf(season to week), latest.map { it.season to it.week })
    }

    @Test
    fun `latest rankings are empty when the poll has never been uploaded`() {
        every { rankingRepository.findLatest(PollType.COACHES_POLL.name) } returns null

        assertEquals(emptyList<Any>(), rankingService.getLatestRankings("COACHES_POLL"))
    }

    @Test
    fun `team rankings return the team's rank for every uploaded week in order`() {
        every { rankingRepository.findByTeamAndPollType(10, PollType.COACHES_POLL.name) } returns
            listOf(
                Ranking(11, 5, PollType.COACHES_POLL, 8, 10, 4, 1),
                Ranking(12, 1, PollType.COACHES_POLL, 3, 10, 1, 0),
            )
        every { teamRepository.findById(10) } returns
            java.util.Optional.of(
                Team().apply {
                    id = 10
                    name = "Army"
                },
            )

        val history = rankingService.getTeamRankings(10, "COACHES_POLL")

        assertEquals(listOf(11 to 5, 12 to 1), history.map { it.season to it.week })
        assertEquals(listOf(8, 3), history.map { it.rank })
        assertEquals(listOf("Army", "Army"), history.map { it.teamName })
    }

    @Test
    fun `ranked games are filtered by team and season and ordered chronologically`() {
        fun game(
            home: String,
            away: String,
            gameSeason: Int,
            gameWeek: Int,
        ) = Game().apply {
            homeTeam = home
            awayTeam = away
            this.season = gameSeason
            this.week = gameWeek
        }
        every { gameRepository.getRankedGames() } returns
            listOf(
                game("Army", "Navy", 12, 5),
                game("Ohio State", "Army", 11, 9),
                game("Ohio State", "Michigan", 12, 2),
                game("Navy", "Army", 12, 1),
            )

        val armyGames = rankingService.getRankedGames("Army", null)
        val armySeason12 = rankingService.getRankedGames("Army", 12)
        val everyone = rankingService.getRankedGames("all", null)

        assertEquals(listOf(11 to 9, 12 to 1, 12 to 5), armyGames.map { it.season to it.week })
        assertEquals(listOf(12 to 1, 12 to 5), armySeason12.map { it.season to it.week })
        assertEquals(4, everyone.size)
    }
}
