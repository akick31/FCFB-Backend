package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.animation.choreography.script.CompletedPassScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.RunPlayScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaybookFormationTest {
    @Test
    fun `every offensive playbook produces a distinct pre-snap formation`() {
        val formations = OffensivePlaybook.entries.map { alignmentSet(it) }
        assertEquals(OffensivePlaybook.entries.size, formations.toSet().size, "two offensive playbooks share a formation")
    }

    @Test
    fun `every defensive playbook produces a distinct front`() {
        val offense = OffensiveAlignments.forPlaybook(OffensivePlaybook.PRO)
        val fronts = DefensivePlaybook.entries.map { DefensiveAlignments.forPlaybook(it, offense).spots.toSet() }
        assertEquals(DefensivePlaybook.entries.size, fronts.toSet().size, "two defensive playbooks share a front")
    }

    @Test
    fun `the scene aligns offense to the offensive playbook and defense to the defensive playbook`() {
        val context = context(OffensivePlaybook.SPREAD, DefensivePlaybook.FOUR_THREE)
        val scene = ScrimmageScene.from(context)

        val expectedOffense =
            OffensiveAlignments.forPlaybook(
                OffensivePlaybook.SPREAD,
            ).spots.map { context.offenseSpot(it.depth, it.lateral) }
        val expectedDefense =
            DefensiveAlignments.forPlaybook(DefensivePlaybook.FOUR_THREE, OffensiveAlignments.forPlaybook(OffensivePlaybook.SPREAD))
                .spots.map { context.defenseSpot(it.depth, it.lateral) }

        assertEquals(expectedOffense, scene.offense)
        assertEquals(expectedDefense, scene.defense)
    }

    @Test
    fun `run and pass scripts realign both units when the matchup's playbooks change`() {
        listOf(RunPlayScript() to PlayCall.RUN, CompletedPassScript() to PlayCall.PASS).forEach { (script, call) ->
            val spread = script.choreograph(context(OffensivePlaybook.SPREAD, DefensivePlaybook.FOUR_THREE, call))
            val flexbone = script.choreograph(context(OffensivePlaybook.FLEXBONE, DefensivePlaybook.THREE_THREE_FIVE, call))

            assert(startSet(spread.offense) != startSet(flexbone.offense)) { "$call offense did not follow the offensive playbook" }
            assert(startSet(spread.defense) != startSet(flexbone.defense)) { "$call defense did not follow the defensive playbook" }
        }
    }

    private fun startSet(tracks: List<Track>) = tracks.map { it.at(0f) }.toSet()

    private fun alignmentSet(playbook: OffensivePlaybook) = OffensiveAlignments.forPlaybook(playbook).spots.toSet()

    private fun context(
        offense: OffensivePlaybook,
        defense: DefensivePlaybook,
        call: PlayCall = PlayCall.RUN,
    ): PlayContext {
        val play =
            Play().apply {
                possession = TeamSide.HOME
                ballLocation = 30
                playCall = call
                actualResult = ActualResult.GAIN
            }
        return PlayContext(play, 30f, 37f, 1f, 1f, offense, defense)
    }
}
