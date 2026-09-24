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
    private val kickSixRenderer = KickSixFrameRenderer(fieldGoalRenderer, overheadRenderer)

    @Test
    fun generatePreviews() {
        val outputDir = File("animation-previews")
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val rendered = mutableListOf<String>()
        scenarios().forEach { (name, preview) ->
            val bytes = renderGif(preview)
            File(outputDir, "$name.gif").writeBytes(bytes)
            assertTrue(bytes.isNotEmpty(), "Expected non-empty GIF bytes for scenario '$name'")
            rendered += name
        }
        File(outputDir, "index.html").writeText(previewIndex(rendered))
        FieldStyle.entries.forEach { style ->
            javax.imageio.ImageIO.write(
                FieldBackgroundPainter.paint(themeFor(style)),
                "png",
                File(outputDir, "field-${style.name.lowercase()}.png"),
            )
        }
        val shell = FieldBackgroundPainter.parseColor(awayTeam.primaryColor)
        val helmet =
            HelmetSprite.render(
                Uniform(jersey = shell, number = java.awt.Color.WHITE, helmet = shell, pants = shell),
                LogoLoader.load(awayTeam.scorebugLogo),
                HELMET_PREVIEW_SIZE,
            )
        javax.imageio.ImageIO.write(helmet.facingRight, "png", File(outputDir, "helmet.png"))
        javax.imageio.ImageIO.write(helmetVariants(), "png", File(outputDir, "helmet-variants.png"))
        javax.imageio.ImageIO.write(FieldBackgroundPainter.paint(boiseStateField()), "png", File(outputDir, "field-boise-state.png"))
        javax.imageio.ImageIO.write(
            FieldBackgroundPainter.paint(themeFor(FieldStyle.BOWL).copy(flipped = true)),
            "png",
            File(outputDir, "field-second-half.png"),
        )
        javax.imageio.ImageIO.write(
            FieldBackgroundPainter.paint(homeFieldWithLogos()),
            "png",
            File(outputDir, "field-home-logos.png"),
        )
        val wallPlay = play(TeamSide.HOME, 75, PlayCall.FIELD_GOAL, ActualResult.GOOD, forcedPlayId = WALL_PREVIEW_PLAY_ID)
        val wallLayout = GoalPostScenePainter.layoutFor(wallPlay)
        listOf("REPEATING_LOGOS", "TEXT_WITH_LOGOS", "TEXT_ONLY", "BLANK").forEach { design ->
            val scene = homeFieldWall(design)
            javax.imageio.ImageIO.write(
                GoalPostScenePainter.paint(scene, scene.endZoneOf(TeamSide.AWAY), wallLayout, midfieldTopOnLeft = false),
                "png",
                File(outputDir, "wall-${design.lowercase()}.png"),
            )
        }
    }

    private fun scenarios(): List<Pair<String, Preview>> =
        listOf(
            "run-pro-short-gain" to play(TeamSide.HOME, 30, PlayCall.RUN, ActualResult.GAIN).endingAt(36),
            "run-flexbone-vs-335" to
                play(TeamSide.HOME, 30, PlayCall.RUN, ActualResult.FIRST_DOWN)
                    .endingAt(42, OffensivePlaybook.FLEXBONE, DefensivePlaybook.THREE_THREE_FIVE),
            "run-for-loss" to play(TeamSide.HOME, 40, PlayCall.RUN, ActualResult.LOSS).endingAt(37),
            "run-loss-stuffed-inside" to
                play(TeamSide.HOME, 40, PlayCall.RUN, ActualResult.LOSS, forcedPlayId = LOSS_INSIDE_PLAY_ID).endingAt(35),
            "run-loss-stuffed-outside" to
                play(TeamSide.HOME, 40, PlayCall.RUN, ActualResult.LOSS, forcedPlayId = LOSS_OUTSIDE_PLAY_ID).endingAt(35),
            "run-loss-pitch" to
                play(TeamSide.HOME, 40, PlayCall.RUN, ActualResult.LOSS, forcedPlayId = LOSS_PITCH_PLAY_ID).endingAt(35),
            "run-loss-reverse" to
                play(TeamSide.HOME, 40, PlayCall.RUN, ActualResult.LOSS, forcedPlayId = LOSS_REVERSE_PLAY_ID).endingAt(35),
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
            "pass-incomplete-tipped" to
                play(
                    TeamSide.HOME,
                    50,
                    PlayCall.PASS,
                    ActualResult.NO_GAIN,
                    Scenario.INCOMPLETE,
                    forcedPlayId = TIPPED_INCOMPLETION_PLAY_ID,
                ).endingAt(50),
            "pass-catch-and-run-short" to play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.GAIN).endingAt(37),
            "pass-catch-and-run-medium" to play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.GAIN).endingAt(45),
            "pass-catch-and-run-long" to play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.FIRST_DOWN).endingAt(58),
            "pass-tipped-completion" to
                play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.GAIN, forcedPlayId = TIPPED_COMPLETION_PLAY_ID).endingAt(42),
            "pass-catch-wrapped-up" to
                play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.GAIN, forcedPlayId = CATCH_WRAPPED_UP_PLAY_ID).endingAt(42),
            "pass-catch-short-run" to
                play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.GAIN, forcedPlayId = CATCH_SHORT_RUN_PLAY_ID).endingAt(42),
            "pass-catch-broke-tackle" to
                play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.GAIN, forcedPlayId = CATCH_BROKE_TACKLE_PLAY_ID).endingAt(42),
            "pass-catch-in-stride" to
                play(TeamSide.HOME, 30, PlayCall.PASS, ActualResult.GAIN, forcedPlayId = CATCH_IN_STRIDE_PLAY_ID).endingAt(42),
            "pass-completion-for-loss" to play(TeamSide.HOME, 40, PlayCall.PASS, ActualResult.LOSS).endingAt(36),
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
            "two-point-option" to
                play(TeamSide.HOME, 97, PlayCall.TWO_POINT, ActualResult.SUCCESS, forcedPlayId = TWO_POINT_OPTION_PLAY_ID)
                    .endingAt(100, OffensivePlaybook.FLEXBONE),
            "two-point-read" to
                play(TeamSide.HOME, 97, PlayCall.TWO_POINT, ActualResult.SUCCESS, forcedPlayId = TWO_POINT_READ_PLAY_ID)
                    .endingAt(100, OffensivePlaybook.SPREAD),
            "two-point-statue" to
                play(TeamSide.HOME, 97, PlayCall.TWO_POINT, ActualResult.SUCCESS, forcedPlayId = TWO_POINT_STATUE_PLAY_ID)
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
            "field-goal-kick-six" to play(TeamSide.HOME, 70, PlayCall.FIELD_GOAL, ActualResult.KICK_SIX).endingAt(0),
            "field-goal-kick-six-long" to play(TeamSide.HOME, 55, PlayCall.FIELD_GOAL, ActualResult.KICK_SIX).endingAt(0),
            "extra-point-good" to play(TeamSide.HOME, 97, PlayCall.PAT, ActualResult.GOOD).endingAt(97),
            "extra-point-blocked" to play(TeamSide.AWAY, 97, PlayCall.PAT, ActualResult.BLOCKED).endingAt(3),
            "field-goal-from-the-2" to play(TeamSide.HOME, 98, PlayCall.FIELD_GOAL, ActualResult.GOOD).endingAt(98),
            "field-goal-long-no-good" to play(TeamSide.AWAY, 57, PlayCall.FIELD_GOAL, ActualResult.NO_GOOD).endingAt(43),
            "field-goal-beyond-range" to play(TeamSide.HOME, 40, PlayCall.FIELD_GOAL, ActualResult.NO_GOOD).endingAt(40),
            "field-goal-playoff" to
                play(TeamSide.HOME, 75, PlayCall.FIELD_GOAL, ActualResult.GOOD).endingAt(75, style = FieldStyle.PLAYOFF),
            "field-goal-bowl" to
                play(TeamSide.HOME, 75, PlayCall.FIELD_GOAL, ActualResult.GOOD).endingAt(75, style = FieldStyle.BOWL),
            // Appended last on purpose: inserting mid-list reseeds every scenario defined after it.
            // A spike is always under center; this proves the SPREAD playbook does not leave the quarterback in shotgun.
            "spike-spread-playbook" to
                play(TeamSide.HOME, 60, PlayCall.SPIKE, ActualResult.SPIKE).endingAt(60, OffensivePlaybook.SPREAD),
            // A reverse and a jet sweep are no longer losses by definition; these exercise the winning outcomes.
            "run-reverse-touchdown" to
                play(TeamSide.HOME, 70, PlayCall.RUN, ActualResult.TOUCHDOWN, forcedPlayId = REVERSE_SCORE_PLAY_ID).endingAt(100),
            "run-jet-sweep-gain" to
                play(TeamSide.HOME, 30, PlayCall.RUN, ActualResult.FIRST_DOWN, forcedPlayId = JET_SWEEP_GAIN_PLAY_ID).endingAt(42),
            "run-jet-sweep-loss" to
                play(TeamSide.HOME, 40, PlayCall.RUN, ActualResult.LOSS, forcedPlayId = JET_SWEEP_LOSS_PLAY_ID).endingAt(35),
        )

    private fun renderGif(preview: Preview): ByteArray {
        val play = preview.play
        val startAbs = FieldCoordinateMapper.toAbsoluteFieldPosition(play.ballLocation, play.possession)
        val endAbs =
            when {
                play.actualResult == ActualResult.SAFETY -> if (play.possession == TeamSide.HOME) -5 else 105
                else -> scoringTeamOrNull(play)?.let { if (it == TeamSide.HOME) 105 else -5 } ?: preview.endAbs
            }
        val renderer =
            when {
                play.actualResult == ActualResult.KICK_SIX -> kickSixRenderer
                classifier.classify(play) == AnimatedPlayType.FIELD_GOAL -> fieldGoalRenderer
                else -> overheadRenderer
            }
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
        val field =
            com.fcfb.arceus.model.TeamField().apply {
                team = boise.name.orEmpty()
                turfColor = BOISE_TURF_COLOR
                endZoneColor = BOISE_END_ZONE_COLOR
            }
        return FieldTheme(
            FieldStyle.HOME_FIELD,
            boise,
            awayTeam,
            boise.scorebugLogo,
            turf = FieldBackgroundPainter.parseColor(field.turfColor),
            homeField = field,
        )
    }

    /** Every preview on one page in scenario order, so the whole set can be reviewed without opening files one by one. */
    private fun previewIndex(names: List<String>): String {
        val cards =
            names.joinToString("\n") { name ->
                """    <figure><img src="$name.gif" alt="$name" loading="lazy"><figcaption>$name</figcaption></figure>"""
            }
        return """
            <!doctype html>
            <meta charset="utf-8">
            <title>Play animation previews</title>
            <style>
              body { background:#15171a; color:#e8e8e8; font:14px system-ui,sans-serif; margin:0; padding:24px; }
              h1 { font-size:18px; font-weight:600; margin:0 0 4px; }
              p.count { color:#9aa0a6; margin:0 0 24px; }
              .grid { display:grid; gap:20px; grid-template-columns:repeat(auto-fill,minmax(420px,1fr)); }
              figure { margin:0; background:#1e2125; border-radius:8px; padding:10px; }
              img { width:100%; height:auto; display:block; border-radius:4px; }
              figcaption { margin-top:8px; font-family:ui-monospace,monospace; font-size:12px; color:#9aa0a6; }
            </style>
            <h1>Play animation previews</h1>
            <p class="count">${names.size} scenarios, in definition order</p>
            <div class="grid">
            $cards
            </div>
            """.trimIndent()
    }

    /** Left to right: plain, stripe, colored facemask, jersey numbers, no decal. */
    private fun helmetVariants(): java.awt.image.BufferedImage {
        val shell = FieldBackgroundPainter.parseColor(homeTeam.primaryColor)
        val accent = FieldBackgroundPainter.parseColor(homeTeam.secondaryColor)
        val base = Uniform(jersey = shell, number = java.awt.Color.WHITE, helmet = shell, pants = shell)
        val variants =
            listOf(
                base,
                base.copy(stripe = accent),
                base.copy(facemask = accent),
                base.copy(helmetLogoMode = HelmetLogoMode.NUMBERS),
                base.copy(helmetLogoMode = HelmetLogoMode.NONE),
            )
        val logo = LogoLoader.load(homeTeam.scorebugLogo)
        val size = HELMET_PREVIEW_SIZE
        val strip = java.awt.image.BufferedImage(size * variants.size, size, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = strip.createGraphics()
        g.color = java.awt.Color(90, 90, 90)
        g.fillRect(0, 0, strip.width, strip.height)
        variants.forEachIndexed { index, uniform ->
            g.drawImage(HelmetSprite.render(uniform, logo, size).facingRight, index * size, 0, null)
        }
        g.dispose()
        return strip
    }

    /** A goal-post scene with a configured wall, so the wall color and design paths actually run. */
    private fun homeFieldWall(design: String): FieldTheme {
        val field =
            com.fcfb.arceus.model.TeamField().apply {
                team = homeTeam.name.orEmpty()
                wallColor = homeTeam.primaryColor
                wallDesign = design
                wallText = homeTeam.name?.uppercase()
                wallTextOutlineColor = homeTeam.secondaryColor
            }
        return FieldTheme(
            style = FieldStyle.HOME_FIELD,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            centerLogoUrl = homeTeam.scorebugLogo,
            wallCaption = field.wallText,
            homeField = field,
        )
    }

    private fun homeFieldWithLogos(): FieldTheme {
        val field =
            com.fcfb.arceus.model.TeamField().apply {
                team = homeTeam.name.orEmpty()
                quarterLogoUrl = homeTeam.scorebugLogo
                redZoneBorderColor = homeTeam.primaryColor
                oobLineColor = homeTeam.primaryColor
            }
        return FieldTheme(
            style = FieldStyle.HOME_FIELD,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            centerLogoUrl = homeTeam.scorebugLogo,
            homeConferenceLogoUrl = conferenceLogoFor(homeTeam),
            awayConferenceLogoUrl = conferenceLogoFor(homeTeam),
            homeField = field,
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
            wallCaption = wallCaptionFor(style),
            homeField = seededTeamField(),
        )

    /** Mirrors what `V31` seeds for every team, so previews show a real configuration rather than a null one. */
    private fun seededTeamField(): com.fcfb.arceus.model.TeamField =
        com.fcfb.arceus.model.TeamField().apply {
            team = homeTeam.name.orEmpty()
            endZoneColor = homeTeam.primaryColor
            midfieldLogoUrl = homeTeam.scorebugLogo
            redZoneBorderColor = homeTeam.primaryColor
            wallColor = homeTeam.primaryColor
        }

    private fun wallCaptionFor(style: FieldStyle): String? =
        when (style) {
            FieldStyle.BOWL -> "Holiday Bowl"
            FieldStyle.PLAYOFF -> "CFP Quarterfinal"
            FieldStyle.NATIONAL_CHAMPIONSHIP -> "National Championship"
            FieldStyle.CONFERENCE_CHAMPIONSHIP -> "Big 12 Championship"
            else -> null
        }

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
        const val BROKEN_UP_PLAY_ID = 903
        const val TIPPED_INCOMPLETION_PLAY_ID = 902
        const val TIPPED_COMPLETION_PLAY_ID = 900
        const val TWO_POINT_OPTION_PLAY_ID = 901
        const val TWO_POINT_READ_PLAY_ID = 920
        const val TWO_POINT_STATUE_PLAY_ID = 900
        const val CATCH_WRAPPED_UP_PLAY_ID = 907
        const val CATCH_SHORT_RUN_PLAY_ID = 902
        const val CATCH_BROKE_TACKLE_PLAY_ID = 901
        const val CATCH_IN_STRIDE_PLAY_ID = 910
        const val TWO_POINT_PASS_PLAY_ID = 902
        const val TWO_POINT_RUN_PLAY_ID = 921
        const val NEAR_MISS_PLAY_ID = 900
        const val DEEP_SCORE_OPEN_PLAY_ID = 902
        const val DEEP_SCORE_STRIDE_PLAY_ID = 900
        const val HAIL_MARY_PLAY_ID = 900
        const val RETURN_SIDELINE_PLAY_ID = 901
        const val RETURN_MIDDLE_PLAY_ID = 911
        const val RETURN_CUTBACK_PLAY_ID = 900
        // Recomputed for the five-value RunConcept draw; the old ids selected different concepts.
        const val LOSS_INSIDE_PLAY_ID = 896
        const val LOSS_OUTSIDE_PLAY_ID = 897
        const val LOSS_PITCH_PLAY_ID = 904
        const val LOSS_REVERSE_PLAY_ID = 900
        const val REVERSE_SCORE_PLAY_ID = 901
        const val JET_SWEEP_LOSS_PLAY_ID = 895
        const val JET_SWEEP_GAIN_PLAY_ID = 902
        const val ONSIDE_ALT_PLAY_ID = 917
        const val PLAYOFF_LOGO =
            "https://am-prod-client-files.ppub-tmaws.io/cfbplayoff/s3fs-public/" +
                "CFP%20Symbol%20Gold%20PMS%20Dark%20BG.PNG"
        const val CONFERENCE_LOGO = "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/4.png"
        const val BOWL_LOGO = "https://1000logos.net/wp-content/uploads/2020/04/Holiday-Bowl-Logo.png"
        const val WALL_PREVIEW_PLAY_ID = 930
        const val BOISE_TURF_COLOR = "#0033A0"
        const val BOISE_END_ZONE_COLOR = "#D64309"
    }
}
