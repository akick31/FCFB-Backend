package com.fcfb.arceus.model.animation

import com.fcfb.arceus.enums.animation.BackdropVariant
import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.enums.animation.PylonStyle
import com.fcfb.arceus.enums.animation.ScoreboardShape
import com.fcfb.arceus.enums.animation.ScoreboardStyle
import com.fcfb.arceus.enums.animation.StadiumBackdrop
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.StadiumDesign
import com.fcfb.arceus.model.appearance.TeamField
import com.fcfb.arceus.model.team.Team
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color

class FieldThemeStadiumTest {
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
    fun `the crowd always keeps its wall and other backdrops drop it unless asked`() {
        val crowd = theme { stadiumBackdrop = "CROWD" }
        val forest = theme { stadiumBackdrop = "FOREST" }
        val forestWithWall = theme { stadiumBackdrop = "FOREST" }.also { it.stadium!!.wallEnabled = true }

        assertTrue(crowd.wallShown(crowd.leftSide()))
        assertFalse(forest.wallShown(forest.leftSide()))
        assertTrue(forestWithWall.wallShown(forestWithWall.leftSide()))
    }

    @Test
    fun `each end zone decides its own wall`() {
        val split =
            theme {
                stadiumBackdrop = "LAKE"
                rightStadiumBackdrop = "CROWD"
            }

        assertFalse(split.wallShown(split.leftSide()))
        assertTrue(split.wallShown(split.rightSide()))
    }

    @Test
    fun `postseason fields with no stadium keep the crowd and its wall`() {
        val bowl = theme(FieldStyle.BOWL) { stadiumBackdrop = "FOREST" }.copy(stadium = null)

        assertEquals(StadiumBackdrop.CROWD, bowl.stadiumBackdrop(bowl.leftSide()))

        assertTrue(bowl.wallShown(bowl.leftSide()))
    }

    @Test
    fun `a full-size crowd never shows a jumbotron even if one was saved`() {
        val fullCrowd =
            theme {
                stadiumBackdrop = "CROWD"
                stadiumSize = 1.0
                jumbotronEnabled = true
            }
        val smallerCrowd =
            theme {
                stadiumBackdrop = "CROWD"
                stadiumSize = 0.6
                jumbotronEnabled = true
            }
        val lake =
            theme {
                stadiumBackdrop = "LAKE"
                jumbotronEnabled = true
            }

        assertFalse(fullCrowd.jumbotronEnabled(fullCrowd.leftSide()))
        assertTrue(smallerCrowd.jumbotronEnabled(smallerCrowd.leftSide()))
        assertTrue(lake.jumbotronEnabled(lake.leftSide()))
    }

    @Test
    fun `jumbotron size is kept within the supported range`() {
        val tooBig = theme { jumbotronSize = 5.0 }
        val tooSmall = theme { rightJumbotronSize = 0.1 }

        assertEquals(1.4f, tooBig.jumbotronSize(tooBig.leftSide()))
        assertEquals(0.7f, tooSmall.jumbotronSize(tooSmall.rightSide()))
    }

    @Test
    fun `a style only applies to the backdrop it belongs to`() {
        val campus =
            theme {
                stadiumBackdrop = "CAMPUS"
                backdropStyle = "CAMPUS_GOTHIC"
            }
        val staleStyle =
            theme {
                stadiumBackdrop = "LAKE"
                backdropStyle = "CAMPUS_GOTHIC"
            }
        val forest = theme { stadiumBackdrop = "FOREST" }

        assertEquals(BackdropVariant.CAMPUS_GOTHIC, campus.backdropVariant(campus.leftSide()))
        assertEquals(BackdropVariant.LAKE_HILLS, staleStyle.backdropVariant(staleStyle.leftSide()))
        assertNull(forest.backdropVariant(forest.leftSide()))
    }

