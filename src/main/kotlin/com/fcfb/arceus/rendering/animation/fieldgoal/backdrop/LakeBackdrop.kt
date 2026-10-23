package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.BackdropVariant
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake.FlatShore
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake.HillShore
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake.LakeShore
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake.LakeWater
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake.MountainShore
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake.OpenHorizon
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake.PineShore
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object LakeBackdrop {
    private const val SEED = 42L

    fun paletteOf(variant: BackdropVariant?): List<Color> = shoreOf(variant).palette + LakeWater.PALETTE

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        variant: BackdropVariant?,
    ) {
        val shore = shoreOf(variant)
        val random = Random(SEED)
        val horizonY = area.yAt(shore.horizonShare)
        shore.paintFarShore(g, random, area, horizonY)
        LakeWater.paint(g, random, area, horizonY, shore.reflection)
    }

    private fun shoreOf(variant: BackdropVariant?): LakeShore =
        when (variant) {
            BackdropVariant.LAKE_OPEN -> OpenHorizon
            BackdropVariant.LAKE_PINES -> PineShore
            BackdropVariant.LAKE_MOUNTAINS -> MountainShore
            BackdropVariant.LAKE_FLAT -> FlatShore
            else -> HillShore
        }
}
