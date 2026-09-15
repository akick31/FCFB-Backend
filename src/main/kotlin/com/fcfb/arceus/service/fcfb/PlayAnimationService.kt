package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.service.fcfb.animation.AnimatedGifEncoder
import com.fcfb.arceus.service.fcfb.animation.BlockedStuffFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.service.fcfb.animation.FieldCoordinateMapper
import com.fcfb.arceus.service.fcfb.animation.FieldGoalAttemptFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.GoalPostScenePainter
import com.fcfb.arceus.service.fcfb.animation.IncompletePassFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.KickArcFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.KickoffReturnFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.OnsideScrambleFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.OverlayPainter
import com.fcfb.arceus.service.fcfb.animation.PassArcFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.PlayAnimationClassifier
import com.fcfb.arceus.service.fcfb.animation.PlayAnimationFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.PlayOutcomeOverlayClassifier
import com.fcfb.arceus.service.fcfb.animation.ReturnZigzagFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.RushArcFrameRenderer
import com.fcfb.arceus.service.fcfb.animation.ShapeFamily
import com.fcfb.arceus.service.fcfb.animation.StaticSnapFrameRenderer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.Color

@Service
class PlayAnimationService(
    private val playService: PlayService,
    private val gameService: GameService,
    private val teamService: TeamService,
    private val playAnimationClassifier: PlayAnimationClassifier,
    private val playOutcomeOverlayClassifier: PlayOutcomeOverlayClassifier,
    private val overlayPainter: OverlayPainter,
    private val animatedGifEncoder: AnimatedGifEncoder,
    private val rushArcFrameRenderer: RushArcFrameRenderer,
    private val passArcFrameRenderer: PassArcFrameRenderer,
    private val incompletePassFrameRenderer: IncompletePassFrameRenderer,
    private val kickArcFrameRenderer: KickArcFrameRenderer,
    private val returnZigzagFrameRenderer: ReturnZigzagFrameRenderer,
    private val staticSnapFrameRenderer: StaticSnapFrameRenderer,
    private val onsideScrambleFrameRenderer: OnsideScrambleFrameRenderer,
    private val blockedStuffFrameRenderer: BlockedStuffFrameRenderer,
    private val fieldGoalAttemptFrameRenderer: FieldGoalAttemptFrameRenderer,
    private val kickoffReturnFrameRenderer: KickoffReturnFrameRenderer,
) {
    fun getPlayAnimationByPlayId(playId: Int): ResponseEntity<ByteArray> {
        val play = playService.getPlayById(playId)
        val homeTeam = teamService.getTeamByName(play.homeTeam)
        val awayTeam = teamService.getTeamByName(play.awayTeam)
        val game = gameService.getGameById(play.gameId)

        val startAbs = FieldCoordinateMapper.toAbsoluteFieldPosition(play.ballLocation, play.possession)
        val endAbs =
            if (play.actualResult == ActualResult.SAFETY) {
                ownGoalTargetFor(play.possession)
            } else {
                scoringTeamOrNull(play)?.let { endZoneTargetFor(it) } ?: resolveEndAbsolutePosition(play, game)
            }

        val offensivePlaybook = if (play.possession == TeamSide.HOME) game.homeOffensivePlaybook else game.awayOffensivePlaybook
        val defensivePlaybook = if (play.possession == TeamSide.HOME) game.awayDefensivePlaybook else game.homeDefensivePlaybook

        val shapeFamily = playAnimationClassifier.classifyShape(play)
        val overlay = playOutcomeOverlayClassifier.classifyOverlay(play)

        val frames =
            frameRendererFor(shapeFamily)
                .renderFrames(play, startAbs, endAbs, homeTeam, awayTeam, offensivePlaybook, defensivePlaybook)
        val decoratedFrames = overlayPainter.applyOverlay(frames, overlay, play, homeTeam, awayTeam)

        val gifBytes = animatedGifEncoder.encode(decoratedFrames, buildPalette(homeTeam, awayTeam))

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.IMAGE_GIF
                contentLength = gifBytes.size.toLong()
            }
        return ResponseEntity(gifBytes, headers, HttpStatus.OK)
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

    private fun endZoneTargetFor(scoringTeam: TeamSide): Int =
        if (scoringTeam == TeamSide.HOME) 100 + END_ZONE_CENTER else -END_ZONE_CENTER

    /**
     * A safety is scored by the defense, but the ball ends up in the tackled OFFENSE's own end
     * zone — the opposite direction from [endZoneTargetFor], which targets the scoring team's
     * opponent's goal.
     */
    private fun ownGoalTargetFor(possessor: TeamSide): Int =
        if (possessor == TeamSide.HOME) -END_ZONE_CENTER else 100 + END_ZONE_CENTER

    private fun frameRendererFor(shapeFamily: ShapeFamily): PlayAnimationFrameRenderer =
        when (shapeFamily) {
            ShapeFamily.RUSH_ARC -> rushArcFrameRenderer
            ShapeFamily.PASS_ARC -> passArcFrameRenderer
            ShapeFamily.INCOMPLETE_PASS -> incompletePassFrameRenderer
            ShapeFamily.KICK_ARC -> kickArcFrameRenderer
            ShapeFamily.RETURN_ZIGZAG -> returnZigzagFrameRenderer
            ShapeFamily.STATIC_SNAP -> staticSnapFrameRenderer
            ShapeFamily.ONSIDE_SCRAMBLE -> onsideScrambleFrameRenderer
            ShapeFamily.BLOCKED_STUFF -> blockedStuffFrameRenderer
            ShapeFamily.FIELD_GOAL_ATTEMPT -> fieldGoalAttemptFrameRenderer
            ShapeFamily.KICKOFF_RETURN -> kickoffReturnFrameRenderer
        }

    private fun buildPalette(
        homeTeam: Team,
        awayTeam: Team,
    ): List<Color> =
        listOf(
            FieldBackgroundPainter.TURF_COLOR,
            FieldBackgroundPainter.LINE_COLOR,
            FieldBackgroundPainter.BALL_COLOR,
            FieldBackgroundPainter.LOS_COLOR,
            FieldBackgroundPainter.FIRST_DOWN_COLOR,
            Color.BLACK,
            FieldBackgroundPainter.parseColor(homeTeam.primaryColor),
            FieldBackgroundPainter.parseColor(awayTeam.primaryColor),
            FieldBackgroundPainter.parseColor(homeTeam.secondaryColor),
            GoalPostScenePainter.SKY_COLOR,
            GoalPostScenePainter.POST_COLOR,
            GoalPostScenePainter.DEFENDER_COLOR,
            GoalPostScenePainter.NET_COLOR,
        ) + GoalPostScenePainter.FAN_COLORS + GoalPostScenePainter.STAND_SHADES

    companion object {
        private const val END_ZONE_CENTER = 5
    }
}
