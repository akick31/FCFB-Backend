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
import java.io.File

class PlayAnimationPreviewTool {
    private val homeTeam =
        Team().apply {
            name = "Duluth"
            primaryColor = "#7DB9D7"
            secondaryColor = "#1A7850"
            scorebugLogo = "https://i.imgur.com/GR15ySn.png"
            conference = "BIG_TEN"
        }

    private val awayTeam =
        Team().apply {
            name = "Wyoming"
            primaryColor = "#ffc425"
            secondaryColor = "#492f24"
            scorebugLogo = "https://a.espncdn.com/i/teamlogos/ncaa/500/2751.png"
            conference = "MOUNTAIN_WEST"
        }

    private val classifier = PlayAnimationClassifier()
    private val overlayClassifier = PlayOutcomeOverlayClassifier()
    private val overlayPainter = OverlayPainter()
    private val encoder = AnimationFitter(AnimatedGifEncoder())
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
        FieldStyle.entries.forEach { style ->
            javax.imageio.ImageIO.write(
                FieldBackgroundPainter.paint(themeFor(style)),
                "png",
                File(outputDir, "field-${style.name.lowercase()}.png"),
            )
        }
        val helmet =
            HelmetSprite.render(
                FieldBackgroundPainter.parseColor(awayTeam.primaryColor),
                LogoLoader.load(awayTeam.scorebugLogo),
                HELMET_PREVIEW_SIZE,
            )
        javax.imageio.ImageIO.write(helmet.facingRight, "png", File(outputDir, "helmet.png"))
        javax.imageio.ImageIO.write(FieldBackgroundPainter.paint(boiseStateField()), "png", File(outputDir, "field-boise-state.png"))
        javax.imageio.ImageIO.write(
            FieldBackgroundPainter.paint(themeFor(FieldStyle.BOWL).copy(flipped = true)),
            "png",
            File(outputDir, "field-second-half.png"),
        )
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
            "run-fumble" to play(TeamSide.HOME, 45, PlayCall.RUN, ActualResult.TURNOVER, Scenario.TURNOVER).endingAt(45),
            "run-fumble-plus-10" to
                play(TeamSide.HOME, 45, PlayCall.RUN, ActualResult.TURNOVER, Scenario.TURNOVER_PLUS_10_YARDS).endingAt(35),
            "run-fumble-minus-10" to
                play(TeamSide.HOME, 45, PlayCall.RUN, ActualResult.TURNOVER, Scenario.TURNOVER_MINUS_10_YARDS).endingAt(65),
            "run-fumble-touchdown" to
                play(TeamSide.HOME, 45, PlayCall.RUN, ActualResult.TURNOVER_TOUCHDOWN, Scenario.TURNOVER_TOUCHDOWN).endingAt(0),
            "safety-run" to play(TeamSide.HOME, 4, PlayCall.RUN, ActualResult.SAFETY).endingAt(-5),
            "safety-sack" to play(TeamSide.AWAY, 6, PlayCall.PASS, ActualResult.SAFETY).endingAt(105, OffensivePlaybook.AIR_RAID),
            "pass-short-completion" to play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.GAIN).endingAt(38, OffensivePlaybook.WEST_COAST),
            "pass-deep-touchdown" to
                play(TeamSide.HOME, 20, PlayCall.PASS, ActualResult.TOUCHDOWN)
                    .endingAt(100, OffensivePlaybook.AIR_RAID, DefensivePlaybook.THREE_FOUR),
            "pass-touchdown-wide-open" to
                play(TeamSide.HOME, 25, PlayCall.PASS, ActualResult.TOUCHDOWN, forcedPlayId = DEEP_SCORE_OPEN_PLAY_ID)
                    .endingAt(100, OffensivePlaybook.AIR_RAID, DefensivePlaybook.THREE_FOUR),
            "pass-touchdown-in-stride" to
                play(TeamSide.HOME, 25, PlayCall.PASS, ActualResult.TOUCHDOWN, forcedPlayId = DEEP_SCORE_STRIDE_PLAY_ID)
                    .endingAt(100, OffensivePlaybook.AIR_RAID, DefensivePlaybook.THREE_FOUR),
            "pass-touchdown-hail-mary" to
                play(TeamSide.HOME, 55, PlayCall.PASS, ActualResult.TOUCHDOWN, forcedPlayId = HAIL_MARY_PLAY_ID)
                    .endingAt(100, OffensivePlaybook.AIR_RAID, DefensivePlaybook.THREE_FOUR),
            "pass-deep-contested" to
                play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.FIRST_DOWN, forcedPlayId = CONTESTED_PLAY_ID)
                    .endingAt(85, OffensivePlaybook.AIR_RAID),
            "pass-short-broken-big-gain" to
                play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.FIRST_DOWN, forcedPlayId = RUN_DOWN_PLAY_ID)
                    .endingAt(85, OffensivePlaybook.WEST_COAST),
            "pass-incomplete" to
                play(TeamSide.HOME, 50, PlayCall.PASS, ActualResult.NO_GAIN, Scenario.INCOMPLETE)
                    .endingAt(50, OffensivePlaybook.SPREAD),
            "pass-incomplete-broken-up" to
                play(TeamSide.HOME, 50, PlayCall.PASS, ActualResult.NO_GAIN, Scenario.INCOMPLETE, forcedPlayId = BROKEN_UP_PLAY_ID)
                    .endingAt(50, OffensivePlaybook.SPREAD),
            "pass-sack" to
                play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.LOSS)
                    .endingAt(23, OffensivePlaybook.SPREAD, DefensivePlaybook.THREE_FOUR),
            "pass-interception" to play(TeamSide.HOME, 50, PlayCall.PASS, ActualResult.TURNOVER).endingAt(55),
            "pass-pick-six" to
                play(TeamSide.HOME, 45, PlayCall.PASS, ActualResult.TURNOVER_TOUCHDOWN)
                    .endingAt(0, OffensivePlaybook.AIR_RAID),
            "two-point-pass" to
                play(TeamSide.HOME, 97, PlayCall.TWO_POINT, ActualResult.SUCCESS, forcedPlayId = TWO_POINT_PASS_PLAY_ID)
                    .endingAt(100, OffensivePlaybook.AIR_RAID),
            "two-point-run" to
                play(TeamSide.HOME, 97, PlayCall.TWO_POINT, ActualResult.SUCCESS, forcedPlayId = TWO_POINT_RUN_PLAY_ID)
                    .endingAt(100, OffensivePlaybook.PRO),
            "two-point-failed" to
                play(TeamSide.HOME, 97, PlayCall.TWO_POINT, ActualResult.FAILED, forcedPlayId = TWO_POINT_PASS_PLAY_ID)
                    .endingAt(98, OffensivePlaybook.AIR_RAID),
            "field-goal-near-miss-block" to
                play(TeamSide.HOME, 70, PlayCall.FIELD_GOAL, ActualResult.GOOD, forcedPlayId = NEAR_MISS_PLAY_ID).endingAt(70),
            "delay-of-game" to play(TeamSide.HOME, 40, PlayCall.RUN, ActualResult.DELAY_OF_GAME).endingAt(35),
            "kneel" to play(TeamSide.HOME, 40, PlayCall.KNEEL, ActualResult.KNEEL).endingAt(38),
            "spike" to play(TeamSide.HOME, 60, PlayCall.SPIKE, ActualResult.SPIKE).endingAt(60),
            "punt-with-return" to play(TeamSide.HOME, 30, PlayCall.PUNT, ActualResult.PUNT).endingAt(68),
            "punt-return-touchdown" to play(TeamSide.HOME, 30, PlayCall.PUNT, ActualResult.PUNT_RETURN_TOUCHDOWN).endingAt(0),
            "punt-blocked" to play(TeamSide.HOME, 40, PlayCall.PUNT, ActualResult.BLOCKED).endingAt(28),
            "kickoff-return" to play(TeamSide.HOME, 35, PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF).endingAt(72),
            "kickoff-return-sideline" to
                play(TeamSide.HOME, 35, PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF, forcedPlayId = RETURN_SIDELINE_PLAY_ID)
                    .endingAt(72),
            "kickoff-return-middle" to
                play(TeamSide.HOME, 35, PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF, forcedPlayId = RETURN_MIDDLE_PLAY_ID)
                    .endingAt(72),
            "kickoff-return-cutback" to
                play(TeamSide.HOME, 35, PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF, forcedPlayId = RETURN_CUTBACK_PLAY_ID)
                    .endingAt(72),
            "kickoff-return-touchdown" to play(TeamSide.HOME, 35, PlayCall.KICKOFF_NORMAL, ActualResult.RETURN_TOUCHDOWN).endingAt(0),
            "kickoff-touchback" to play(TeamSide.HOME, 35, PlayCall.KICKOFF_NORMAL, ActualResult.KICKOFF, Scenario.TOUCHBACK).endingAt(75),
            "onside-kick-recovered" to play(TeamSide.HOME, 35, PlayCall.KICKOFF_ONSIDE, ActualResult.SUCCESSFUL_ONSIDE).endingAt(46),
            "onside-kick-failed" to play(TeamSide.HOME, 35, PlayCall.KICKOFF_ONSIDE, ActualResult.FAILED_ONSIDE).endingAt(47),
            "onside-kick-failed-2" to
                play(TeamSide.HOME, 35, PlayCall.KICKOFF_ONSIDE, ActualResult.FAILED_ONSIDE, forcedPlayId = ONSIDE_ALT_PLAY_ID)
                    .endingAt(47),
            "onside-kick-recovered-2" to
                play(TeamSide.HOME, 35, PlayCall.KICKOFF_ONSIDE, ActualResult.SUCCESSFUL_ONSIDE, forcedPlayId = ONSIDE_ALT_PLAY_ID)
                    .endingAt(46),
            "field-goal-good" to play(TeamSide.HOME, 75, PlayCall.FIELD_GOAL, ActualResult.GOOD).endingAt(75),
            "field-goal-no-good" to play(TeamSide.HOME, 60, PlayCall.FIELD_GOAL, ActualResult.NO_GOOD).endingAt(60),
            "field-goal-no-good-2" to play(TeamSide.AWAY, 70, PlayCall.FIELD_GOAL, ActualResult.NO_GOOD).endingAt(30),
            "field-goal-no-good-3" to play(TeamSide.HOME, 65, PlayCall.FIELD_GOAL, ActualResult.NO_GOOD).endingAt(65),
            "field-goal-no-good-4" to play(TeamSide.HOME, 72, PlayCall.FIELD_GOAL, ActualResult.NO_GOOD).endingAt(72),
            "field-goal-good-2" to play(TeamSide.AWAY, 68, PlayCall.FIELD_GOAL, ActualResult.GOOD).endingAt(32),
            "field-goal-blocked" to play(TeamSide.HOME, 70, PlayCall.FIELD_GOAL, ActualResult.BLOCKED).endingAt(70),
            "extra-point-good" to play(TeamSide.HOME, 97, PlayCall.PAT, ActualResult.GOOD).endingAt(97),
            "extra-point-blocked" to play(TeamSide.AWAY, 97, PlayCall.PAT, ActualResult.BLOCKED).endingAt(3),
            "field-goal-from-the-2" to play(TeamSide.HOME, 98, PlayCall.FIELD_GOAL, ActualResult.GOOD).endingAt(98),
            "field-goal-long-no-good" to play(TeamSide.AWAY, 42, PlayCall.FIELD_GOAL, ActualResult.NO_GOOD).endingAt(58),
            "field-goal-playoff" to
                play(TeamSide.HOME, 75, PlayCall.FIELD_GOAL, ActualResult.GOOD).endingAt(75, style = FieldStyle.PLAYOFF),
            "field-goal-bowl" to
                play(TeamSide.HOME, 75, PlayCall.FIELD_GOAL, ActualResult.GOOD).endingAt(75, style = FieldStyle.BOWL),
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
        val frames =
            renderer.renderFrames(
                play,
                startAbs,
                endAbs,
                themeFor(preview.style),
                preview.offensivePlaybook,
                preview.defensivePlaybook,
            )
        val decoratedFrames = overlayPainter.applyOverlay(frames, overlayClassifier.classifyOverlay(play), play, homeTeam, awayTeam)
        return encoder.fit(decoratedFrames, AnimationPalette.forTheme(themeFor(preview.style)))
    }

    private fun scoringTeamOrNull(play: Play): TeamSide? =
        when (play.actualResult) {
            ActualResult.TOUCHDOWN, ActualResult.KICKING_TEAM_TOUCHDOWN, ActualResult.PUNT_TEAM_TOUCHDOWN -> play.possession
            ActualResult.TURNOVER_TOUCHDOWN, ActualResult.RETURN_TOUCHDOWN, ActualResult.KICK_SIX,
            ActualResult.PUNT_RETURN_TOUCHDOWN, ActualResult.DEFENSE_TWO_POINT,
            -> if (play.possession == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
            else -> null
        }

    private fun play(
        possession: TeamSide,
        ballLocation: Int,
        playCall: PlayCall,
        actualResult: ActualResult,
        result: Scenario? = null,
        forcedPlayId: Int? = null,
    ): Play =
        Play().apply {
            playId = forcedPlayId ?: nextPlayId++
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
        style: FieldStyle = FieldStyle.HOME_FIELD,
    ) = Preview(this, endAbs, offensivePlaybook, defensivePlaybook, style)

    private fun boiseStateField(): FieldTheme {
        val boise =
            Team().apply {
                name = "Boise State"
                primaryColor = "#D64309"
                secondaryColor = "#0033A0"
                scorebugLogo = "https://a.espncdn.com/i/teamlogos/ncaa/500/68.png"
            }
        val override = TeamFieldOverrides.forTeam(boise.name)
        return FieldTheme(
            FieldStyle.HOME_FIELD,
            boise,
            awayTeam,
            boise.scorebugLogo,
            turf = override?.turf ?: FieldBackgroundPainter.TURF_COLOR,
        )
    }

    private fun conferenceLogoFor(team: Team): String? =
        when (team.conference) {
            "BIG_TEN" -> "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/5.png"
            "MOUNTAIN_WEST" -> "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/17.png"
            else -> null
        }

    private fun themeFor(style: FieldStyle): FieldTheme =
        FieldTheme(
            style = style,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            centerLogoUrl = if (style == FieldStyle.HOME_FIELD) homeTeam.scorebugLogo else postseasonLogoFor(style),
            homeConferenceLogoUrl = if (style == FieldStyle.BOWL) conferenceLogoFor(homeTeam) else null,
            awayConferenceLogoUrl = if (style == FieldStyle.BOWL) conferenceLogoFor(awayTeam) else null,
        )

    private fun postseasonLogoFor(style: FieldStyle): String =
        when (style) {
            FieldStyle.CONFERENCE_CHAMPIONSHIP -> CONFERENCE_LOGO
            FieldStyle.BOWL -> BOWL_LOGO
            else -> PLAYOFF_LOGO
        }

    private data class Preview(
        val play: Play,
        val endAbs: Int,
        val offensivePlaybook: OffensivePlaybook,
        val defensivePlaybook: DefensivePlaybook,
        val style: FieldStyle,
    )

    private companion object {
        var nextPlayId = 1
        const val HELMET_PREVIEW_SIZE = 256

        const val CONTESTED_PLAY_ID = 902
        const val RUN_DOWN_PLAY_ID = 900
        const val BROKEN_UP_PLAY_ID = 954
        const val TWO_POINT_PASS_PLAY_ID = 902
        const val TWO_POINT_RUN_PLAY_ID = 900
        const val NEAR_MISS_PLAY_ID = 900
        const val DEEP_SCORE_OPEN_PLAY_ID = 902
        const val DEEP_SCORE_STRIDE_PLAY_ID = 900
        const val HAIL_MARY_PLAY_ID = 900
        const val RETURN_SIDELINE_PLAY_ID = 901
        const val RETURN_MIDDLE_PLAY_ID = 911
        const val RETURN_CUTBACK_PLAY_ID = 900
        const val ONSIDE_ALT_PLAY_ID = 917
        const val PLAYOFF_LOGO =
            "https://am-prod-client-files.ppub-tmaws.io/cfbplayoff/s3fs-public/" +
                "CFP%20Symbol%20Gold%20PMS%20Dark%20BG.PNG"
        const val CONFERENCE_LOGO = "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/4.png"
        const val BOWL_LOGO = "https://1000logos.net/wp-content/uploads/2020/04/Holiday-Bowl-Logo.png"
    }
}
