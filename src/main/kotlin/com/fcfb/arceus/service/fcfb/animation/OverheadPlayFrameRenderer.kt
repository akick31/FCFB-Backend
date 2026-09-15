package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.service.fcfb.animation.choreography.ChoreographyPainter
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldCamera
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.BlockedKickPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.DeadBallPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.KickoffPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.KneelPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.OnsideKickPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.PassPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.PuntPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.RunPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.SpikePlayScript
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

@Component
class OverheadPlayFrameRenderer(
    private val classifier: PlayAnimationClassifier,
) : PlayAnimationFrameRenderer {
    private val deadBall = DeadBallPlayScript()
    private val scripts: Map<AnimatedPlayType, PlayScript> =
        mapOf(
            AnimatedPlayType.RUN to RunPlayScript(),
            AnimatedPlayType.PASS to PassPlayScript(),
            AnimatedPlayType.KNEEL to KneelPlayScript(),
            AnimatedPlayType.SPIKE to SpikePlayScript(),
            AnimatedPlayType.PUNT to PuntPlayScript(),
            AnimatedPlayType.BLOCKED_KICK to BlockedKickPlayScript(),
            AnimatedPlayType.KICKOFF to KickoffPlayScript(),
            AnimatedPlayType.ONSIDE_KICK to OnsideKickPlayScript(),
        )

    override fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        homeTeam: Team,
        awayTeam: Team,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
    ): List<BufferedImage> {
        val context =
            PlayContext(
                play = play,
                lineOfScrimmage = startAbs.toFloat(),
                endSpot = endAbs.toFloat(),
                forward = if (play.possession == TeamSide.HOME) 1f else -1f,
                side = if (play.playId % 2 == 0) 1f else -1f,
                offensivePlaybook = offensivePlaybook,
                defensivePlaybook = defensivePlaybook,
            )
        val choreography = (scripts[classifier.classify(play)] ?: deadBall).choreograph(context)
        val camera = FieldCamera(choreography.ball, lookAhead = context.forward * CAMERA_LOOK_AHEAD)
        val field = FieldBackgroundPainter.paint(homeTeam, awayTeam, camera.zoom)
        if (shouldDrawScrimmageLines(play.playCall)) {
            FieldBackgroundPainter.drawScrimmageLines(field, startAbs, firstDownAbsFor(play, startAbs), camera.zoom)
        }
        return animationTimeline().map { progress -> ChoreographyPainter.paint(field, camera, choreography, progress) }
    }

    companion object {
        private const val CAMERA_LOOK_AHEAD = 4f
    }
}
