package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.animation.choreography.script.BlockedKickPlayScript
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlockedPuntRecoveryTest {
    @Test
    fun `a blocked punt is fallen on behind the line, not carried downfield`() {
        (1..PLAYS_TRIED).forEach { playId ->
            val lineOfScrimmage = 30f
            val play =
                Play().apply {
                    this.playId = playId
                    possession = TeamSide.HOME
                    ballLocation = 30
                    playCall = PlayCall.PUNT
                    actualResult = ActualResult.BLOCKED
                }
            val context =
                PlayContext(play, lineOfScrimmage, DOWNFIELD_END_SPOT, 1f, 1f, OffensivePlaybook.PRO, DefensivePlaybook.FOUR_THREE)

            val choreography = BlockedKickPlayScript().choreograph(context)
            val ball = choreography.ball.at(choreography.endsAt).position

            assertTrue(ball.along < lineOfScrimmage, "play $playId ball ended past the line at ${ball.along}")
            assertTrue(
                ball.along > lineOfScrimmage - MAX_YARDS_BEHIND,
                "play $playId ball ended ${lineOfScrimmage - ball.along}yd behind the line",
            )
            val nearestDefender = choreography.defense.minOf { it.at(choreography.endsAt).distanceTo(ball) }
            assertTrue(nearestDefender < COVER_DISTANCE, "play $playId nearest defender was ${nearestDefender}yd from the ball")
        }
    }

    companion object {
        private const val PLAYS_TRIED = 40
        private const val DOWNFIELD_END_SPOT = 60f
        private const val MAX_YARDS_BEHIND = 16f
        private const val COVER_DISTANCE = 2.5f
    }
}
