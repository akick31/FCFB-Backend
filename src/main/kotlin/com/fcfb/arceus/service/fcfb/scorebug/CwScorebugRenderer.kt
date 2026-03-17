package com.fcfb.arceus.service.fcfb.scorebug

import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

/**
 * CW-style scorebug renderer.
 * Currently delegates to ESPN renderer; replace with custom CW design when ready.
 */
@Component
class CwScorebugRenderer(
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