    @Test
    fun `the right end zone uses the left style until it picks its own backdrop`() {
        val matching =
            theme {
                stadiumBackdrop = "MOUNTAINS"
                backdropStyle = "MOUNTAINS_DESERT"
                rightBackdropStyle = "MOUNTAINS_SMOKY"
            }
        val ownBackdrop =
            theme {
                stadiumBackdrop = "MOUNTAINS"
                backdropStyle = "MOUNTAINS_DESERT"
                rightStadiumBackdrop = "LAKE"
                rightBackdropStyle = "LAKE_PINES"
            }

        assertEquals(BackdropVariant.MOUNTAINS_DESERT, matching.backdropVariant(matching.rightSide()))
        assertEquals(BackdropVariant.LAKE_PINES, ownBackdrop.backdropVariant(ownBackdrop.rightSide()))
    }

    @Test
    fun `the right scoreboard style falls back to the left and then to classic`() {
        val leftOnly = theme { jumbotronStyle = "VINTAGE" }
        val both =
            theme {
                jumbotronStyle = "VINTAGE"
                rightJumbotronStyle = "SEGMENT"
            }

        assertEquals(ScoreboardStyle.VINTAGE, leftOnly.scoreboardStyle(leftOnly.rightSide()))
        assertEquals(ScoreboardStyle.SEGMENT, both.scoreboardStyle(both.rightSide()))
        assertEquals(ScoreboardStyle.CLASSIC, theme { }.scoreboardStyle(TeamSide.HOME))
    }

    @Test
    fun `jumbotron height is kept between ground level and the top setting`() {
        val raised = theme { jumbotronElevation = 3.0 }
        val sunk = theme { rightJumbotronElevation = -1.0 }

        assertEquals(1f, raised.jumbotronElevation(raised.leftSide()))
        assertEquals(0f, sunk.jumbotronElevation(sunk.rightSide()))
    }

    @Test
    fun `scoreboard width stays between its natural width and double`() {
        val tooWide = theme { jumbotronWidth = 9.0 }
        val tooNarrow = theme { rightJumbotronWidth = 0.4 }

        assertEquals(2f, tooWide.jumbotronWidth(tooWide.leftSide()))
        assertEquals(1f, tooNarrow.jumbotronWidth(tooNarrow.rightSide()))
    }

    @Test
    fun `right side shape and scoreboard logo match the left unless set`() {
        val matching =
            theme {
                jumbotronShape = "ARCHED"
                scoreboardLogo = "SECONDARY"
            }.copy(homeTeam = Team().apply { secondaryLogo = "secondary.png" })
        val split =
            theme {
                jumbotronShape = "ARCHED"
                rightJumbotronShape = "ROUNDED"
                scoreboardLogo = "SECONDARY"
                rightScoreboardLogo = "NONE"
            }

        assertEquals(ScoreboardShape.ARCHED, matching.scoreboardShape(matching.rightSide()))
        assertEquals("secondary.png", matching.scoreboardLogoUrl(matching.rightSide()))
        assertEquals(ScoreboardShape.ROUNDED, split.scoreboardShape(split.rightSide()))
        assertNull(split.scoreboardLogoUrl(split.rightSide()))
    }

    @Test
    fun `logos only go on boards with a topper`() {
        val rectangle =
            theme {
                scoreboardLogo = "PRIMARY"
            }.copy(homeTeam = Team().apply { logo = "primary.png" })
        val marquee =
            theme {
                scoreboardLogo = "PRIMARY"
                jumbotronShape = "MARQUEE"
            }.copy(homeTeam = Team().apply { logo = "primary.png" })

        assertNull(rectangle.scoreboardLogoUrl(rectangle.leftSide()))
        assertEquals("primary.png", marquee.scoreboardLogoUrl(marquee.leftSide()))
    }

    @Test
    fun `facility buildings never show the wall`() {
        val facility = theme { stadiumBackdrop = "FACILITY" }.also { it.stadium!!.wallEnabled = true }

        assertFalse(facility.wallShown(facility.leftSide()))
    }

