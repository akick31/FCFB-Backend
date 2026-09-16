package com.fcfb.arceus.service.fcfb.animation.choreography

/**
 * [facingLocked] holds offensive players who keep their pre-snap facing however they move, so a quarterback dropping back
 * still faces downfield instead of turning to run backwards.
 */
data class Choreography(
    val offense: List<Track>,
    val defense: List<Track>,
    val ball: BallTrack,
    val callout: Callout? = null,
    val facingLocked: Set<Int> = emptySet(),
    val showBall: Boolean = true,
    /** When the play is actually over. Frames past this are dead air, so the animation stops rather than holding on a freeze. */
    val endsAt: Float = 1f,
)
