package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.animation.choreography.script.RunPlayScript
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class TwoPointEndZoneTest {
    @ParameterizedTest
    @EnumSource(TeamSide::class)
    fun `a good conversion has the ball well inside the end zone when the animation stops`(possession: TeamSide) {
        val forward = if (possession == TeamSide.HOME) 1f else -1f
        val goalLine = if (possession == TeamSide.HOME) 100f else 0f
        (1..PLAYS_TRIED).forEach { playId ->
            val play =
                Play().apply {
                    this.playId = playId
                    this.possession = possession
                    ballLocation = 97
                    playCall = PlayCall.TWO_POINT
                    actualResult = ActualResult.SUCCESS
                }
            val lineOfScrimmage = if (possession == TeamSide.HOME) 97f else 3f
            val endSpot = goalLine + forward * END_ZONE_CENTER
            val context = PlayContext(play, lineOfScrimmage, endSpot, forward, 1f, OffensivePlaybook.PRO, DefensivePlaybook.FOUR_THREE)

            val choreography = RunPlayScript().choreograph(context)
            val depthPastGoalLine = (choreography.ball.at(choreography.endsAt).position.along - goalLine) * forward

            assertTrue(depthPastGoalLine >= MIN_DEPTH, "play $playId ended ${depthPastGoalLine}yd past the goal line")
        }
    }

    companion object {
        private const val PLAYS_TRIED = 60
        private const val END_ZONE_CENTER = 5f
        private const val MIN_DEPTH = 3f
    }
}