    @Test
    fun `outside posts are set per side and default to under the board`() {
        val field = theme { rightJumbotronPylonsOutside = true }

        assertFalse(field.pylonsOutside(field.leftSide()))
        assertTrue(field.pylonsOutside(field.rightSide()))
    }

    @Test
    fun `retired pylon and shape values fall back to the defaults`() {
        val field =
            theme {
                jumbotronPylonStyle = "TWIN"
                jumbotronShape = "HEXAGON"
            }

        assertEquals(PylonStyle.STEEL, field.pylonStyle(field.leftSide()))
        assertEquals(ScoreboardShape.RECTANGLE, field.scoreboardShape(field.leftSide()))
    }

    @Test
    fun `scenery behind the stadium resolves per side and open sky clears it`() {
        val field =
            theme {
                skylineBackdrop = "MOUNTAINS_DESERT"
                rightSkylineBackdrop = "NONE"
            }
        val matching = theme { skylineBackdrop = "FOREST" }

        assertEquals(Skyline(StadiumBackdrop.MOUNTAINS, BackdropVariant.MOUNTAINS_DESERT), field.skyline(field.leftSide()))
        assertNull(field.skyline(field.rightSide()))
        assertEquals(StadiumBackdrop.FOREST, matching.skyline(matching.rightSide())?.backdrop)
    }

    @Test
    fun `only scenery can sit behind the stadium`() {
        assertNull(Skyline.from("CROWD"))
        assertNull(Skyline.from("FACILITY_BRICK"))
        assertNull(Skyline.from("HILL"))
    }

    @Test
    fun `board and topper colors fall back to the left side and ignore blanks`() {
        val field =
            theme {
                jumbotronColor = "#492f24"
                topperColor = "#ffc425"
                rightTopperColor = ""
            }

        assertEquals(Color(0x49, 0x2f, 0x24), field.jumbotronColor(field.rightSide()))
        assertNull(field.topperColor(field.rightSide()))
    }

    @Test
    fun `a full-width low building cancels the board over it but a tower ignores the setting`() {
        val low =
            theme {
                stadiumBackdrop = "FACILITY"
                backdropStyle = "FACILITY_NARROW_GEORGIAN"
                jumbotronOverBuilding = true
                facilityFullWidth = true
            }
        val tower =
            theme {
                stadiumBackdrop = "FACILITY"
                backdropStyle = "FACILITY_TOWER"
                jumbotronOverBuilding = true
                facilityFullWidth = true
            }

        assertFalse(low.jumbotronOverBuilding(low.leftSide()))
        assertTrue(tower.jumbotronOverBuilding(tower.leftSide()))
    }

    @Test
    fun `a bowl played at a venue uses that venue's stadium and titles the board with the bowl`() {
        val venue =
            StadiumDesign().apply {
                stadiumBackdrop = "MOUNTAINS"
                jumbotronEnabled = true
            }
        val bowl = BowlField().apply { bowl = "New Mexico Bowl" }
        val atVenue =
            FieldTheme(
                style = FieldStyle.BOWL,
                homeTeam = Team().apply { name = "Wyoming" },
                awayTeam = Team().apply { name = "Boise State" },
                centerLogoUrl = null,
                bowlField = bowl,
                stadium = venue,
            )
        val neutral = atVenue.copy(stadium = null)

        assertEquals(StadiumBackdrop.MOUNTAINS, atVenue.stadiumBackdrop(atVenue.leftSide()))
        assertTrue(atVenue.jumbotronEnabled(atVenue.leftSide()))
        assertEquals("NEW MEXICO BOWL", atVenue.scoreboardTitle())
        assertEquals(StadiumBackdrop.CROWD, neutral.stadiumBackdrop(neutral.leftSide()))
        assertFalse(neutral.jumbotronEnabled(neutral.leftSide()))
    }
}
