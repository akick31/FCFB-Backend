package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount

object TowerMassing {
    const val WING_HALF = 260
    const val WING_HEIGHT = 92
    const val TOWER_HALF = 98
    const val TOWER_HEIGHT = 176
    private const val BOARD_SPAN = 760

    fun boardOver(
        area: BackdropArea,
        footY: Int,
        roofReference: Int,
    ): JumbotronMount = FacilityGround.pylonsOver(area, footY, FacilityFrame(area, footY).y(roofReference), BOARD_SPAN)
}
