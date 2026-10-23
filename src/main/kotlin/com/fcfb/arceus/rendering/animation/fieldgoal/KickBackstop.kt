package com.fcfb.arceus.rendering.animation.fieldgoal

sealed interface KickBackstop {
    data object Net : KickBackstop

    data class Wall(
        val topY: Int,
    ) : KickBackstop

    data class Grass(
        val landingY: Int,
        val distance: Float,
    ) : KickBackstop

    data class Facade(
        val landingY: Int,
    ) : KickBackstop
}
