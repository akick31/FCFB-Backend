package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color

data class Uniform(
    val jersey: Color,
    val number: Color,
    val helmet: Color,
    val pants: Color,
    val facemask: Color = Color.WHITE,
    val numberOutline: Color? = null,
    val stripe: Color? = null,
    val stripeType: StripeType = StripeType.SINGLE,
    val outerStripe: Color? = null,
    val helmetNumber: Color = Color.WHITE,
    val helmetNumberFont: String? = null,
    val jerseyNumberFont: String? = null,
    val helmetLogoMode: HelmetLogoMode = HelmetLogoMode.MAIN,
    val logoSize: Float = 1f,
    val logoX: Float = 0f,
    val logoY: Float = 0f,
    val logoRotation: Float = 0f,
    val hasShoulderStripe: Boolean = false,
    val shoulderStripeColor: Color? = null,
    val jerseyText: String? = null,
)
