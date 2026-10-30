package com.fcfb.arceus.rendering.preview

import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.enums.animation.StadiumPreviewView
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.appearance.StadiumDesign
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.rendering.animation.fieldgoal.GoalPostScenePainter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StadiumPreviewRendererTest {
    @Test
    fun `the close and long kick rows and the two ends are separated by a light divider`() {
        val theme =
            FieldTheme(
                style = FieldStyle.HOME_FIELD,
                homeTeam =
                    Team().apply {
                        name = "Wyoming"
                        primaryColor = "#492f24"
                        secondaryColor = "#ffc425"
                    },
                awayTeam =
                    Team().apply {
                        name = "Boise State"
                        primaryColor = "#0033a0"
                        secondaryColor = "#d64309"
                    },
                centerLogoUrl = null,
                stadium = StadiumDesign(),
            )

        val sheet = StadiumPreviewRenderer.closeAndLongBothEnds(theme)

        val viewHeight = (sheet.height - StadiumPreviewRenderer.GUTTER) / 2
        val viewWidth = (sheet.width - StadiumPreviewRenderer.GUTTER) / 2
        val middle = StadiumPreviewRenderer.GUTTER / 2
        val divider = StadiumPreviewRenderer.DIVIDER_COLOR.rgb
        assertEquals(divider, sheet.getRGB(sheet.width / 4, viewHeight + middle))
        assertEquals(divider, sheet.getRGB(viewWidth + middle, viewHeight / 2))
    }

    @Test
    fun `one end shows its close and long kick stacked, and a scoreboard view zooms in on the board`() {
        val theme =
            FieldTheme(
                style = FieldStyle.HOME_FIELD,
                homeTeam =
                    Team().apply {
                        name = "Wyoming"
                        primaryColor = "#492f24"
                        secondaryColor = "#ffc425"
                    },
                awayTeam =
                    Team().apply {
                        name = "Boise State"
                        primaryColor = "#0033a0"
                        secondaryColor = "#d64309"
                    },
                centerLogoUrl = null,
                stadium =
                    StadiumDesign().apply {
                        stadiumSize = 0.6
                        jumbotronEnabled = true
                        rightStadiumBackdrop = "CROWD"
                    },
            )

        val oneEnd = StadiumPreviewRenderer.render(theme, StadiumPreviewView.LEFT)
        val closeUp = StadiumPreviewRenderer.render(theme, StadiumPreviewView.LEFT_SCOREBOARD)
        val noBoard = StadiumPreviewRenderer.render(theme, StadiumPreviewView.RIGHT_SCOREBOARD)

        assertEquals(GoalPostScenePainter.SCENE_WIDTH, oneEnd.width)
        assertTrue(oneEnd.height > oneEnd.width / 2)
        assertEquals(600, closeUp.width)
        assertTrue(closeUp.height < oneEnd.height)
        assertEquals(oneEnd.height, noBoard.height)
    }
}
