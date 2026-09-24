package com.fcfb.arceus.service.fcfb.animation

/**
 * Two point conversions are run from the same three yard line every time, so the only thing that
 * distinguishes one from another is the concept. The result (SUCCESS or FAILED) is independent of
 * the kind — a fade can be caught or dropped, an option can be strung out or walked in.
 */
enum class TwoPointKind {
    POWER,
    FADE,
    OPTION,
    READ,
    STATUE,
}
