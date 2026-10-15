package com.fcfb.arceus.enums.animation

enum class StripeType {
    SINGLE,
    TRIPLE,
    TRIPLE_FLUSH,
    ;

    companion object {
        fun from(stored: String?): StripeType = entries.firstOrNull { it.name == stored?.uppercase() } ?: SINGLE
    }
}
