package com.fcfb.arceus.enums.animation

enum class HelmetLogoMode {
    MAIN,
    UPLOAD,
    NUMBERS,
    NONE,
    ;

    val drawsLogo: Boolean get() = this == MAIN || this == UPLOAD

    companion object {
        fun from(
            stored: String?,
            hasLogo: Boolean,
        ): HelmetLogoMode {
            if (!hasLogo) return NONE
            return entries.firstOrNull { it.name == stored?.uppercase() && it != NONE } ?: MAIN
        }
    }
}
