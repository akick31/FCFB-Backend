package com.fcfb.arceus.service.fcfb.animation

/**
 * How much of a completion's gain comes after the catch. [minShare]/[maxShare] are fractions of
 * the play's total gain that the receiver covers on his own, so the same kinds apply to a short
 * curl and a fifty yard strike alike.
 */
enum class CatchKind(
    val minShare: Float,
    val maxShare: Float,
) {
    WRAPPED_UP(0f, 0.05f),
    SHORT_RUN(0.15f, 0.35f),
    BROKE_A_TACKLE(0.4f, 0.6f),
    CAUGHT_IN_STRIDE(0.65f, 0.85f),
}
