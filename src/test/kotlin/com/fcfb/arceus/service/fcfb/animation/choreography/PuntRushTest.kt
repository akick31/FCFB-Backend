package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.animation.choreography.script.BlockedKickPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.PuntPlayScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PuntRushTest {
    @Test
    fun `a blocked punt is charged by every lineman while the gunners stay jammed`() {
        (1..PLAYS_TRIED).forEach { playId ->
            val context = context(playId, ActualResult.BLOCKED, endSpot = 28f)
            val defense = BlockedKickPlayScript().choreograph(context).defense
            val punter = context.offenseSpot(PUNT_DEPTH, 0f)

            (0 until LINEMEN).forEach { lineman ->
                assertTrue(defense[lineman].at(EARLY).distanceTo(defense[lineman].at(0f)) > MIN_CHARGE, "play $playId lineman $lineman")
                assertTrue(defense[lineman].at(AFTER_KICK).distanceTo(punter) < CLOSE, "play $playId lineman $lineman")
            }
            GUNNER_INDEXES.forEach { gunner ->
                assertEquals(0f, defense[gunner].at(AFTER_KICK).distanceTo(defense[gunner].at(0f)), 0.001f, "play $playId gunner $gunner")
            }
        }
    }

    @Test
    fun `some good punts see the whole line charge the punter and only some`() {
        val charged =
            (1..PLAYS_TRIED).count { playId ->
                val context = context(playId, ActualResult.PUNT, endSpot = 68f)
                val defense = PuntPlayScript().choreograph(context).defense
                val punter = context.offenseSpot(PUNT_DEPTH, 0f)
                (0 until LINEMEN).all { closestApproach(defense[it], punter) < CLOSE }
            }

        assertTrue(charged in MIN_CHARGED..MAX_CHARGED, "$charged of $PLAYS_TRIED punts were charged")
    }

    private fun closestApproach(
        track: Track,
        punter: FieldPoint,
    ): Float = (0..SAMPLES).minOf { track.at(RUSH_FROM + (AFTER_KICK - RUSH_FROM) * it / SAMPLES).distanceTo(punter) }

    private fun context(
        playId: Int,
        result: ActualResult,
        endSpot: Float,
    ): PlayContext {
        val play =
            Play().apply {
                this.playId = playId
                possession = TeamSide.HOME
                ballLocation = 30
                playCall = PlayCall.PUNT
                actualResult = result
            }
        return PlayContext(play, 30f, endSpot, 1f, 1f, OffensivePlaybook.PRO, DefensivePlaybook.FOUR_THREE)
    }

    companion object {
        private const val PLAYS_TRIED = 60
        private const val LINEMEN = 6
        private val GUNNER_INDEXES = listOf(6, 7)
        private const val PUNT_DEPTH = 14f
        private const val EARLY = 0.08f
        private const val RUSH_FROM = 0.2f
        private const val SAMPLES = 20
        private const val AFTER_KICK = 0.3f
        private const val MIN_CHARGE = 3f
        private const val CLOSE = 5f
        private const val MIN_CHARGED = 8
        private const val MAX_CHARGED = 40
    }
}
