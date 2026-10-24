package com.fcfb.arceus.rendering.animation.fieldgoal

import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.enums.animation.KickOutcome
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.appearance.StadiumDesign
import com.fcfb.arceus.model.appearance.TeamField
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KickBackstopTest {
    private val layout =
        GoalPostScenePainter.layoutFor(
            Play().apply {
                possession = TeamSide.HOME
                ballLocation = 75
                playCall = PlayCall.FIELD_GOAL
                actualResult = ActualResult.GOOD
            },
        )

    private fun theme(
        style: FieldStyle = FieldStyle.HOME_FIELD,
        configure: StadiumDesign.() -> Unit,
    ) = FieldTheme(
        style = style,
        homeTeam = Team().apply { name = "Wyoming" },
        awayTeam = Team().apply { name = "Boise State" },
        centerLogoUrl = null,
        homeField = TeamField(),
        stadium = StadiumDesign().apply(configure),
    )

    @Test
    fun `only a crowd backdrop keeps the net`() {
        val crowd = theme { stadiumBackdrop = "CROWD" }
        val forest = theme { stadiumBackdrop = "FOREST" }

        assertEquals(KickBackstop.Net, GoalPostScenePainter.backstopFor(crowd, crowd.leftSide(), layout))
        assertTrue(GoalPostScenePainter.backstopFor(forest, forest.leftSide(), layout) is KickBackstop.Grass)
    }

    @Test
    fun `a wall without a crowd lets the ball carry over it`() {
        val walled =
            theme {
                stadiumBackdrop = "LAKE"
                wallEnabled = true
            }

        assertTrue(GoalPostScenePainter.backstopFor(walled, walled.leftSide(), layout) is KickBackstop.Wall)
    }

    @Test
    fun `postseason fields with no stadium keep the net while one at a venue follows that venue`() {
        val atVenue = theme(FieldStyle.BOWL) { stadiumBackdrop = "FOREST" }
        val noStadium = atVenue.copy(stadium = null)

        assertEquals(KickBackstop.Net, GoalPostScenePainter.backstopFor(noStadium, noStadium.leftSide(), layout))
        assertTrue(GoalPostScenePainter.backstopFor(atVenue, atVenue.leftSide(), layout) is KickBackstop.Grass)
    }

    @Test
    fun `a ball carried over the wall drops out of sight while one onto the grass stays in view`() {
        val wallTop = 100
        val overWall = KickTrajectory(layout, KickOutcome.GOOD, 0f, KickBackstop.Wall(wallTop))
        val ontoGrass = KickTrajectory(layout, KickOutcome.GOOD, 0f, KickBackstop.Grass(layout.endZoneTopY - 10, distance = 0.5f))

        assertFalse(overWall.at(1f).visible)
        assertTrue(ontoGrass.at(1f).visible)
        assertEquals((layout.endZoneTopY - 10).toFloat(), ontoGrass.at(1f).shadowY)
    }

    @Test
    fun `the ball rebounds off a full facility building`() {
        val facility =
            theme {
                stadiumBackdrop = "FACILITY"
                backdropStyle = "FACILITY_BRICK"
            }

        assertTrue(GoalPostScenePainter.backstopFor(facility, facility.leftSide(), layout) is KickBackstop.Facade)
    }

    @Test
    fun `the ball lands shorter in front of a building under the board than in front of a normal jumbotron`() {
        val open = theme { stadiumBackdrop = "FOREST" }
        val inFront =
            theme {
                stadiumBackdrop = "FACILITY"
                backdropStyle = "FACILITY_NARROW_GEORGIAN"
                jumbotronEnabled = true
            }
        val underBoard =
            theme {
                stadiumBackdrop = "FACILITY"
                backdropStyle = "FACILITY_NARROW_GEORGIAN"
                jumbotronEnabled = true
                jumbotronOverBuilding = true
            }

        val far = GoalPostScenePainter.backstopFor(open, open.leftSide(), layout) as KickBackstop.Grass
        val shortOfBoard = GoalPostScenePainter.backstopFor(inFront, inFront.leftSide(), layout) as KickBackstop.Grass
        val shortOfBuilding = GoalPostScenePainter.backstopFor(underBoard, underBoard.leftSide(), layout) as KickBackstop.Grass

        assertTrue(far.landingY < shortOfBoard.landingY)
        assertTrue(shortOfBoard.landingY < shortOfBuilding.landingY)
    }

    @Test
    fun `a ball carried onto open grass never rises back up once it starts down`() {
        val trajectory = KickTrajectory(layout, KickOutcome.GOOD, 0f, KickBackstop.Grass(layout.endZoneTopY - 40, distance = 1f))
        val ys = (0..100).map { trajectory.at(trajectory.arrival + (1f - trajectory.arrival) * it / 100f).y }

        assertTrue(ys.zipWithNext().all { (before, after) -> after >= before - 0.01f })
    }
}
