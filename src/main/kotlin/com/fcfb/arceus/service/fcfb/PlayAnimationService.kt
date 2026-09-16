package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.animation.AnimatedPlayType
import com.fcfb.arceus.service.fcfb.animation.AnimationFitter
import com.fcfb.arceus.service.fcfb.animation.AnimationPalette
import com.fcfb.arceus.service.fcfb.animation.FieldCoordinateMapper
import com.fcfb.arceus.service.fcfb.animation.FieldGoalAttemptFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.FieldThemeResolver
import com.fcfb.arceus.service.fcfb.animation.OverheadPlayFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.OverlayPainter
import com.fcfb.arceus.service.fcfb.animation.PlayAnimationClassifier
import com.fcfb.arceus.service.fcfb.animation.PlayOutcomeOverlayClassifier
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
    private val fieldThemeResolver: FieldThemeResolver,
    private val teamUniformService: TeamUniformService,
) {
    fun getPlayAnimationByPlayId(playId: Int): ResponseEntity<ByteArray> {
        val play = playService.getPlayById(playId)
        val homeTeam = teamService.getTeamByName(play.homeTeam)
        val awayTeam = teamService.getTeamByName(play.awayTeam)
        val game = gameService.getGameById(play.gameId)

        val startAbs = FieldCoordinateMapper.toAbsoluteFieldPosition(play.ballLocation, play.possession)
        val endAbs = resultSpot(play, game, startAbs)

        val offensivePlaybook = if (play.possession == TeamSide.HOME) game.homeOffensivePlaybook else game.awayOffensivePlaybook
        val defensivePlaybook = if (play.possession == TeamSide.HOME) game.awayDefensivePlaybook else game.homeDefensivePlaybook

        val isFieldGoal = playAnimationClassifier.classify(play) == AnimatedPlayType.FIELD_GOAL
        val renderer = if (isFieldGoal) fieldGoalAttemptFrameRenderer else overheadPlayFrameRenderer
        val homeUniform = teamUniformService.uniformFor(play.homeTeam, game.season, game.week)
        val awayUniform = teamUniformService.uniformFor(play.awayTeam, game.season, game.week)
        val theme = fieldThemeResolver.resolve(play, game, homeTeam, awayTeam, homeUniform, awayUniform)
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

    /**
     * `Play.ballLocation`/`possession` are set once at play creation and never updated, so they
     * always hold the play's start, never its result. The result only ever lands on the next
     * play's own start fields, or on `Game` directly if this is the game's most recent play.
     */
    private fun resolveEndAbsolutePosition(
        play: Play,
        game: Game,
    ): Int {
        val nextPlay = playService.getPlayImmediatelyAfterOrNull(play.gameId, play.playId)
        if (nextPlay != null) {
            return FieldCoordinateMapper.toAbsoluteFieldPosition(nextPlay.ballLocation, nextPlay.possession)
        }
        return FieldCoordinateMapper.toAbsoluteFieldPosition(game.ballLocation, game.possession)
    }

    /**
     * By the time this endpoint is called for a scoring play, `Game` has already moved on to the
     * PAT/kickoff setup, so [resolveEndAbsolutePosition]'s fallback no longer reflects "the ball
     * reached the end zone." The scoring team isn't always `play.possession` either — a defensive
     * or return score is scored by the opposite side.
     */
    private fun scoringTeamOrNull(play: Play): TeamSide? =
        when (play.actualResult) {
            ActualResult.TOUCHDOWN, ActualResult.KICKING_TEAM_TOUCHDOWN, ActualResult.PUNT_TEAM_TOUCHDOWN -> play.possession
            ActualResult.TURNOVER_TOUCHDOWN, ActualResult.RETURN_TOUCHDOWN, ActualResult.KICK_SIX,
            ActualResult.PUNT_RETURN_TOUCHDOWN, ActualResult.DEFENSE_TWO_POINT,
            -> if (play.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
            else -> null
        }

    private fun endZoneTargetFor(scoringTeam: TeamSide): Int = if (scoringTeam == TeamSide.HOME) 100 + END_ZONE_CENTER else -END_ZONE_CENTER

    /**
     * A safety is scored by the defense, but the ball ends up in the tackled OFFENSE's own end
     * zone — the opposite direction from [endZoneTargetFor], which targets the scoring team's
     * opponent's goal.
     */
    private fun ownGoalTargetFor(possessor: TeamSide): Int = if (possessor == TeamSide.HOME) -END_ZONE_CENTER else 100 + END_ZONE_CENTER

    companion object {
        private const val END_ZONE_CENTER = 5
    }
}
