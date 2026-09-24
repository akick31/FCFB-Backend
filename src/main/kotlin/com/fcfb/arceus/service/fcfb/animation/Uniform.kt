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
    val helmetNumber: Color = Color.WHITE,
    val helmetLogoMode: HelmetLogoMode = HelmetLogoMode.MAIN,
)
