package com.fcfb.arceus.service.fcfb.animation

/** A helmet's center-stripe layout: one band, or three parallel bands with a distinct inner and outer color. */
enum class StripeType {
    SINGLE,
    TRIPLE,
    TRIPLE_FLUSH,
    ;

    companion object {
        fun from(stored: String?): StripeType = entries.firstOrNull { it.name == stored?.uppercase() } ?: SINGLE
    }
}
