package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.io.File

/**
 * Renders sample GIFs straight through the real rendering pipeline (classifiers, renderers,
 * overlay painter, encoder) without a database or Spring context — for visually spot-checking
 * animation output on demand. Fetches the home team logo over the network for the wall-tiling
 * and midfield-logo previews, so this one test does hit the network; not `@Disabled` since it's
 * still safe to run as part of the normal suite, just meant to be run directly with `--tests`
 * to regenerate preview GIFs.
 */
class PlayAnimationPreviewTool {
    private val homeTeam =
        Team().apply {
            name = "Home Tigers"
            primaryColor = "#B8291F"
            secondaryColor = "#F2A900"
            scorebugLogo = "https://i.imgur.com/GR15ySn.png"
        }

    private val awayTeam =
        Team().apply {
            name = "Away Wolves"
            primaryColor = "#1F4EB8"
            scorebugLogo = null
        }

    private val classifier = PlayAnimationClassifier()
    private val overlayClassifier = PlayOutcomeOverlayClassifier()
    private val overlayPainter = OverlayPainter()
    private val encoder = AnimatedGifEncoder()

    private val renderers =
        mapOf(
            ShapeFamily.RUSH_ARC to RushArcFrameRenderer(),
            ShapeFamily.PASS_ARC to PassArcFrameRenderer(),
            ShapeFamily.INCOMPLETE_PASS to IncompletePassFrameRenderer(),
            ShapeFamily.KICK_ARC to KickArcFrameRenderer(),
            ShapeFamily.RETURN_ZIGZAG to ReturnZigzagFrameRenderer(),
            ShapeFamily.STATIC_SNAP to StaticSnapFrameRenderer(),
            ShapeFamily.ONSIDE_SCRAMBLE to OnsideScrambleFrameRenderer(),
            ShapeFamily.BLOCKED_STUFF to BlockedStuffFrameRenderer(),
            ShapeFamily.FIELD_GOAL_ATTEMPT to FieldGoalAttemptFrameRenderer(),
            ShapeFamily.KICKOFF_RETURN to KickoffReturnFrameRenderer(),
        )

