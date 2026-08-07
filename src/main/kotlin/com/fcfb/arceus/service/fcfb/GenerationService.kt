package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service

@Service
class GenerationService(
    private val gameStatsService: GameStatsService,
    private val seasonStatsService: SeasonStatsService,
    private val postseasonSeasonStatsService: PostseasonSeasonStatsService,
    private val conferenceStatsService: ConferenceStatsService,
    private val postseasonConferenceStatsService: PostseasonConferenceStatsService,
    private val leagueStatsService: LeagueStatsService,
    private val postseasonLeagueStatsService: PostseasonLeagueStatsService,
    private val playbookStatsService: PlaybookStatsService,
    private val postseasonPlaybookStatsService: PostseasonPlaybookStatsService,
    private val recordService: RecordService,
) {
    fun generateAll() {
        Logger.info("Starting full stats and records generation")

        runStep("game stats") { gameStatsService.generateAllGameStats() }
        runStep("season stats") { seasonStatsService.generateAllSeasonStats() }
        runStep("postseason season stats") { postseasonSeasonStatsService.generateAllPostseasonSeasonStats() }
        runStep("playbook stats") { playbookStatsService.generateAllPlaybookStats() }
        runStep("postseason playbook stats") { postseasonPlaybookStatsService.generateAllPostseasonPlaybookStats() }
        runStep("records") { recordService.generateAllRecords() }
        runStep("conference stats") { conferenceStatsService.generateAllConferenceStats() }
        runStep("league stats") { leagueStatsService.generateAllLeagueStats() }
        runStep("postseason conference stats") { postseasonConferenceStatsService.generateAllPostseasonConferenceStats() }
        runStep("postseason league stats") { postseasonLeagueStatsService.generateAllPostseasonLeagueStats() }

        Logger.info("Completed full stats and records generation")
    }

    private fun runStep(
        name: String,
        step: () -> Unit,
    ) {
        Logger.info("Generation step starting: $name")
        try {
            step()
        } catch (e: Exception) {
            throw Exception("Full generation failed at step '$name'", e)
        }
        Logger.info("Generation step completed: $name")
    }
}
