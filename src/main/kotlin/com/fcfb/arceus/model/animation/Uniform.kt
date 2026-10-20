package com.fcfb.arceus.model.animation

import com.fcfb.arceus.enums.animation.HelmetLogoMode
import com.fcfb.arceus.enums.animation.StripeType
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
    val numberTopText: String? = null,
    val bumperColor: Color = DEFAULT_BUMPER_COLOR,
) {
    companion object {
        val DEFAULT_BUMPER_COLOR: Color = Color(0xDD, 0xDD, 0xDD)
    }
}