    @Test
    fun generatePreviews() {
        val outputDir = File("animation-previews")
        outputDir.mkdirs()

        val scenarios =
            listOf(
                "run-short-gain" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 30, playCall = PlayCall.RUN, actualResult = ActualResult.GAIN, yards = 6)
                        .withEnd(36),
                "run-for-loss" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 40, playCall = PlayCall.RUN, actualResult = ActualResult.LOSS, yards = -3)
                        .withEnd(37),
                "pass-touchdown-deep-throw" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 20, playCall = PlayCall.PASS, actualResult = ActualResult.TOUCHDOWN, yards = 20)
                        .withEnd(50),
                "pass-incomplete" to
                    buildPlay(
                        possession = TeamSide.HOME,
                        ballLocation = 50,
                        playCall = PlayCall.PASS,
                        actualResult = ActualResult.NO_GAIN,
                        result = Scenario.INCOMPLETE,
                        yards = 0,
                    ).withEnd(50),
                "kickoff-return" to
                    buildPlay(
                        possession = TeamSide.HOME,
                        ballLocation = 35,
                        playCall = PlayCall.KICKOFF_NORMAL,
                        actualResult = ActualResult.KICKOFF,
                        yards = 20,
                    ).withEnd(80),
                "kickoff-return-touchdown" to
                    buildPlay(
                        possession = TeamSide.HOME,
                        ballLocation = 35,
                        playCall = PlayCall.KICKOFF_NORMAL,
                        actualResult = ActualResult.RETURN_TOUCHDOWN,
                        yards = 65,
                    ).withEnd(50),
                "kickoff-touchback" to
                    buildPlay(
                        possession = TeamSide.HOME,
                        ballLocation = 35,
                        playCall = PlayCall.KICKOFF_NORMAL,
                        actualResult = ActualResult.KICKOFF,
                        result = Scenario.TOUCHBACK,
                        yards = 0,
                    ).withEnd(25),
                "punt-with-return" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 40, playCall = PlayCall.PUNT, actualResult = ActualResult.PUNT, yards = 35)
                        .withEnd(55),
                "punt-blocked" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 40, playCall = PlayCall.PUNT, actualResult = ActualResult.BLOCKED, yards = 2)
                        .withEnd(42),
                "field-goal-good" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 75, playCall = PlayCall.FIELD_GOAL, actualResult = ActualResult.GOOD, yards = 0)
                        .withEnd(75),
                "field-goal-long" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 40, playCall = PlayCall.FIELD_GOAL, actualResult = ActualResult.GOOD, yards = 0)
                        .withEnd(40),
                "field-goal-no-good" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 75, playCall = PlayCall.FIELD_GOAL, actualResult = ActualResult.NO_GOOD, yards = 0)
                        .withEnd(75),
                "field-goal-blocked" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 75, playCall = PlayCall.FIELD_GOAL, actualResult = ActualResult.BLOCKED, yards = 0)
                        .withEnd(75),
                "pat-good" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 97, playCall = PlayCall.PAT, actualResult = ActualResult.GOOD, yards = 0)
                        .withEnd(97),
                "turnover" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 50, playCall = PlayCall.PASS, actualResult = ActualResult.TURNOVER, yards = 0)
                        .withEnd(48),
                "pick-six" to
                    buildPlay(
                        possession = TeamSide.HOME,
                        ballLocation = 45,
                        playCall = PlayCall.PASS,
                        actualResult = ActualResult.TURNOVER_TOUCHDOWN,
                        yards = 0,
                    ).withEnd(45),
                "safety" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 2, playCall = PlayCall.RUN, actualResult = ActualResult.SAFETY, yards = -2)
                        .withEnd(0),
                "onside-kick-recovery" to
                    buildPlay(
                        possession = TeamSide.HOME,
                        ballLocation = 35,
                        playCall = PlayCall.KICKOFF_ONSIDE,
                        actualResult = ActualResult.SUCCESSFUL_ONSIDE,
                        yards = 7,
                    ).withEnd(42),
                "formation-flexbone-vs-335" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 30, playCall = PlayCall.RUN, actualResult = ActualResult.GAIN, yards = 4)
                        .withEnd(34, OffensivePlaybook.FLEXBONE, DefensivePlaybook.THREE_THREE_FIVE),
                "formation-spread-sack" to
                    buildPlay(possession = TeamSide.HOME, ballLocation = 30, playCall = PlayCall.PASS, actualResult = ActualResult.LOSS, yards = -7)
                        .withEnd(23, OffensivePlaybook.SPREAD, DefensivePlaybook.THREE_FOUR),
            )

        scenarios.forEach { (name, play) ->
            val bytes = renderGif(play)
            File(outputDir, "$name.gif").writeBytes(bytes)
            assertTrue(bytes.isNotEmpty(), "Expected non-empty GIF bytes for scenario '$name'")
        }
    }

    private fun renderGif(play: PlayWithEnd): ByteArray {
        val startAbs = FieldCoordinateMapper.toAbsoluteFieldPosition(play.play.ballLocation, play.play.possession)
        val endAbs =
            if (play.play.actualResult == ActualResult.SAFETY) {
                ownGoalTargetFor(play.play.possession)
            } else {
                scoringTeamOrNull(play.play)?.let { endZoneTargetFor(it) } ?: play.fallbackEndAbs
            }

        val shapeFamily = classifier.classifyShape(play.play)
        val overlay = overlayClassifier.classifyOverlay(play.play)

        val renderer = renderers.getValue(shapeFamily)
        val frames =
            renderer.renderFrames(play.play, startAbs, endAbs, homeTeam, awayTeam, play.offensivePlaybook, play.defensivePlaybook)
        val decoratedFrames = overlayPainter.applyOverlay(frames, overlay, play.play, homeTeam, awayTeam)

        return encoder.encode(decoratedFrames, buildPalette())
    }

    private fun scoringTeamOrNull(play: Play): TeamSide? =
        when (play.actualResult) {
            ActualResult.TOUCHDOWN, ActualResult.KICKING_TEAM_TOUCHDOWN, ActualResult.PUNT_TEAM_TOUCHDOWN -> play.possession
            ActualResult.TURNOVER_TOUCHDOWN, ActualResult.RETURN_TOUCHDOWN, ActualResult.KICK_SIX,
            ActualResult.PUNT_RETURN_TOUCHDOWN, ActualResult.DEFENSE_TWO_POINT,
            -> if (play.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
            else -> null
        }

    private fun endZoneTargetFor(scoringTeam: TeamSide): Int = if (scoringTeam == TeamSide.HOME) 105 else -5

    private fun ownGoalTargetFor(possessor: TeamSide): Int = if (possessor == TeamSide.HOME) -5 else 105

    private fun buildPalette(): List<Color> =
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
        ) + GoalPostScenePainter.FAN_COLORS + GoalPostScenePainter.STAND_SHADES

    private fun buildPlay(
        possession: TeamSide,
        ballLocation: Int,
        playCall: PlayCall,
        actualResult: ActualResult,
        yards: Int,
        result: Scenario? = null,
    ): Play {
        val play = Play()
        play.playId = nextPlayId++
        play.gameId = 1
        play.possession = possession
        play.ballLocation = ballLocation
        play.down = 1
        play.yardsToGo = 10
        play.playCall = playCall
        play.result = result
        play.actualResult = actualResult
        play.yards = yards
        play.homeTeam = homeTeam.name.orEmpty()
        play.awayTeam = awayTeam.name.orEmpty()
        return play
    }

    private fun Play.withEnd(
        fallbackEndAbs: Int,
        offensivePlaybook: OffensivePlaybook = OffensivePlaybook.PRO,
        defensivePlaybook: DefensivePlaybook = DefensivePlaybook.FOUR_THREE,
    ) = PlayWithEnd(this, fallbackEndAbs, offensivePlaybook, defensivePlaybook)

    private data class PlayWithEnd(
        val play: Play,
        val fallbackEndAbs: Int,
        val offensivePlaybook: OffensivePlaybook,
        val defensivePlaybook: DefensivePlaybook,
    )

    private companion object {
        var nextPlayId = 1
    }
}
