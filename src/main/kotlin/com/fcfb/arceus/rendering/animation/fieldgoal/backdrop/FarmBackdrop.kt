package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.BackdropVariant
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm.CropFarm
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm.FarmStyle
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm.HayFarm
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm.OrchardFarm
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm.PastureFarm
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object FarmBackdrop {
    private const val SEED = 42L

    fun paletteOf(variant: BackdropVariant?): List<Color> = styleOf(variant).palette

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        variant: BackdropVariant?,
    ) = styleOf(variant).paint(g, Random(SEED), area)

    private fun styleOf(variant: BackdropVariant?): FarmStyle =
        when (variant) {
            BackdropVariant.FIELD_PASTURE -> PastureFarm
            BackdropVariant.FIELD_HAY -> HayFarm
            BackdropVariant.FIELD_ORCHARD -> OrchardFarm
            else -> CropFarm
        }
}
