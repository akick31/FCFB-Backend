package com.fcfb.arceus.service.fcfb.animation

/** Which logo a render slot uses. [CUSTOM] keeps the slot's own URL; the others point at the team's primary or secondary logo. */
enum class LogoSource {
    CUSTOM,
    PRIMARY,
    SECONDARY,
    ;

    companion object {
        fun from(stored: String?): LogoSource = entries.firstOrNull { it.name == stored?.uppercase() } ?: CUSTOM
    }
}
