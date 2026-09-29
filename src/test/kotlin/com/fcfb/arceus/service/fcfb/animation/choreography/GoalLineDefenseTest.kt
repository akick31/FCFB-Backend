package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class GoalLineDefenseTest {
    @ParameterizedTest
    @EnumSource(DefensivePlaybook::class)
    fun `no defender lines up behind the back of the end zone on a goal-line snap`(defense: DefensivePlaybook) {
        OffensivePlaybook.entries.forEach { offense ->
            listOf(TeamSide.HOME, TeamSide.AWAY).forEach { possession ->
                val forward = if (possession == TeamSide.HOME) 1f else -1f
                val lineOfScrimmage = if (possession == TeamSide.HOME) 97f else 3f
                val goalLine = if (possession == TeamSide.HOME) 100f else 0f
                val play =
                    Play().apply {
                        this.possession = possession
                        ballLocation = 3
                        playCall = PlayCall.TWO_POINT
                        actualResult = ActualResult.SUCCESS
                    }
                val endSpot = goalLine + forward * 5f
                val context = PlayContext(play, lineOfScrimmage, endSpot, forward, 1f, offense, defense)
                val scene = ScrimmageScene.from(context)

                scene.defense.forEach { spot ->
                    val depthPastGoal = (spot.along - goalLine) * forward
                    assertTrue(
                        depthPastGoal <= MAX_END_ZONE_DEPTH + TOLERANCE,
                        "$offense/$defense/$possession defender ${depthPastGoal}yd deep",
                    )
                }
            }
        }
    }

    companion object {
        private const val MAX_END_ZONE_DEPTH = 3f
        private const val TOLERANCE = 0.01f
    }
}
