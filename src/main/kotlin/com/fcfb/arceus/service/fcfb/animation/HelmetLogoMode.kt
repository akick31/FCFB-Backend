package com.fcfb.arceus.service.fcfb.animation

/** What a helmet carries. [NONE] is render-only: the database expresses it with `has_logo`, not with this value. */
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
