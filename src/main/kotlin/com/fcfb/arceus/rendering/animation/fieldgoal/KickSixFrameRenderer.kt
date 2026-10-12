package com.fcfb.arceus.rendering.animation.fieldgoal

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.rendering.animation.AnimatedGifEncoder
import com.fcfb.arceus.rendering.animation.AnimationFitter
import com.fcfb.arceus.rendering.animation.choreography.FieldCamera
import com.fcfb.arceus.rendering.animation.choreography.script.BlockedKickPlayScript
import com.fcfb.arceus.rendering.animation.choreography.script.ShortKickReturnScript
import com.fcfb.arceus.rendering.animation.frame.OverheadPlayFrameRenderer
import com.fcfb.arceus.rendering.animation.frame.PlayAnimationFrameRenderer
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

/**
 * A kick six opens in the goal-post scene and finishes overhead. The two scenes render at
 * different heights, and neither [AnimationFitter] nor [AnimatedGifEncoder] resizes — the encoder
 * takes its dimensions from the first frame — so every goal-post frame is cropped to the overhead
 * height before the two halves are concatenated.
 */
@Component
class KickSixFrameRenderer(
    private val fieldGoalAttemptFrameRenderer: FieldGoalAttemptFrameRenderer,
    private val overheadPlayFrameRenderer: OverheadPlayFrameRenderer,
) : PlayAnimationFrameRenderer {
    override fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        theme: FieldTheme,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
    ): List<BufferedImage> {
        val returned = KickDistance.fallsShort(play)
        val kickFrames =
            fieldGoalAttemptFrameRenderer
                .renderKickPhase(
                    play,
                    theme,
                    blocked = !returned,
                    endsAt = if (returned) CATCH_ENDS_AT else BLOCK_ENDS_AT,
                    caught = returned,
                )
                .map { croppedToFieldHeight(it) }
        val handoff = if (returned) ShortKickReturnScript.CATCH_AT else BlockedKickPlayScript.DEFLECT_AT
        val returnFrames =
            overheadPlayFrameRenderer.renderFromProgress(
                play,
                startAbs,
                endAbs,
                theme,
                offensivePlaybook,
                defensivePlaybook,
                handoff,
            )
        return kickFrames + returnFrames
    }

    private fun croppedToFieldHeight(scene: BufferedImage): BufferedImage {
        if (scene.height <= FieldCamera.VIEW_HEIGHT) return scene
        val cropped = BufferedImage(scene.width, FieldCamera.VIEW_HEIGHT, BufferedImage.TYPE_INT_RGB)
        val graphics = cropped.createGraphics()
        graphics.drawImage(scene, 0, FieldCamera.VIEW_HEIGHT - scene.height, null)
        graphics.dispose()
        return cropped
    }

    companion object {
        private const val CATCH_ENDS_AT = 0.85f
        private const val BLOCK_ENDS_AT = 0.42f
    }
}
