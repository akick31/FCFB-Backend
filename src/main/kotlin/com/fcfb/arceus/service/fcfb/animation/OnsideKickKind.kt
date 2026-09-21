package com.fcfb.arceus.service.fcfb.animation

enum class OnsideKickKind(
    val minDepth: Float,
    val maxDepth: Float,
    val hopHeight: Float,
    val hops: Int,
) {
    DRIBBLER(9.5f, 11.5f, 1.2f, 4),
    HIGH_HOP(10.5f, 13.5f, 5.5f, 2),
    DRIVEN(13f, 17f, 1.8f, 3),
}
