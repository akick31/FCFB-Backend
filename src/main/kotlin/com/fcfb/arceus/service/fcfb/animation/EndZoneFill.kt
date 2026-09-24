package com.fcfb.arceus.service.fcfb.animation

/** Which team color fills a bowl end zone. The text outline takes the other one, so storing a color would lose that rule. */
enum class EndZoneFill {
    NONE,
    PRIMARY,
    SECONDARY,
    ;

    companion object {
        fun from(stored: String?): EndZoneFill = entries.firstOrNull { it.name == stored?.uppercase() } ?: PRIMARY
    }
}
