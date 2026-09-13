package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

/** Phase 1 stub: reuses the kick arc shape until a dedicated onside-scramble path is built. */
@Component
class OnsideScrambleFrameRenderer(
    private val kickArcFrameRenderer: KickArcFrameRenderer,
) : PlayAnimationFrameRenderer {
    override fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        homeTeam: Team,
        awayTeam: Team,
    ): List<BufferedImage> = kickArcFrameRenderer.renderFrames(play, startAbs, endAbs, homeTeam, awayTeam)
}
