package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.BackdropVariant
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains.AlpineMountains
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains.DesertMesas
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains.JaggedPeaks
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains.LoneVolcano
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains.MountainStyle
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains.RollingHills
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains.SmokyRidges
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object MountainBackdrop {
    private const val SEED = 42L

    fun paletteOf(variant: BackdropVariant?): List<Color> = styleOf(variant).palette

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        variant: BackdropVariant?,
    ) = styleOf(variant).paint(g, Random(SEED), area)

    private fun styleOf(variant: BackdropVariant?): MountainStyle =
        when (variant) {
            BackdropVariant.MOUNTAINS_DESERT -> DesertMesas
            BackdropVariant.MOUNTAINS_SMOKY -> SmokyRidges
            BackdropVariant.MOUNTAINS_VOLCANO -> LoneVolcano
            BackdropVariant.MOUNTAINS_HILLS -> RollingHills
            BackdropVariant.MOUNTAINS_JAGGED -> JaggedPeaks
            else -> AlpineMountains
        }
}
