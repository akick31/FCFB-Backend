package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.animation.AnimatedPlayType
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.rendering.animation.AnimationFitter
import com.fcfb.arceus.rendering.animation.AnimationPalette
import com.fcfb.arceus.rendering.animation.PlayAnimationClassifier
import com.fcfb.arceus.rendering.animation.PlayOutcomeOverlayClassifier
import com.fcfb.arceus.rendering.animation.field.FieldCoordinateMapper
import com.fcfb.arceus.rendering.animation.fieldgoal.FieldGoalAttemptFrameRenderer
import com.fcfb.arceus.rendering.animation.fieldgoal.KickSixFrameRenderer
import com.fcfb.arceus.rendering.animation.frame.OverheadPlayFrameRenderer
import com.fcfb.arceus.rendering.animation.frame.OverlayPainter
import com.fcfb.arceus.service.fcfb.appearance.TeamUniformService
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.play.PlayService
import com.fcfb.arceus.service.fcfb.team.TeamService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class PlayAnimationService(
    private val playService: PlayService,
    private val gameService: GameService,
    private val teamService: TeamService,
    private val playAnimationClassifier: PlayAnimationClassifier,
    private val playOutcomeOverlayClassifier: PlayOutcomeOverlayClassifier,
    private val overlayPainter: OverlayPainter,
    private val animationFitter: AnimationFitter,
    private val overheadPlayFrameRenderer: OverheadPlayFrameRenderer,
    private val fieldGoalAttemptFrameRenderer: FieldGoalAttemptFrameRenderer,
    private val kickSixFrameRenderer: KickSixFrameRenderer,
    private val fieldThemeService: FieldThemeService,
    private val teamUniformService: TeamUniformService,
    @Value("\${animations.scrimmage-only:true}") private val scrimmageOnly: Boolean,
) {
    fun getPlayAnimationByPlayId(playId: Int): ResponseEntity<ByteArray> {
        val play = playService.getPlayById(playId)
        val game = gameService.getGameById(play.gameId)
        if (scrimmageOnly && game.gameType != GameType.SCRIMMAGE) {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
        val homeTeam = teamService.getTeamByName(play.homeTeam)
        val awayTeam = teamService.getTeamByName(play.awayTeam)

        val startAbs = FieldCoordinateMapper().toAbsoluteFieldPosition(play.ballLocation, play.possession)
        val endAbs = resultSpot(play, game, startAbs)

        val offensivePlaybook = if (play.possession == TeamSide.HOME) game.homeOffensivePlaybook else game.awayOffensivePlaybook
        val defensivePlaybook = if (play.possession == TeamSide.HOME) game.awayDefensivePlaybook else game.homeDefensivePlaybook

        val isKickSix = play.actualResult == ActualResult.KICK_SIX
        val isFieldGoal = playAnimationClassifier.classify(play) == AnimatedPlayType.FIELD_GOAL
        val renderer =
            when {
                isKickSix -> kickSixFrameRenderer
                isFieldGoal -> fieldGoalAttemptFrameRenderer
                else -> overheadPlayFrameRenderer
            }
        val homeUniform = teamUniformService.uniformFor(play.homeTeam, game.season, game.week)
        val awayUniform = teamUniformService.uniformFor(play.awayTeam, game.season, game.week)
        val theme = fieldThemeService.resolve(play, game, homeTeam, awayTeam, homeUniform, awayUniform)
        val frames = renderer.renderFrames(play, startAbs, endAbs, theme, offensivePlaybook, defensivePlaybook)
        val overlay = playOutcomeOverlayClassifier.classifyOverlay(play)
        val decoratedFrames = overlayPainter.applyOverlay(frames, overlay, play, homeTeam, awayTeam)

        val gifBytes = animationFitter.fit(decoratedFrames, AnimationPalette.forTheme(theme))

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.IMAGE_GIF
                contentLength = gifBytes.size.toLong()
            }
        return ResponseEntity(gifBytes, headers, HttpStatus.OK)
    }

    private fun resultSpot(
        play: Play,
        game: Game,
        startAbs: Int,
    ): Int {
        val twoPoint = play.playCall == PlayCall.TWO_POINT
        return when {
            play.actualResult == ActualResult.SAFETY -> ownGoalTargetFor(play.possession)
            twoPoint && play.actualResult == ActualResult.SUCCESS -> endZoneTargetFor(play.possession)
            twoPoint && play.actualResult == ActualResult.FAILED -> startAbs + if (play.possession == TeamSide.HOME) 1 else -1
            else -> scoringTeamOrNull(play)?.let { endZoneTargetFor(it) } ?: resolveEndAbsolutePosition(play, game)
        }
    }

    private fun resolveEndAbsolutePosition(
        play: Play,
        game: Game,
    ): Int {
        val nextPlay = playService.getPlayImmediatelyAfterOrNull(play.gameId, play.playId)
        if (nextPlay != null) {
            return FieldCoordinateMapper().toAbsoluteFieldPosition(nextPlay.ballLocation, nextPlay.possession)
        }
        return FieldCoordinateMapper().toAbsoluteFieldPosition(game.ballLocation, game.possession)
    }

    private fun scoringTeamOrNull(play: Play): TeamSide? =
        when (play.actualResult) {
            ActualResult.TOUCHDOWN, ActualResult.KICKING_TEAM_TOUCHDOWN, ActualResult.PUNT_TEAM_TOUCHDOWN -> play.possession
            ActualResult.TURNOVER_TOUCHDOWN, ActualResult.RETURN_TOUCHDOWN, ActualResult.KICK_SIX,
            ActualResult.PUNT_RETURN_TOUCHDOWN, ActualResult.DEFENSE_TWO_POINT,
            -> if (play.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
            else -> null
        }

    private fun endZoneTargetFor(scoringTeam: TeamSide): Int = if (scoringTeam == TeamSide.HOME) 100 + END_ZONE_CENTER else -END_ZONE_CENTER

    private fun ownGoalTargetFor(possessor: TeamSide): Int = if (possessor == TeamSide.HOME) -END_ZONE_CENTER else 100 + END_ZONE_CENTER

    companion object {
        private const val END_ZONE_CENTER = 5
    }
}
