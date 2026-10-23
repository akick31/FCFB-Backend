package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount

object FacilityGround {
    const val FOOT_SHARE = 0.6f
    private const val PYLON_GAP = 16
    private const val FACE_BOARD_LIFT = 58

    fun footY(area: BackdropArea): Int = area.groundY + ((area.frontY - area.groundY) * FOOT_SHARE).toInt()

    fun pylonsOver(
        area: BackdropArea,
        foot: Int,
        roofY: Int,
        spanReference: Int,
    ): JumbotronMount =
        JumbotronMount(
            groundY = foot,
            restingPost = foot - (roofY - area.scaled(PYLON_GAP)),
            canLift = false,
            keepInFrame = false,
            spanWidth = area.scaled(spanReference),
            fixedScale = true,
        )

    fun onFace(area: BackdropArea): JumbotronMount =
        JumbotronMount(
            groundY = area.groundY,
            restingPost = 0,
            canLift = false,
            keepInFrame = false,
            faceBottom = area.groundY - area.scaled(FACE_BOARD_LIFT),
            fixedScale = true,
        )
}
