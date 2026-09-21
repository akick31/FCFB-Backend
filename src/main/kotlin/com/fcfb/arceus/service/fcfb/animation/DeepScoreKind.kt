package com.fcfb.arceus.service.fcfb.animation

enum class DeepScoreKind(
    val coverTrail: Float,
    val coverShoulder: Float,
    val covered: Boolean,
) {
    WIDE_OPEN(0f, 0f, false),
    IN_STRIDE(2.5f, 0.8f, true),
    HAIL_MARY(0.9f, 1.3f, true),
}
