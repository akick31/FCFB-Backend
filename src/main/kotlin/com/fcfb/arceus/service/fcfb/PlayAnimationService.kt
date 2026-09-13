package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.service.fcfb.animation.AnimatedGifEncoder
import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.service.fcfb.animation.FieldCoordinateMapper
import com.fcfb.arceus.service.fcfb.animation.KickArcFrameRenderer
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
    private val teamService: TeamService,
    private val playAnimationClassifier: PlayAnimationClassifier,
    private val playOutcomeOverlayClassifier: PlayOutcomeOverlayClassifier,
    private val overlayPainter: OverlayPainter,
    private val animatedGifEncoder: AnimatedGifEncoder,
    private val rushArcFrameRenderer: RushArcFrameRenderer,
    private val passArcFrameRenderer: PassArcFrameRenderer,
    private val kickArcFrameRenderer: KickArcFrameRenderer,
    private val returnZigzagFrameRenderer: ReturnZigzagFrameRenderer,
    private val staticSnapFrameRenderer: StaticSnapFrameRenderer,
    private val onsideScrambleFrameRenderer: OnsideScrambleFrameRenderer,
) {
    fun getPlayAnimationByPlayId(playId: Int): ResponseEntity<ByteArray> {
        val play = playService.getPlayById(playId)
        val homeTeam = teamService.getTeamByName(play.homeTeam)
        val awayTeam = teamService.getTeamByName(play.awayTeam)

        val startAbs = resolveStartAbsolutePosition(play)
        val endAbs = FieldCoordinateMapper.toAbsoluteFieldPosition(play.ballLocation, play.possession)

        val shapeFamily = playAnimationClassifier.classifyShape(play)
        val overlay = playOutcomeOverlayClassifier.classifyOverlay(play)

        val frames = frameRendererFor(shapeFamily).renderFrames(play, startAbs, endAbs, homeTeam, awayTeam)
        val decoratedFrames = overlayPainter.applyOverlay(frames, overlay, play, homeTeam, awayTeam)

        val gifBytes = animatedGifEncoder.encode(decoratedFrames, buildPalette(homeTeam, awayTeam))

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.IMAGE_GIF
                contentLength = gifBytes.size.toLong()
            }
        return ResponseEntity(gifBytes, headers, HttpStatus.OK)
    }

    private fun resolveStartAbsolutePosition(play: Play): Int {
        val previousPlay = playService.getPlayImmediatelyBeforeOrNull(play.gameId, play.playId)
        if (previousPlay == null) {
            // Opening kickoff of a game/half: no prior play row exists yet. The kicking team is
            // whichever side is NOT the current play's possession, since a kickoff row's possession
            // is set to the receiving team from creation (see PointAfterPlayProcessor/FieldGoalPlayProcessor,
            // which likewise pre-set ballLocation = 35 for the ensuing kickoff).
            val kickingTeam = if (play.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
            return FieldCoordinateMapper.toAbsoluteFieldPosition(OPENING_KICKOFF_YARD_LINE, kickingTeam)
        }
        val startBallLocation =
            if (previousPlay.possession == play.possession) previousPlay.ballLocation else 100 - previousPlay.ballLocation
        return FieldCoordinateMapper.toAbsoluteFieldPosition(startBallLocation, play.possession)
    }

    private fun frameRendererFor(shapeFamily: ShapeFamily): PlayAnimationFrameRenderer =
        when (shapeFamily) {
            ShapeFamily.RUSH_ARC -> rushArcFrameRenderer
            ShapeFamily.PASS_ARC -> passArcFrameRenderer
            ShapeFamily.KICK_ARC -> kickArcFrameRenderer
            ShapeFamily.RETURN_ZIGZAG -> returnZigzagFrameRenderer
            ShapeFamily.STATIC_SNAP -> staticSnapFrameRenderer
            ShapeFamily.ONSIDE_SCRAMBLE -> onsideScrambleFrameRenderer
        }

    private fun buildPalette(
        homeTeam: Team,
        awayTeam: Team,
    ): List<Color> =
        listOf(
            FieldBackgroundPainter.TURF_COLOR,
            FieldBackgroundPainter.LINE_COLOR,
            FieldBackgroundPainter.BALL_COLOR,
            Color.YELLOW,
            Color.BLACK,
            FieldBackgroundPainter.parseColor(homeTeam.primaryColor),
            FieldBackgroundPainter.parseColor(awayTeam.primaryColor),
        )

    companion object {
        private const val OPENING_KICKOFF_YARD_LINE = 35
    }
}
