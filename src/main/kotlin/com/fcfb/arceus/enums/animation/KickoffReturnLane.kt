package com.fcfb.arceus.enums.animation

enum class KickoffReturnLane(
    val endLateralShare: Float,
) {
    SIDELINE(1f),
    MIDDLE(0.1f),
    CUTBACK(-0.75f),
}
