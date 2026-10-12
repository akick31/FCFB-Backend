package com.fcfb.arceus.rendering.scorebug

import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

@Component
class PacTwelveScorebugRenderer(
    private val espnRenderer: EspnScorebugRenderer,
) : ScorebugRendererBase(), ScorebugRenderer {
    override fun render(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage {
        return espnRenderer.render(game, homeTeam, awayTeam)
    }
}
