package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.animation.choreography.Celebration
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.ChoreographyPainter
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldCamera
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
import com.fcfb.arceus.service.fcfb.animation.choreography.script.BlockedKickPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.DeadBallPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.KickoffPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.KneelPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.OnsideKickPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.PassPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.PuntPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.RunPlayScript
import com.fcfb.arceus.service.fcfb.animation.choreography.script.ShortKickReturnScript
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
            AnimatedPlayType.SHORT_KICK_RETURN to ShortKickReturnScript(),
            AnimatedPlayType.KICKOFF to KickoffPlayScript(),
            AnimatedPlayType.ONSIDE_KICK to OnsideKickPlayScript(),
        )

    override fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        theme: FieldTheme,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
    ): List<BufferedImage> = renderFromProgress(play, startAbs, endAbs, theme, offensivePlaybook, defensivePlaybook, 0f)

    internal fun renderFromProgress(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        theme: FieldTheme,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        fromProgress: Float,
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
        val scripted = (scripts[classifier.classify(play)] ?: deadBall).choreograph(context)
        val choreography = if (play.actualResult in CELEBRATED_SCORES) Celebration.after(scripted, SCORE_AT) else scripted
        val helmets = matchupHelmets(play, theme, context.forward)
        val camera = FieldCamera(choreography.ball, lookAhead = context.forward * CAMERA_LOOK_AHEAD, flipped = theme.flipped)
        val field = FieldBackgroundPainter.paint(theme, camera.zoom)
        if (shouldDrawScrimmageLines(play.playCall) && play.actualResult != ActualResult.DELAY_OF_GAME) {
            FieldBackgroundPainter.drawScrimmageLines(field, startAbs, firstDownAbsFor(play, startAbs), camera.zoom, theme.flipped)
        }
        val extraFrames = ((ballTravelYards(choreography) - LONG_PLAY_YARDS) * EXTRA_FRAMES_PER_YARD).toInt().coerceIn(0, MAX_EXTRA_FRAMES)
        val celebrationFrames = if (play.actualResult in CELEBRATED_SCORES) CELEBRATION_FRAMES else 0
        return animationTimeline(extraFrames, celebrationFrames, choreography.endsAt)
            .filter { it >= fromProgress }
            .map { progress -> ChoreographyPainter.paint(field, camera, choreography, progress, helmets) }
    }

    private fun ballTravelYards(choreography: Choreography): Float {
        var total = 0f
        var previous = choreography.ball.at(0f).position
        for (step in 1..TRAVEL_SAMPLES) {
            val point = choreography.ball.at(step.toFloat() / TRAVEL_SAMPLES).position
            total += previous.distanceTo(point)
            previous = point
        }
        return total
    }

    private fun matchupHelmets(
        play: Play,
        theme: FieldTheme,
        forward: Float,
    ): MatchupHelmets {
        val (homeUniform, awayUniform) = theme.uniforms()
        val home = HelmetSprite.render(homeUniform.helmet, LogoLoader.load(theme.homeLogoUrl()), HELMET_SIZE)
        val away = HelmetSprite.render(awayUniform.helmet, LogoLoader.load(theme.awayLogoUrl()), HELMET_SIZE)
        val (offense, defense) = if (play.possession == TeamSide.HOME) home to away else away to home
        return MatchupHelmets(offense, defense, offenseFacesRight = forward > 0f)
    }

    companion object {
        private const val HELMET_SIZE = 37
        private const val LONG_PLAY_YARDS = 20f
        private const val EXTRA_FRAMES_PER_YARD = 0.8f
        private const val MAX_EXTRA_FRAMES = 96
        private const val CELEBRATION_FRAMES = 10
        private const val TRAVEL_SAMPLES = 120
        private const val CAMERA_LOOK_AHEAD = 4f
        private val CELEBRATED_SCORES =
            setOf(
                ActualResult.TOUCHDOWN,
                ActualResult.TURNOVER_TOUCHDOWN,
                ActualResult.RETURN_TOUCHDOWN,
                ActualResult.KICKING_TEAM_TOUCHDOWN,
                ActualResult.KICK_SIX,
                ActualResult.PUNT_RETURN_TOUCHDOWN,
                ActualResult.PUNT_TEAM_TOUCHDOWN,
                ActualResult.DEFENSE_TWO_POINT,
                ActualResult.SUCCESS,
            )
    }
}
