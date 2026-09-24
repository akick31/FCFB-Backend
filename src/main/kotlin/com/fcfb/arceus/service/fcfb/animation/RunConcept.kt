package com.fcfb.arceus.service.fcfb.animation

/**
 * Drawn for every run, not only losses, so a reverse or a jet sweep can gain, score or lose. [REVERSE] takes the ball
 * back against the motion man's direction; [JET_SWEEP] keeps going the way he was already running.
 */
enum class RunConcept {
    POWER,
    OUTSIDE,
    PITCH,
    REVERSE,
    JET_SWEEP,
    ;

    val inMotion: Boolean get() = this == REVERSE || this == JET_SWEEP
}
