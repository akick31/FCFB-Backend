package com.fcfb.arceus.service.fcfb.scorebug

import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

@Component
class PacTwelveScorebugRenderer(
    private val espnRenderer: EspnScorebugRenderer,
) : ScorebugRendererBase() {
    override fun render(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage {
        return espnRenderer.render(game, homeTeam, awayTeam)
    }
}
