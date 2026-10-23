package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.BackdropVariant
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.BrickFacility
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.BrickTowerFacility
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.CollegiateNarrowFacility
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.ConcreteFacility
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.FacilityStyle
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.GeorgianNarrowFacility
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.ModernNarrowFacility
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.ModernTowerFacility
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.StoneTowerFacility
import java.awt.Color
import java.awt.Graphics2D

object FacilityBackdrop {
    fun paletteOf(variant: BackdropVariant?): List<Color> = styleOf(variant).palette

    fun fillsView(variant: BackdropVariant?): Boolean = styleOf(variant).fillsView

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        variant: BackdropVariant?,
        footY: Int,
    ) = styleOf(variant).paint(g, area, footY)

    fun boardOver(
        area: BackdropArea,
        variant: BackdropVariant?,
        footY: Int,
    ): JumbotronMount = styleOf(variant).boardOver(area, footY)

    private fun styleOf(variant: BackdropVariant?): FacilityStyle =
        when (variant) {
            BackdropVariant.FACILITY_CONCRETE -> ConcreteFacility
            BackdropVariant.FACILITY_TOWER -> BrickTowerFacility
            BackdropVariant.FACILITY_TOWER_STONE -> StoneTowerFacility
            BackdropVariant.FACILITY_TOWER_MODERN -> ModernTowerFacility
            BackdropVariant.FACILITY_NARROW_GEORGIAN -> GeorgianNarrowFacility
            BackdropVariant.FACILITY_NARROW_MODERN -> ModernNarrowFacility
            BackdropVariant.FACILITY_NARROW_COLLEGIATE -> CollegiateNarrowFacility
            else -> BrickFacility
        }
}
