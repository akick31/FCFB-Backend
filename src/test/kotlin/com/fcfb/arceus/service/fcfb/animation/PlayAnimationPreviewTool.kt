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
 * Renders sample GIFs straight through the real rendering pipeline without a database or Spring
 * context, for visually spot-checking animation output. Fetches the home team logo over the network.
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
    private val overheadRenderer = OverheadPlayFrameRenderer(classifier)
    private val fieldGoalRenderer = FieldGoalAttemptFrameRenderer()

    @Test
    fun generatePreviews() {
        val outputDir = File("animation-previews")
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        scenarios().forEach { (name, preview) ->
            val bytes = renderGif(preview)
            File(outputDir, "$name.gif").writeBytes(bytes)
            assertTrue(bytes.isNotEmpty(), "Expected non-empty GIF bytes for scenario '$name'")
        }
    }

    private fun scenarios(): List<Pair<String, Preview>> =
        listOf(
            "run-pro-short-gain" to play(TeamSide.HOME, 30, PlayCall.RUN, ActualResult.GAIN).endingAt(36),
            "run-flexbone-vs-335" to
                play(TeamSide.HOME, 30, PlayCall.RUN, ActualResult.FIRST_DOWN)
                    .endingAt(42, OffensivePlaybook.FLEXBONE, DefensivePlaybook.THREE_THREE_FIVE),
            "run-for-loss" to play(TeamSide.HOME, 40, PlayCall.RUN, ActualResult.LOSS).endingAt(37),
            "run-touchdown-away" to
                play(TeamSide.AWAY, 88, PlayCall.RUN, ActualResult.TOUCHDOWN)
                    .endingAt(0, OffensivePlaybook.SPREAD, DefensivePlaybook.FOUR_FOUR),
            "run-fumble" to play(TeamSide.HOME, 45, PlayCall.RUN, ActualResult.TURNOVER).endingAt(49),
            "safety" to play(TeamSide.HOME, 2, PlayCall.RUN, ActualResult.SAFETY).endingAt(0),
            "pass-short-completion" to play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.GAIN).endingAt(38, OffensivePlaybook.WEST_COAST),
            "pass-deep-touchdown" to
                play(TeamSide.HOME, 20, PlayCall.PASS, ActualResult.TOUCHDOWN)
                    .endingAt(0, OffensivePlaybook.AIR_RAID, DefensivePlaybook.THREE_FOUR),
            "pass-incomplete" to
                play(TeamSide.HOME, 50, PlayCall.PASS, ActualResult.NO_GAIN, Scenario.INCOMPLETE)
                    .endingAt(50, OffensivePlaybook.SPREAD),
            "pass-sack" to
                play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.LOSS)
                    .endingAt(23, OffensivePlaybook.SPREAD, DefensivePlaybook.THREE_FOUR),
            "pass-interception" to play(TeamSide.HOME, 50, PlayCall.PASS, ActualResult.TURNOVER).endingAt(55),
            "pass-pick-six" to
                play(TeamSide.HOME, 45, PlayCall.PASS, ActualResult.TURNOVER_TOUCHDOWN)
                    .endingAt(0, OffensivePlaybook.AIR_RAID),
            "kneel" to play(TeamSide.HOME, 40, PlayCall.KNEEL, ActualResult.KNEEL).endingAt(38),
            "spike" to play(TeamSide.HOME, 60, PlayCall.SPIKE, ActualResult.SPIKE).endingAt(60),
            "punt-with-return" to play(TeamSide.HOME, 30, PlayCall.PUNT, ActualResult.PUNT).endingAt(68),
            "punt-return-touchdown" to play(TeamSide.HOME, 30, PlayCall.PUNT, ActualResult.PUNT_RETURN_TOUCHDOWN).endingAt(0),
            "punt-blocked" to play(TeamSide.HOME, 40, PlayCall.PUNT, ActualResult.BLOCKED).endingAt(28),
            "kickoff-return" to play(TeamSide.HOME, 35, PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF).endingAt(72),
            "kickoff-return-touchdown" to play(TeamSide.HOME, 35, PlayCall.KICKOFF_NORMAL, ActualResult.RETURN_TOUCHDOWN).endingAt(0),
            "kickoff-touchback" to play(TeamSide.HOME, 35, PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF, Scenario.TOUCHBACK).endingAt(75),
            "onside-kick-recovered" to play(TeamSide.HOME, 35, PlayCall.KICKOFF_ONSIDE, ActualResult.SUCCESSFUL_ONSIDE).endingAt(46),
            "field-goal-good" to play(TeamSide.HOME, 75, PlayCall.FIELD_GOAL, ActualResult.GOOD).endingAt(75),
            "field-goal-no-good" to play(TeamSide.HOME, 60, PlayCall.FIELD_GOAL, ActualResult.NO_GOOD).endingAt(60),
        )

    private fun renderGif(preview: Preview): ByteArray {
        val play = preview.play
        val startAbs = FieldCoordinateMapper.toAbsoluteFieldPosition(play.ballLocation, play.possession)
        val endAbs =
            when {
                play.actualResult == ActualResult.SAFETY -> if (play.possession == TeamSide.HOME) -5 else 105
                else -> scoringTeamOrNull(play)?.let { if (it == TeamSide.HOME) 105 else -5 } ?: preview.endAbs
            }
        val renderer = if (classifier.classify(play) == AnimatedPlayType.FIELD_GOAL) fieldGoalRenderer else overheadRenderer
        val frames = renderer.renderFrames(play, startAbs, endAbs, homeTeam, awayTeam, preview.offensivePlaybook, preview.defensivePlaybook)
        val decoratedFrames = overlayPainter.applyOverlay(frames, overlayClassifier.classifyOverlay(play), play, homeTeam, awayTeam)
        return encoder.encode(decoratedFrames, palette())
    }

    private fun scoringTeamOrNull(play: Play): TeamSide? =
        when (play.actualResult) {
            ActualResult.TOUCHDOWN, ActualResult.KICKING_TEAM_TOUCHDOWN, ActualResult.PUNT_TEAM_TOUCHDOWN -> play.possession
            ActualResult.TURNOVER_TOUCHDOWN, ActualResult.RETURN_TOUCHDOWN, ActualResult.KICK_SIX,
            ActualResult.PUNT_RETURN_TOUCHDOWN, ActualResult.DEFENSE_TWO_POINT,
            -> if (play.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
            else -> null
        }

    private fun palette(): List<Color> =
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

    private fun play(
        possession: TeamSide,
        ballLocation: Int,
        playCall: PlayCall,
        actualResult: ActualResult,
        result: Scenario? = null,
    ): Play =
        Play().apply {
            playId = nextPlayId++
            gameId = 1
            this.possession = possession
            this.ballLocation = ballLocation
            down = 1
            yardsToGo = 10
            this.playCall = playCall
            this.result = result
            this.actualResult = actualResult
            homeTeam = this@PlayAnimationPreviewTool.homeTeam.name.orEmpty()
            awayTeam = this@PlayAnimationPreviewTool.awayTeam.name.orEmpty()
        }

    private fun Play.endingAt(
        endAbs: Int,
        offensivePlaybook: OffensivePlaybook = OffensivePlaybook.PRO,
        defensivePlaybook: DefensivePlaybook = DefensivePlaybook.FOUR_THREE,
    ) = Preview(this, endAbs, offensivePlaybook, defensivePlaybook)

    private data class Preview(
        val play: Play,
        val endAbs: Int,
        val offensivePlaybook: OffensivePlaybook,
        val defensivePlaybook: DefensivePlaybook,
    )

    private companion object {
        var nextPlayId = 1
    }
}
