package com.fcfb.arceus.rendering.animation.fieldgoal

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GoalPostSceneTiltTest {
    @ParameterizedTest
    @ValueSource(ints = [99, 95, 90, 85, 80, 75, 70, 65, 60, 55, 50])
    fun `every kick leaves the zoomed close-view room above the wall and keeps the kicker in frame`(ballLocation: Int) {
        val layout =
            GoalPostScenePainter.layoutFor(
                Play().apply {
                    playCall = PlayCall.FIELD_GOAL
                    this.ballLocation = ballLocation
                    possession = TeamSide.HOME
                },
            )
        val wallTop = layout.mapY(140)

        assertTrue(wallTop >= (BackdropArea.CLOSE_ROOM * layout.scale).toInt() - 1, "wall top $wallTop at scale ${layout.scale}")
        assertTrue(layout.yardY(layout.lineOfScrimmageYards + GoalPostScenePainter.HOLD_DEPTH_YARDS) < GoalPostScenePainter.SCENE_HEIGHT)
    }
}
