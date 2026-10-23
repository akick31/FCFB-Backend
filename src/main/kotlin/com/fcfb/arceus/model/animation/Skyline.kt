package com.fcfb.arceus.model.animation

import com.fcfb.arceus.enums.animation.BackdropVariant
import com.fcfb.arceus.enums.animation.StadiumBackdrop

data class Skyline(
    val backdrop: StadiumBackdrop,
    val variant: BackdropVariant?,
) {
    companion object {
        private val SCENERY =
            setOf(
                StadiumBackdrop.BUILDING,
                StadiumBackdrop.CAMPUS,
                StadiumBackdrop.FOREST,
                StadiumBackdrop.FIELD,
                StadiumBackdrop.LAKE,
                StadiumBackdrop.MOUNTAINS,
            )

        fun from(stored: String?): Skyline? {
            val name = stored?.uppercase() ?: return null
            BackdropVariant.entries.firstOrNull { it.name == name && it.backdrop in SCENERY }?.let { return Skyline(it.backdrop, it) }
            val backdrop = StadiumBackdrop.entries.firstOrNull { it.name == name && it in SCENERY } ?: return null
            return Skyline(backdrop, BackdropVariant.resolve(backdrop, null))
        }
    }
}
