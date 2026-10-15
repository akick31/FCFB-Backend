package com.fcfb.arceus.enums.animation

enum class RunConcept {
    POWER,
    OUTSIDE,
    PITCH,
    REVERSE,
    JET_SWEEP,
    ;

    val inMotion: Boolean get() = this == REVERSE || this == JET_SWEEP
}
